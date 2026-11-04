package br.com.talisma.imoveis.dados;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.talisma.imoveis.R;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Conta do usuário (EAP 2.4.1, D7): login, cadastro, verificação de e-mail, esqueci/redefinir senha,
 * perfil, alterar senha e sair. Decide por {@link Preferencias#modoDemonstracao} entre o
 * {@link ServidorSimulado} e a {@link Api}; a conversão do JSON fica aqui e nos {@code deJson}.
 * <p>
 * Erros para a tela (6.6) — todos chegam como {@link ApiException}:
 * <ul>
 *   <li>login: 401 credencial inválida (erro no campo senha); {@link ApiException#isContaBloqueada()}
 *   (423, RN-002) → diálogo de bloqueio; {@link ApiException#isEmailNaoVerificado()} (403) →
 *   VerificarEmail(CADASTRO); {@link ApiException#PERFIL_INCOMPATIVEL} → "Esta conta não é de
 *   corretor/cliente";</li>
 *   <li>cadastro: 409 e-mail já cadastrado (erro no campo);</li>
 *   <li>chamadas logadas: {@link ApiException#isSessaoExpirada()} → sessão já limpa; abrir o Login
 *   com retorno e {@link ApiException#mensagemParaUsuario} ("Sua sessão expirou");</li>
 *   <li>demais: {@link ApiException#mensagemParaUsuario}.</li>
 * </ul>
 * Ao entrar (login ou verificação de e-mail) a {@link Sessao} é salva e os favoritos são
 * sincronizados em segundo plano (D10).
 */
public class ContaRepository {

    private final Context context;

    public ContaRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // ---------------------------------------------------------------- autenticação

    /**
     * Login. {@code perfil} = modo da tela ({@link Usuario#CLIENTE} ou {@link Usuario#CORRETOR});
     * conta de outro perfil não entra ({@link ApiException#PERFIL_INCOMPATIVEL}). {@code null} aceita
     * qualquer perfil.
     */
    public void entrar(String email, String senha, @Nullable String perfil, Tarefas.Callback<Usuario> callback) {
        Tarefas.executar(() -> {
            JSONObject resposta;
            if (simulado()) {
                resposta = new ServidorSimulado(context).login(email, senha);
            } else {
                JSONObject corpo = new JSONObject();
                colocar(corpo, "email", email.trim());
                colocar(corpo, "senha", senha);
                resposta = Api.objeto(new Api(context).post(ApiRotas.LOGIN, corpo));
            }
            return iniciarSessao(resposta, perfil);
        }, callback);
    }

    /** Cadastro de cliente (CPF opcional — D12); depois, VerificarEmail(CADASTRO). 409 = e-mail existente. */
    public void cadastrar(String nome, @Nullable String cpf, String email, String senha,
                          Tarefas.Callback<Void> callback) {
        String cpfDigitos = Validacoes.vazio(cpf) ? null : Formatos.digitos(cpf);
        Tarefas.executar(() -> {
            if (simulado()) {
                new ServidorSimulado(context).cadastrar(nome, cpfDigitos, email, senha);
            } else {
                JSONObject corpo = new JSONObject();
                colocar(corpo, "nome", nome.trim());
                colocar(corpo, "cpf", cpfDigitos);
                colocar(corpo, "email", email.trim());
                colocar(corpo, "senha", senha);
                new Api(context).post(ApiRotas.CADASTRO, corpo);
            }
            return null;
        }, callback);
    }

    /** Valida o código de 4 dígitos do cadastro e já entra (login automático — 6.5). */
    public void verificarEmail(String email, String codigo, Tarefas.Callback<Usuario> callback) {
        Tarefas.executar(() -> {
            JSONObject resposta;
            if (simulado()) {
                resposta = new ServidorSimulado(context).verificarEmail(email, codigo);
            } else {
                JSONObject corpo = new JSONObject();
                colocar(corpo, "email", email.trim());
                colocar(corpo, "codigo", codigo);
                resposta = Api.objeto(new Api(context).post(ApiRotas.VERIFICAR_EMAIL, corpo));
            }
            return iniciarSessao(resposta, null);
        }, callback);
    }

    /** "Não recebeu? Reenviar código". */
    public void reenviarCodigo(String email, Tarefas.Callback<Void> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                new ServidorSimulado(context).reenviarCodigo();
            } else {
                new Api(context).post(ApiRotas.REENVIAR_CODIGO, corpoEmail(email));
            }
            return null;
        }, callback);
    }

    /** Esqueci a senha, passo 1 (D29): envia o código ao e-mail. */
    public void esqueciSenha(String email, Tarefas.Callback<Void> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                new ServidorSimulado(context).esqueciSenha();
            } else {
                new Api(context).post(ApiRotas.ESQUECI_SENHA, corpoEmail(email));
            }
            return null;
        }, callback);
    }

    /**
     * Esqueci a senha, passo 3 (D29): código (VerificarEmail em modo REDEFINICAO) + nova senha.
     * Código errado → 400 com a mensagem da API.
     */
    public void redefinirSenha(String email, String codigo, String novaSenha, Tarefas.Callback<Void> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                new ServidorSimulado(context).redefinirSenha(email, codigo, novaSenha);
            } else {
                JSONObject corpo = corpoEmail(email);
                colocar(corpo, "codigo", codigo);
                colocar(corpo, "novaSenha", novaSenha);
                new Api(context).post(ApiRotas.REDEFINIR_SENHA, corpo);
            }
            return null;
        }, callback);
    }

    // ---------------------------------------------------------------- usuário logado

    /**
     * Perfil atualizado do servidor (Dados pessoais, Solicitar visita). Traz o CPF — só para exibir
     * mascarado ou saber se falta (D12); a {@link Sessao} guarda o resto, nunca o CPF.
     */
    public void perfil(Tarefas.Callback<Usuario> callback) {
        Tarefas.executar(() -> {
            Usuario usuario;
            if (simulado()) {
                usuario = new ServidorSimulado(context).perfil(Sessao.token(context));
            } else {
                usuario = usuarioDeJson(Api.objeto(new Api(context).get(ApiRotas.PERFIL)));
            }
            Sessao.atualizarUsuario(context, usuario);
            return usuario;
        }, callback);
    }

    /** Salva nome, CPF (só se informado) e telefone (D12, D30). */
    public void salvarPerfil(String nome, @Nullable String cpf, @Nullable String telefone,
                             Tarefas.Callback<Usuario> callback) {
        String cpfDigitos = Validacoes.vazio(cpf) ? null : Formatos.digitos(cpf);
        Tarefas.executar(() -> {
            Usuario usuario;
            if (simulado()) {
                usuario = new ServidorSimulado(context).salvarPerfil(Sessao.token(context), nome, cpfDigitos, telefone);
            } else {
                JSONObject corpo = new JSONObject();
                colocar(corpo, "nome", nome.trim());
                colocar(corpo, "cpf", cpfDigitos);
                colocar(corpo, "telefone", telefone);
                usuario = usuarioDeJson(Api.objeto(new Api(context).put(ApiRotas.PERFIL, corpo)));
            }
            Sessao.atualizarUsuario(context, usuario);
            return usuario;
        }, callback);
    }

    /** Alterar senha logado; senha atual errada → 400 com a mensagem da API (a sessão continua). */
    public void alterarSenha(String senhaAtual, String novaSenha, Tarefas.Callback<Void> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                new ServidorSimulado(context).alterarSenha(Sessao.token(context), senhaAtual, novaSenha);
            } else {
                JSONObject corpo = new JSONObject();
                colocar(corpo, "senhaAtual", senhaAtual);
                colocar(corpo, "novaSenha", novaSenha);
                new Api(context).put(ApiRotas.ALTERAR_SENHA, corpo);
            }
            return null;
        }, callback);
    }

    /** Sair (D35): limpa a sessão. Os favoritos locais ficam (D10). */
    public void sair() {
        Sessao.limpar(context);
    }

    // ---------------------------------------------------------------- modo demonstração (D26)

    /**
     * Liga/desliga o Modo demonstração (6.7): salva em {@link Preferencias}, limpa a sessão e o cache
     * da vitrine, e deixa os favoritos locais como "não sincronizados" para irem à conta do outro
     * servidor no próximo login. Antes, a tela pode avisar com
     * {@link FavoritoRepository#naoSincronizados}.
     */
    public void alternarModoDemonstracao(boolean ligado) {
        if (Preferencias.modoDemonstracao(context) == ligado) {
            return;
        }
        Preferencias.setModoDemonstracao(context, ligado);
        Sessao.limpar(context);
        new ImovelRepository(context).limparCache();
        new FavoritoRepository(context).desvincularDoServidor();
    }

    // ---------------------------------------------------------------- internos

    private boolean simulado() {
        return Preferencias.modoDemonstracao(context);
    }

    /** {@code { token, usuario }} → confere o perfil, salva a sessão e sincroniza os favoritos. */
    private Usuario iniciarSessao(JSONObject resposta, @Nullable String perfil) throws ApiException {
        String token;
        Usuario usuario;
        try {
            token = resposta.getString("token");
            usuario = Usuario.deJson(resposta.getJSONObject("usuario"));
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
        if (perfil != null && !perfil.equals(usuario.perfil)) {
            throw new ApiException(ApiException.PERFIL_INCOMPATIVEL, context.getString(
                    Usuario.CORRETOR.equals(perfil) ? R.string.conta_erro_nao_corretor : R.string.conta_erro_nao_cliente));
        }
        Sessao.salvar(context, token, usuario);
        new FavoritoRepository(context).sincronizarEmSegundoPlano();
        return usuario;
    }

    private static Usuario usuarioDeJson(JSONObject o) throws ApiException {
        try {
            return Usuario.deJson(o);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static JSONObject corpoEmail(String email) {
        JSONObject corpo = new JSONObject();
        colocar(corpo, "email", email.trim());
        return corpo;
    }

    /** {@code put} sem exceção verificada; valor {@code null} omite a chave (campo opcional). */
    private static void colocar(JSONObject corpo, String chave, @Nullable Object valor) {
        try {
            corpo.putOpt(chave, valor);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }
}
