package br.com.talisma.imoveis.dados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.com.talisma.imoveis.R;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * "API falsa" do modo demonstração (D24, 6.7): lê {@code assets/fake} e grava nas tabelas
 * {@code sim_*} do {@link BancoLocal}, com atraso de ~600 ms e falha de rede simulável
 * ({@link Preferencias#simularFalhaRede}). Os métodos bloqueiam: chamar sempre via {@code Tarefas}.
 * <p>
 * Vitrine (T03): lista com filtros e paginação, destaques, detalhe e opções de filtro, com o mesmo
 * formato das rotas da 6.9.
 * <p>
 * Contas, favoritos remotos e visitas (T05): na primeira execução copia {@code usuarios.json} para
 * {@code sim_usuario} (D27); login errado incrementa {@code tentativas} e a 3ª falha bloqueia (RN-002,
 * 423); e-mail não verificado → 403; código de verificação sempre {@value #CODIGO_VERIFICACAO};
 * token {@code "simulado-" + id} (token inválido → 401 de sessão expirada). Visita PENDENTE vira
 * CONFIRMADA quando lida 2 min após {@code criada_em} (D37). Fotos (T07) entram na task seguinte.
 */
public class ServidorSimulado {

    private static final String TAG = "ServidorSimulado";
    private static final String ARQUIVO_IMOVEIS = "fake/imoveis.json";
    private static final String ARQUIVO_USUARIOS = "fake/usuarios.json";
    private static final long ATRASO_MS = 600;

    /** Código de verificação de e-mail e de redefinição de senha do modo simulado (6.7). */
    public static final String CODIGO_VERIFICACAO = "1234";
    private static final String PREFIXO_TOKEN = "simulado-";
    /** RN-002: bloqueia na 3ª tentativa errada. */
    private static final int MAX_TENTATIVAS = 3;
    /** D37: no simulado, a visita é confirmada 2 min depois de criada. */
    private static final long CONFIRMAR_VISITA_MS = 2 * 60 * 1000;
    private static final String[] COLUNAS_VISITA =
            {"id", "imovel_id", "data", "periodo", "telefone", "mensagem", "status", "criada_em"};

    private static List<Imovel> imoveisCarregados;
    private static boolean usuariosCopiados;

    private final Context context;

    public ServidorSimulado(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Todos os imóveis do seed (já aprovados — RN-001), na ordem do arquivo. */
    public List<Imovel> imoveis() throws ApiException {
        simularRede();
        List<Imovel> imoveis = new ArrayList<>();
        for (Imovel i : carregarImoveis(context)) {
            imoveis.add(copia(i));
        }
        return imoveis;
    }

    /**
     * {@code GET /api/imoveis}: imóveis que passam no filtro (D16), página {@code pagina} (base 0)
     * de {@link Pagina#TAMANHO_PADRAO}. Preço mínimo maior que o máximo → 400 (RN-003).
     */
    public Pagina<Imovel> listar(Filtro filtro, int pagina) throws ApiException {
        simularRede();
        if (filtro.precoMin != null && filtro.precoMax != null && filtro.precoMin > filtro.precoMax) {
            throw new ApiException(400, context.getString(R.string.erro_preco_min_maior_max));
        }
        List<Imovel> filtrados = new ArrayList<>();
        for (Imovel i : carregarImoveis(context)) {
            if (filtro.aceita(i)) {
                filtrados.add(copia(i));
            }
        }
        return Pagina.recortar(filtrados, pagina);
    }

    /** {@code GET /api/imoveis/destaques}: imóveis marcados como destaque. */
    public List<Imovel> destaques() throws ApiException {
        simularRede();
        List<Imovel> destaques = new ArrayList<>();
        for (Imovel i : carregarImoveis(context)) {
            if (i.destaque) {
                destaques.add(copia(i));
            }
        }
        return destaques;
    }

    /** {@code GET /api/imoveis/filtros}: tipos e cidades disponíveis. */
    public Filtro.Opcoes opcoes() throws ApiException {
        simularRede();
        return Filtro.Opcoes.de(carregarImoveis(context));
    }

    /** Imóvel pelo id; 404 se não existir. */
    public Imovel imovel(long id) throws ApiException {
        simularRede();
        for (Imovel i : carregarImoveis(context)) {
            if (i.id == id) {
                return copia(i);
            }
        }
        throw new ApiException(404, null);
    }

    // ---------------------------------------------------------------- autenticação (6.9)

    /**
     * {@code POST /api/auth/login} → {@code { token, usuario }}. 401 credencial inválida; 423 conta
     * bloqueada (já bloqueada ou 3ª falha seguida — RN-002); 403 e-mail não verificado.
     */
    public JSONObject login(String email, String senha) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id;
        String senhaCerta;
        boolean bloqueado;
        boolean verificado;
        int tentativas;
        try (Cursor c = db.query(BancoLocal.SIM_USUARIO,
                new String[]{"id", "senha", "bloqueado", "email_verificado", "tentativas"},
                "email = ?", new String[]{normalizar(email)}, null, null, null)) {
            if (!c.moveToFirst()) {
                throw new ApiException(401, texto(R.string.conta_erro_login_invalido));
            }
            id = c.getLong(0);
            senhaCerta = c.getString(1);
            bloqueado = c.getInt(2) == 1;
            verificado = c.getInt(3) == 1;
            tentativas = c.getInt(4);
        }
        if (bloqueado) {
            throw new ApiException(423, texto(R.string.conta_erro_bloqueada));
        }
        ContentValues valores = new ContentValues();
        if (!senhaCerta.equals(senha)) {
            tentativas++;
            valores.put("tentativas", tentativas);
            boolean bloquear = tentativas >= MAX_TENTATIVAS;
            if (bloquear) {
                valores.put("bloqueado", 1);
            }
            atualizarUsuario(db, id, valores);
            throw bloquear
                    ? new ApiException(423, texto(R.string.conta_erro_bloqueada))
                    : new ApiException(401, texto(R.string.conta_erro_login_invalido));
        }
        valores.put("tentativas", 0);
        atualizarUsuario(db, id, valores);
        if (!verificado) {
            throw new ApiException(403, texto(R.string.conta_erro_email_nao_verificado));
        }
        return sessaoJson(db, id);
    }

    /** {@code POST /api/auth/cadastro} (perfil CLIENTE, e-mail a verificar). 409 se o e-mail já existe. */
    public void cadastrar(String nome, @Nullable String cpf, String email, String senha) throws ApiException {
        simularRede();
        if (Validacoes.vazio(nome)) {
            throw new ApiException(400, texto(R.string.conta_erro_nome_obrigatorio));
        }
        if (!Validacoes.email(email)) {
            throw new ApiException(400, texto(R.string.conta_erro_email_invalido));
        }
        if (!Validacoes.senha(senha)) {
            throw new ApiException(400, texto(R.string.conta_erro_senha_curta));
        }
        if (!Validacoes.vazio(cpf) && !Validacoes.cpf(cpf)) {
            throw new ApiException(400, texto(R.string.conta_erro_cpf_invalido));
        }
        SQLiteDatabase db = banco();
        if (idPorEmail(db, email) != null) {
            throw new ApiException(409, texto(R.string.conta_erro_email_cadastrado));
        }
        ContentValues valores = new ContentValues();
        valores.put("nome", nome.trim());
        valores.put("email", normalizar(email));
        valores.put("senha", senha);
        valores.put("cpf", Validacoes.vazio(cpf) ? null : Formatos.digitos(cpf));
        valores.put("perfil", Usuario.CLIENTE);
        valores.put("bloqueado", 0);
        valores.put("email_verificado", 0);
        valores.put("tentativas", 0);
        db.insertOrThrow(BancoLocal.SIM_USUARIO, null, valores);
    }

    /** {@code POST /api/auth/verificar-email} → {@code { token, usuario }}; código errado → 400. */
    public JSONObject verificarEmail(String email, String codigo) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        Long id = idPorEmail(db, email);
        if (id == null || !CODIGO_VERIFICACAO.equals(codigo)) {
            throw new ApiException(400, texto(R.string.conta_erro_codigo_invalido));
        }
        ContentValues valores = new ContentValues();
        valores.put("email_verificado", 1);
        atualizarUsuario(db, id, valores);
        return sessaoJson(db, id);
    }

    /** {@code POST /api/auth/reenviar-codigo} → 204 (o código simulado é sempre o mesmo). */
    public void reenviarCodigo() throws ApiException {
        simularRede();
    }

    /** {@code POST /api/auth/esqueci-senha} → 204 (não revela se o e-mail existe). */
    public void esqueciSenha() throws ApiException {
        simularRede();
    }

    /** {@code POST /api/auth/redefinir-senha} → 204; código errado ou e-mail desconhecido → 400. */
    public void redefinirSenha(String email, String codigo, String novaSenha) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        Long id = idPorEmail(db, email);
        if (id == null || !CODIGO_VERIFICACAO.equals(codigo)) {
            throw new ApiException(400, texto(R.string.conta_erro_codigo_invalido));
        }
        if (!Validacoes.senha(novaSenha)) {
            throw new ApiException(400, texto(R.string.conta_erro_senha_curta));
        }
        ContentValues valores = new ContentValues();
        valores.put("senha", novaSenha);
        atualizarUsuario(db, id, valores);
    }

    // ---------------------------------------------------------------- usuário logado (6.9)

    /** {@code GET /api/usuarios/me}. */
    public Usuario perfil(@Nullable String token) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        return usuario(db, usuarioDoToken(db, token));
    }

    /** {@code PUT /api/usuarios/me}: nome obrigatório; CPF e telefone, quando informados, válidos. */
    public Usuario salvarPerfil(@Nullable String token, String nome, @Nullable String cpf,
                                @Nullable String telefone) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        if (Validacoes.vazio(nome)) {
            throw new ApiException(400, texto(R.string.conta_erro_nome_obrigatorio));
        }
        if (!Validacoes.vazio(cpf) && !Validacoes.cpf(cpf)) {
            throw new ApiException(400, texto(R.string.conta_erro_cpf_invalido));
        }
        if (!Validacoes.vazio(telefone) && !Validacoes.telefone(telefone)) {
            throw new ApiException(400, texto(R.string.conta_erro_telefone_invalido));
        }
        ContentValues valores = new ContentValues();
        valores.put("nome", nome.trim());
        if (!Validacoes.vazio(cpf)) {
            valores.put("cpf", Formatos.digitos(cpf));
        }
        if (telefone != null) {
            valores.put("telefone", Validacoes.vazio(telefone) ? null : Formatos.telefone(telefone));
        }
        atualizarUsuario(db, id, valores);
        return usuario(db, id);
    }

    /** {@code PUT /api/usuarios/me/senha} → 204; senha atual errada → 400 (não derruba a sessão). */
    public void alterarSenha(@Nullable String token, String senhaAtual, String novaSenha) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        try (Cursor c = db.query(BancoLocal.SIM_USUARIO, new String[]{"senha"}, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (!c.moveToFirst() || !c.getString(0).equals(senhaAtual)) {
                throw new ApiException(400, texto(R.string.conta_erro_senha_atual));
            }
        }
        if (!Validacoes.senha(novaSenha)) {
            throw new ApiException(400, texto(R.string.conta_erro_senha_curta));
        }
        ContentValues valores = new ContentValues();
        valores.put("senha", novaSenha);
        atualizarUsuario(db, id, valores);
    }

    // ---------------------------------------------------------------- favoritos remotos (D10)

    /** {@code GET /api/usuarios/me/favoritos} → imóveis favoritos da conta. */
    public List<Imovel> favoritos(@Nullable String token) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        List<Imovel> favoritos = new ArrayList<>();
        try (Cursor c = db.query(BancoLocal.SIM_FAVORITO, new String[]{"imovel_id"}, "usuario_id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            while (c.moveToNext()) {
                Imovel imovel = imovelDoSeed(c.getLong(0));
                if (imovel != null) {
                    favoritos.add(copia(imovel));
                }
            }
        }
        return favoritos;
    }

    /** {@code PUT /api/usuarios/me/favoritos/{imovelId}} → 204; imóvel inexistente → 404. */
    public void adicionarFavorito(@Nullable String token, long imovelId) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        if (imovelDoSeed(imovelId) == null) {
            throw new ApiException(404, texto(R.string.visita_erro_imovel_nao_encontrado));
        }
        ContentValues valores = new ContentValues();
        valores.put("usuario_id", id);
        valores.put("imovel_id", imovelId);
        db.insertWithOnConflict(BancoLocal.SIM_FAVORITO, null, valores, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** {@code DELETE /api/usuarios/me/favoritos/{imovelId}} → 204 (mesmo se já não existia). */
    public void removerFavorito(@Nullable String token, long imovelId) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        db.delete(BancoLocal.SIM_FAVORITO, "usuario_id = ? AND imovel_id = ?",
                new String[]{String.valueOf(id), String.valueOf(imovelId)});
    }

    // ---------------------------------------------------------------- visitas (D4, D30, D37)

    /**
     * {@code POST /api/visitas} → 201 Visita PENDENTE (gera o lead — RN-004). Data a partir de amanhã,
     * período válido e telefone obrigatório; CPF obrigatório se a conta ainda não tiver. Telefone e
     * CPF informados ficam salvos no perfil (D30).
     */
    public Visita solicitarVisita(@Nullable String token, long imovelId, String data, String periodo,
                                  String telefone, @Nullable String cpf, @Nullable String mensagem)
            throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        if (imovelDoSeed(imovelId) == null) {
            throw new ApiException(404, texto(R.string.visita_erro_imovel_nao_encontrado));
        }
        if (!dataAPartirDeAmanha(data)) {
            throw new ApiException(400, texto(R.string.visita_erro_data));
        }
        if (!Visita.MANHA.equals(periodo) && !Visita.TARDE.equals(periodo) && !Visita.NOITE.equals(periodo)) {
            throw new ApiException(400, texto(R.string.visita_erro_periodo));
        }
        if (!Validacoes.telefone(telefone)) {
            throw new ApiException(400, texto(R.string.conta_erro_telefone_invalido));
        }
        String cpfVisita = usuario(db, id).cpf;
        if (Validacoes.vazio(cpfVisita)) {
            if (Validacoes.vazio(cpf)) {
                throw new ApiException(400, texto(R.string.visita_erro_cpf_obrigatorio));
            }
            if (!Validacoes.cpf(cpf)) {
                throw new ApiException(400, texto(R.string.conta_erro_cpf_invalido));
            }
            cpfVisita = Formatos.digitos(cpf);
        }
        String telefoneFormatado = Formatos.telefone(telefone);

        ContentValues perfil = new ContentValues();
        perfil.put("telefone", telefoneFormatado);
        perfil.put("cpf", cpfVisita);
        atualizarUsuario(db, id, perfil);

        ContentValues valores = new ContentValues();
        valores.put("usuario_id", id);
        valores.put("imovel_id", imovelId);
        valores.put("data", data);
        valores.put("periodo", periodo);
        valores.put("telefone", telefoneFormatado);
        valores.put("cpf", cpfVisita);
        valores.put("mensagem", Validacoes.vazio(mensagem) ? null : mensagem.trim());
        valores.put("status", Visita.PENDENTE);
        valores.put("criada_em", System.currentTimeMillis());
        long visitaId = db.insertOrThrow(BancoLocal.SIM_VISITA, null, valores);
        Log.i(TAG, "Lead de visita criado para o imóvel " + imovelId); // RN-004, sem dado pessoal
        return visita(db, id, visitaId);
    }

    /** {@code GET /api/visitas/minhas}: da mais nova para a mais antiga. */
    public List<Visita> minhasVisitas(@Nullable String token) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        confirmarVisitasPendentes(db, id);
        List<Visita> visitas = new ArrayList<>();
        try (Cursor c = db.query(BancoLocal.SIM_VISITA, COLUNAS_VISITA, "usuario_id = ?",
                new String[]{String.valueOf(id)}, null, null, "criada_em DESC, id DESC")) {
            while (c.moveToNext()) {
                visitas.add(visitaDoCursor(c));
            }
        }
        return visitas;
    }

    /** {@code PATCH /api/visitas/{id}/cancelar}: só PENDENTE ou CONFIRMADA (D37); senão 400. */
    public Visita cancelarVisita(@Nullable String token, long visitaId) throws ApiException {
        simularRede();
        SQLiteDatabase db = banco();
        long id = usuarioDoToken(db, token);
        confirmarVisitasPendentes(db, id);
        if (!visita(db, id, visitaId).podeCancelar()) {
            throw new ApiException(400, texto(R.string.visita_erro_nao_cancelavel));
        }
        ContentValues valores = new ContentValues();
        valores.put("status", Visita.CANCELADA);
        db.update(BancoLocal.SIM_VISITA, valores, "id = ?", new String[]{String.valueOf(visitaId)});
        return visita(db, id, visitaId);
    }

    // ---------------------------------------------------------------- internos de contas e visitas

    private SQLiteDatabase banco() throws ApiException {
        SQLiteDatabase db = BancoLocal.instancia(context).getWritableDatabase();
        copiarUsuarios(db);
        return db;
    }

    /** Primeira execução: copia {@code usuarios.json} para {@code sim_usuario} (D27). */
    private void copiarUsuarios(SQLiteDatabase db) throws ApiException {
        synchronized (ServidorSimulado.class) {
            if (usuariosCopiados) {
                return;
            }
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + BancoLocal.SIM_USUARIO, null)) {
                if (c.moveToFirst() && c.getInt(0) > 0) {
                    usuariosCopiados = true;
                    return;
                }
            }
            try {
                JSONArray lista = new JSONArray(lerAsset(context, ARQUIVO_USUARIOS));
                db.beginTransaction();
                try {
                    for (int k = 0; k < lista.length(); k++) {
                        JSONObject u = lista.getJSONObject(k);
                        ContentValues valores = new ContentValues();
                        valores.put("id", u.getLong("id"));
                        valores.put("nome", u.getString("nome"));
                        valores.put("email", normalizar(u.getString("email")));
                        valores.put("senha", u.getString("senha"));
                        valores.put("cpf", u.isNull("cpf") ? null : u.getString("cpf"));
                        valores.put("telefone", u.isNull("telefone") ? null : u.getString("telefone"));
                        valores.put("perfil", u.getString("perfil"));
                        valores.put("bloqueado", u.optBoolean("bloqueado") ? 1 : 0);
                        valores.put("email_verificado", u.optBoolean("emailVerificado") ? 1 : 0);
                        valores.put("tentativas", 0);
                        db.insertOrThrow(BancoLocal.SIM_USUARIO, null, valores);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                usuariosCopiados = true;
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Falha ao ler " + ARQUIVO_USUARIOS, e);
                throw new ApiException(500, null, e);
            }
        }
    }

    /** Id do usuário do token {@code "simulado-" + id}; inválido → limpa a sessão e devolve 401. */
    private long usuarioDoToken(SQLiteDatabase db, @Nullable String token) throws ApiException {
        if (token != null && token.startsWith(PREFIXO_TOKEN)) {
            try {
                long id = Long.parseLong(token.substring(PREFIXO_TOKEN.length()));
                try (Cursor c = db.query(BancoLocal.SIM_USUARIO, new String[]{"id"}, "id = ?",
                        new String[]{String.valueOf(id)}, null, null, null)) {
                    if (c.moveToFirst()) {
                        return id;
                    }
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Token simulado malformado");
            }
        }
        Sessao.limpar(context);
        throw ApiException.sessaoExpirada(null);
    }

    private static JSONObject sessaoJson(SQLiteDatabase db, long id) throws ApiException {
        try {
            JSONObject resposta = new JSONObject();
            resposta.put("token", PREFIXO_TOKEN + id);
            resposta.put("usuario", usuario(db, id).paraJson());
            return resposta;
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static Usuario usuario(SQLiteDatabase db, long id) throws ApiException {
        try (Cursor c = db.query(BancoLocal.SIM_USUARIO,
                new String[]{"id", "nome", "email", "cpf", "telefone", "perfil"}, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (!c.moveToFirst()) {
                throw new ApiException(404, null);
            }
            Usuario u = new Usuario();
            u.id = c.getLong(0);
            u.nome = c.getString(1);
            u.email = c.getString(2);
            u.cpf = c.isNull(3) ? null : c.getString(3);
            u.telefone = c.isNull(4) ? null : c.getString(4);
            u.perfil = c.getString(5);
            return u;
        }
    }

    @Nullable
    private static Long idPorEmail(SQLiteDatabase db, @Nullable String email) {
        try (Cursor c = db.query(BancoLocal.SIM_USUARIO, new String[]{"id"}, "email = ?",
                new String[]{normalizar(email)}, null, null, null)) {
            return c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    private static void atualizarUsuario(SQLiteDatabase db, long id, ContentValues valores) {
        db.update(BancoLocal.SIM_USUARIO, valores, "id = ?", new String[]{String.valueOf(id)});
    }

    private static String normalizar(@Nullable String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** D37: PENDENTE lida 2 min após {@code criada_em} vira CONFIRMADA. */
    private static void confirmarVisitasPendentes(SQLiteDatabase db, long usuarioId) {
        ContentValues valores = new ContentValues();
        valores.put("status", Visita.CONFIRMADA);
        db.update(BancoLocal.SIM_VISITA, valores, "usuario_id = ? AND status = ? AND criada_em <= ?",
                new String[]{String.valueOf(usuarioId), Visita.PENDENTE,
                        String.valueOf(System.currentTimeMillis() - CONFIRMAR_VISITA_MS)});
    }

    private Visita visita(SQLiteDatabase db, long usuarioId, long visitaId) throws ApiException {
        try (Cursor c = db.query(BancoLocal.SIM_VISITA, COLUNAS_VISITA, "id = ? AND usuario_id = ?",
                new String[]{String.valueOf(visitaId), String.valueOf(usuarioId)}, null, null, null)) {
            if (!c.moveToFirst()) {
                throw new ApiException(404, texto(R.string.visita_erro_nao_encontrada));
            }
            return visitaDoCursor(c);
        }
    }

    private Visita visitaDoCursor(Cursor c) throws ApiException {
        Visita v = new Visita();
        v.id = c.getLong(0);
        v.imovelId = c.getLong(1);
        v.data = c.getString(2);
        v.periodo = c.getString(3);
        v.telefone = c.getString(4);
        v.mensagem = c.isNull(5) ? null : c.getString(5);
        v.status = c.getString(6);
        v.criadaEm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).format(new Date(c.getLong(7)));
        Imovel imovel = imovelDoSeed(v.imovelId);
        if (imovel != null) {
            v.imovelTitulo = imovel.titulo;
            v.imovelFoto = imovel.capa();
        }
        return v;
    }

    @Nullable
    private Imovel imovelDoSeed(long id) throws ApiException {
        for (Imovel i : carregarImoveis(context)) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    /** Data yyyy-MM-dd válida e a partir de amanhã (D30). */
    private static boolean dataAPartirDeAmanha(@Nullable String data) {
        if (data == null || !data.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        formato.setLenient(false);
        try {
            formato.parse(data);
        } catch (ParseException e) {
            return false;
        }
        Calendar amanha = Calendar.getInstance();
        amanha.add(Calendar.DAY_OF_YEAR, 1);
        return data.compareTo(formato.format(amanha.getTime())) >= 0;
    }

    private String texto(int id) {
        return context.getString(id);
    }

    /** Atraso de rede e, se ligado no Modo demonstração, falha de conexão. */
    private void simularRede() throws ApiException {
        try {
            Thread.sleep(ATRASO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Preferencias.simularFalhaRede(context)) {
            throw ApiException.semConexao(null);
        }
    }

    /** Cópia do imóvel do seed: quem recebe pode alterar (ex.: {@code distanciaKm}) sem afetar o seed. */
    private static Imovel copia(Imovel i) throws ApiException {
        try {
            return Imovel.deJson(i.paraJson());
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static synchronized List<Imovel> carregarImoveis(Context context) throws ApiException {
        if (imoveisCarregados == null) {
            try {
                JSONArray lista = new JSONArray(lerAsset(context, ARQUIVO_IMOVEIS));
                List<Imovel> imoveis = new ArrayList<>();
                for (int k = 0; k < lista.length(); k++) {
                    imoveis.add(Imovel.deJson(lista.getJSONObject(k)));
                }
                imoveisCarregados = Collections.unmodifiableList(imoveis);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Falha ao ler " + ARQUIVO_IMOVEIS, e);
                throw new ApiException(500, null, e);
            }
        }
        return imoveisCarregados;
    }

    private static String lerAsset(Context context, String caminho) throws IOException {
        try (InputStream in = context.getAssets().open(caminho);
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                saida.write(buffer, 0, lidos);
            }
            return saida.toString(StandardCharsets.UTF_8.name());
        }
    }
}
