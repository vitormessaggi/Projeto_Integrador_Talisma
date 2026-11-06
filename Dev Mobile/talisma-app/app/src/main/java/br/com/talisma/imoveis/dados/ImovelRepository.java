package br.com.talisma.imoveis.dados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.talisma.imoveis.util.Localizacao;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Imóveis da vitrine (D7): lista com filtros e paginação, destaques, detalhe e opções de filtro.
 * Decide por {@link Preferencias#modoDemonstracao} entre o {@link ServidorSimulado} e a {@link Api}.
 * <p>
 * Cache (D36): sempre online; cada resposta boa é salva no {@link BancoLocal} e só é usada quando a
 * rede falha — o resultado chega com {@code offline = true} (banner "Você está offline"). Sem a
 * lista exata em cache, o filtro roda no app sobre os imóveis já vistos (D16); sem nada visto, o
 * erro de rede segue para a tela (estado OFFLINE).
 * <p>
 * Buscas recentes e último filtro (2.3.3) ficam na tabela {@code busca_recente}.
 */
public class ImovelRepository {

    private static final String TAG = "ImovelRepository";
    private static final String CHAVE_DESTAQUES = "destaques";
    private static final String CHAVE_OPCOES = "opcoes";
    private static final String PREFIXO_LISTA = "lista:";
    /** "Perto de mim" ordena tudo o que o filtro devolve; limite de páginas lidas para isso. */
    private static final int MAX_PAGINAS_PERTO = 10;
    /** Cidades das buscas recentes mostradas no sheet de cidade (6.5). */
    private static final int MAX_CIDADES_RECENTES = 5;

    private final Context context;

    public ImovelRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // ---------------------------------------------------------------- vitrine

    /** Página {@code pagina} (base 0, 10 por página — D28) dos imóveis que passam no filtro. */
    public void listar(@Nullable Filtro filtro, int pagina, Tarefas.Callback<Pagina<Imovel>> callback) {
        listar(filtro, pagina, null, callback);
    }

    /**
     * Como {@link #listar(Filtro, int, Tarefas.Callback)}; com "Perto de mim" ativo e {@code origem}
     * conhecida, calcula a distância de cada imóvel e devolve tudo numa página só, ordenada da mais
     * perto para a mais longe (D15). Imóveis sem coordenadas vão para o fim.
     */
    public void listar(@Nullable Filtro filtro, int pagina, @Nullable Location origem,
                       Tarefas.Callback<Pagina<Imovel>> callback) {
        Filtro f = filtro != null ? filtro : new Filtro();
        Tarefas.executar(() -> f.pertoDeMim && origem != null
                ? listarPorDistancia(f, pagina, origem)
                : buscarPagina(f, pagina), callback);
    }

    /** Imóveis em destaque da Home. */
    public void destaques(Tarefas.Callback<List<Imovel>> callback) {
        Tarefas.executar(() -> {
            try {
                List<Imovel> destaques;
                if (simulado()) {
                    destaques = new ServidorSimulado(context).destaques();
                } else {
                    destaques = imoveisDeJson(Api.lista(new Api(context).get(ApiRotas.DESTAQUES)));
                }
                salvarCacheLista(CHAVE_DESTAQUES, imoveisParaJson(destaques).toString());
                return destaques;
            } catch (ApiException e) {
                if (!e.isSemConexao()) {
                    throw e;
                }
                List<Imovel> cache = destaquesDoCache();
                if (cache.isEmpty()) {
                    throw e;
                }
                return cache;
            }
        }, callback);
    }

    /** Detalhe do imóvel; guarda os 30 mais recentes para uso offline (D36). */
    public void detalhe(long id, Tarefas.Callback<Imovel> callback) {
        Tarefas.executar(() -> {
            try {
                Imovel imovel;
                if (simulado()) {
                    imovel = new ServidorSimulado(context).imovel(id);
                } else {
                    imovel = imovelDeJson(Api.objeto(new Api(context).get(ApiRotas.imovel(id))));
                }
                salvarCacheImovel(imovel);
                return imovel;
            } catch (ApiException e) {
                if (!e.isSemConexao()) {
                    throw e;
                }
                Imovel cache = imovelDoCache(id);
                if (cache == null) {
                    throw e;
                }
                return cache;
            }
        }, callback);
    }

    /** Tipos e cidades para os filtros da Home e de Imóveis. */
    public void opcoes(Tarefas.Callback<Filtro.Opcoes> callback) {
        Tarefas.executar(() -> {
            try {
                Filtro.Opcoes opcoes;
                if (simulado()) {
                    opcoes = new ServidorSimulado(context).opcoes();
                } else {
                    opcoes = opcoesDeJson(Api.objeto(new Api(context).get(ApiRotas.OPCOES_FILTRO)));
                }
                salvarCacheLista(CHAVE_OPCOES, opcoes.paraJson().toString());
                return opcoes;
            } catch (ApiException e) {
                if (!e.isSemConexao()) {
                    throw e;
                }
                Filtro.Opcoes cache = opcoesDoCache();
                if (cache == null) {
                    throw e;
                }
                return cache;
            }
        }, callback);
    }

    /** Apaga o cache da vitrine (ao alternar o Modo demonstração — 6.7). Buscas recentes ficam. */
    public void limparCache() {
        Tarefas.emSegundoPlano(() -> {
            SQLiteDatabase db = BancoLocal.instancia(context).getWritableDatabase();
            db.delete(BancoLocal.CACHE_LISTA, null, null);
            db.delete(BancoLocal.CACHE_IMOVEL, null, null);
        });
    }

    // ---------------------------------------------------------------- buscas recentes (2.3.3)

    /** Registra o filtro aplicado como busca mais recente (mantém as 10 últimas). Filtro vazio é ignorado. */
    public void salvarBusca(Filtro filtro) {
        if (filtro.vazio()) {
            return;
        }
        String json = filtro.paraJson().toString();
        Tarefas.emSegundoPlano(() -> {
            SQLiteDatabase db = BancoLocal.instancia(context).getWritableDatabase();
            ContentValues valores = new ContentValues();
            valores.put("filtro_json", json);
            valores.put("criada_em", System.currentTimeMillis());
            db.insertWithOnConflict(BancoLocal.BUSCA_RECENTE, null, valores, SQLiteDatabase.CONFLICT_REPLACE);
            db.execSQL("DELETE FROM " + BancoLocal.BUSCA_RECENTE + " WHERE id NOT IN (SELECT id FROM "
                    + BancoLocal.BUSCA_RECENTE + " ORDER BY criada_em DESC, id DESC LIMIT "
                    + BancoLocal.MAX_BUSCA_RECENTE + ")");
        });
    }

    /** Buscas recentes, da mais nova para a mais antiga (até 10). */
    public void buscasRecentes(Tarefas.Callback<List<Filtro>> callback) {
        Tarefas.executar(this::lerBuscas, callback);
    }

    /** Último filtro aplicado (Imóveis aberta sem extra — 6.5) ou {@code null}. */
    public void ultimoFiltro(Tarefas.Callback<Filtro> callback) {
        Tarefas.executar(() -> {
            List<Filtro> buscas = lerBuscas();
            return buscas.isEmpty() ? null : buscas.get(0);
        }, callback);
    }

    /** Cidades das buscas recentes, sem repetir (até 5), para o topo do sheet de cidade (6.5). */
    public void cidadesRecentes(Tarefas.Callback<List<String>> callback) {
        Tarefas.executar(() -> {
            Set<String> cidades = new LinkedHashSet<>();
            for (Filtro f : lerBuscas()) {
                if (f.cidade != null && cidades.size() < MAX_CIDADES_RECENTES) {
                    cidades.add(f.cidade);
                }
            }
            return new ArrayList<>(cidades);
        }, callback);
    }

    // ---------------------------------------------------------------- internos

    private boolean simulado() {
        return Preferencias.modoDemonstracao(context);
    }

    private Pagina<Imovel> buscarPagina(Filtro filtro, int pagina) throws ApiException {
        String chave = PREFIXO_LISTA + chaveFiltro(filtro) + ":" + pagina;
        try {
            Pagina<Imovel> resultado;
            if (simulado()) {
                resultado = new ServidorSimulado(context).listar(filtro, pagina);
            } else {
                resultado = paginaDeJson(Api.objeto(new Api(context).get(ApiRotas.imoveis(filtro, pagina))));
            }
            salvarCacheLista(chave, resultado.paraJson(Imovel::paraJson).toString());
            return resultado;
        } catch (ApiException e) {
            if (!e.isSemConexao()) {
                throw e;
            }
            Pagina<Imovel> cache = paginaDoCache(chave, filtro, pagina);
            if (cache == null) {
                throw e;
            }
            return cache;
        }
    }

    private Pagina<Imovel> listarPorDistancia(Filtro filtro, int pagina, Location origem) throws ApiException {
        Pagina<Imovel> resultado = new Pagina<>();
        resultado.pagina = pagina;
        resultado.totalPaginas = 1;
        if (pagina > 0) {
            return resultado; // tudo já veio na página 0
        }
        int p = 0;
        Pagina<Imovel> atual;
        do {
            atual = buscarPagina(filtro, p++);
            resultado.itens.addAll(atual.itens);
            resultado.offline |= atual.offline;
        } while (atual.temProxima() && p < MAX_PAGINAS_PERTO);
        Localizacao.ordenarPorDistancia(context, origem, resultado.itens);
        return resultado;
    }

    /** Filtro sem "Perto de mim" (não muda o que a API devolve), com as chaves sempre na mesma ordem. */
    private static String chaveFiltro(Filtro filtro) {
        JSONObject json = filtro.paraJson();
        json.remove("pertoDeMim");
        return json.toString();
    }

    private void salvarCacheLista(String chave, String json) {
        ContentValues valores = new ContentValues();
        valores.put("chave", chave);
        valores.put("json", json);
        valores.put("atualizado_em", System.currentTimeMillis());
        BancoLocal.instancia(context).getWritableDatabase()
                .insertWithOnConflict(BancoLocal.CACHE_LISTA, null, valores, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void salvarCacheImovel(Imovel imovel) {
        SQLiteDatabase db = BancoLocal.instancia(context).getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("id", imovel.id);
        valores.put("json", imovel.paraJson().toString());
        valores.put("visto_em", System.currentTimeMillis());
        db.insertWithOnConflict(BancoLocal.CACHE_IMOVEL, null, valores, SQLiteDatabase.CONFLICT_REPLACE);
        db.execSQL("DELETE FROM " + BancoLocal.CACHE_IMOVEL + " WHERE id NOT IN (SELECT id FROM "
                + BancoLocal.CACHE_IMOVEL + " ORDER BY visto_em DESC LIMIT " + BancoLocal.MAX_CACHE_IMOVEL + ")");
    }

    @Nullable
    private String lerCacheLista(String chave) {
        try (Cursor c = BancoLocal.instancia(context).getReadableDatabase().query(BancoLocal.CACHE_LISTA,
                new String[]{"json"}, "chave = ?", new String[]{chave}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    @Nullable
    private Pagina<Imovel> paginaDoCache(String chave, Filtro filtro, int pagina) {
        Pagina<Imovel> resultado = null;
        String json = lerCacheLista(chave);
        if (json != null) {
            try {
                resultado = Pagina.deJson(new JSONObject(json), Imovel::deJson);
            } catch (JSONException e) {
                Log.w(TAG, "Cache de lista ilegível", e);
            }
        }
        if (resultado == null) {
            List<Imovel> filtrados = new ArrayList<>();
            for (Imovel i : imoveisDoCache().values()) {
                if (filtro.aceita(i)) {
                    filtrados.add(i);
                }
            }
            resultado = Pagina.recortar(filtrados, pagina);
            if (resultado.itens.isEmpty()) {
                return null;
            }
        }
        resultado.offline = true;
        for (Imovel i : resultado.itens) {
            i.offline = true;
        }
        return resultado;
    }

    private List<Imovel> destaquesDoCache() {
        List<Imovel> destaques = new ArrayList<>();
        String json = lerCacheLista(CHAVE_DESTAQUES);
        if (json != null) {
            try {
                destaques = imoveisDeJson(new JSONArray(json));
            } catch (JSONException | ApiException e) {
                Log.w(TAG, "Cache de destaques ilegível", e);
            }
        }
        if (destaques.isEmpty()) {
            for (Imovel i : imoveisDoCache().values()) {
                if (i.destaque) {
                    destaques.add(i);
                }
            }
        }
        for (Imovel i : destaques) {
            i.offline = true;
        }
        return destaques;
    }

    @Nullable
    private Imovel imovelDoCache(long id) {
        Imovel imovel = imoveisDoCache().get(id);
        if (imovel != null) {
            imovel.offline = true;
        }
        return imovel;
    }

    @Nullable
    private Filtro.Opcoes opcoesDoCache() {
        String json = lerCacheLista(CHAVE_OPCOES);
        if (json != null) {
            try {
                return Filtro.Opcoes.deJson(new JSONObject(json));
            } catch (JSONException e) {
                Log.w(TAG, "Cache de opções ilegível", e);
            }
        }
        Map<Long, Imovel> vistos = imoveisDoCache();
        return vistos.isEmpty() ? null : Filtro.Opcoes.de(new ArrayList<>(vistos.values()));
    }

    /** Todos os imóveis já vistos (detalhes, listas e destaques em cache), sem repetir, por id. */
    private Map<Long, Imovel> imoveisDoCache() {
        Map<Long, Imovel> imoveis = new LinkedHashMap<>();
        SQLiteDatabase db = BancoLocal.instancia(context).getReadableDatabase();
        try (Cursor c = db.query(BancoLocal.CACHE_IMOVEL, new String[]{"json"},
                null, null, null, null, "visto_em DESC")) {
            while (c.moveToNext()) {
                try {
                    Imovel i = Imovel.deJson(new JSONObject(c.getString(0)));
                    imoveis.put(i.id, i);
                } catch (JSONException e) {
                    Log.w(TAG, "Imóvel em cache ilegível", e);
                }
            }
        }
        try (Cursor c = db.query(BancoLocal.CACHE_LISTA, new String[]{"chave", "json"},
                null, null, null, null, "atualizado_em DESC")) {
            while (c.moveToNext()) {
                String chave = c.getString(0);
                try {
                    JSONArray itens;
                    if (CHAVE_DESTAQUES.equals(chave)) {
                        itens = new JSONArray(c.getString(1));
                    } else if (chave.startsWith(PREFIXO_LISTA)) {
                        itens = new JSONObject(c.getString(1)).optJSONArray("content");
                    } else {
                        continue;
                    }
                    for (int k = 0; itens != null && k < itens.length(); k++) {
                        Imovel i = Imovel.deJson(itens.getJSONObject(k));
                        if (!imoveis.containsKey(i.id)) {
                            imoveis.put(i.id, i);
                        }
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Lista em cache ilegível", e);
                }
            }
        }
        return imoveis;
    }

    private List<Filtro> lerBuscas() {
        List<Filtro> buscas = new ArrayList<>();
        try (Cursor c = BancoLocal.instancia(context).getReadableDatabase().query(BancoLocal.BUSCA_RECENTE,
                new String[]{"filtro_json"}, null, null, null, null, "criada_em DESC, id DESC")) {
            while (c.moveToNext()) {
                try {
                    buscas.add(Filtro.deJson(new JSONObject(c.getString(0))));
                } catch (JSONException e) {
                    Log.w(TAG, "Busca recente ilegível", e);
                }
            }
        }
        return buscas;
    }

    // ---------------------------------------------------------------- conversão da API (deJson dos modelos)

    private static Pagina<Imovel> paginaDeJson(JSONObject o) throws ApiException {
        try {
            return Pagina.deJson(o, Imovel::deJson);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static Imovel imovelDeJson(JSONObject o) throws ApiException {
        try {
            return Imovel.deJson(o);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static Filtro.Opcoes opcoesDeJson(JSONObject o) throws ApiException {
        try {
            return Filtro.Opcoes.deJson(o);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private static List<Imovel> imoveisDeJson(JSONArray lista) throws ApiException {
        List<Imovel> imoveis = new ArrayList<>();
        try {
            for (int k = 0; k < lista.length(); k++) {
                imoveis.add(Imovel.deJson(lista.getJSONObject(k)));
            }
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
        return imoveis;
    }

    private static JSONArray imoveisParaJson(List<Imovel> imoveis) {
        JSONArray lista = new JSONArray();
        for (Imovel i : imoveis) {
            lista.put(i.paraJson());
        }
        return lista;
    }
}
