package br.com.talisma.imoveis.dados;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.com.talisma.imoveis.util.Formatos;

/**
 * Filtro da lista de imóveis (6.8, D16, D32). Serializado em JSON para o extra {@code filtro},
 * a chave de {@code cache_lista} e {@code busca_recente}.
 */
public class Filtro {

    /** {@link Imovel#VENDA} | {@link Imovel#LOCACAO} */
    @Nullable
    public String finalidade;
    @Nullable
    public String tipo;
    @Nullable
    public String cidade;
    @Nullable
    public Double precoMin;
    @Nullable
    public Double precoMax;
    @Nullable
    public Integer quartosMin;
    public boolean pertoDeMim;

    public static Filtro deJson(JSONObject o) throws JSONException {
        Filtro f = new Filtro();
        f.finalidade = o.isNull("finalidade") ? null : o.optString("finalidade", null);
        f.tipo = o.isNull("tipo") ? null : o.optString("tipo", null);
        f.cidade = o.isNull("cidade") ? null : o.optString("cidade", null);
        f.precoMin = o.isNull("precoMin") ? null : o.getDouble("precoMin");
        f.precoMax = o.isNull("precoMax") ? null : o.getDouble("precoMax");
        f.quartosMin = o.isNull("quartosMin") ? null : o.getInt("quartosMin");
        f.pertoDeMim = o.optBoolean("pertoDeMim", false);
        return f;
    }

    /** JSON com as chaves sempre na mesma ordem (serve de chave de cache). Campos nulos são omitidos. */
    public JSONObject paraJson() {
        JSONObject o = new JSONObject();
        try {
            o.putOpt("finalidade", finalidade);
            o.putOpt("tipo", tipo);
            o.putOpt("cidade", cidade);
            o.putOpt("precoMin", precoMin);
            o.putOpt("precoMax", precoMax);
            o.putOpt("quartosMin", quartosMin);
            if (pertoDeMim) {
                o.put("pertoDeMim", true);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    /** Filtro sem nenhum critério (nem "Perto de mim"). */
    public boolean vazio() {
        return paraJson().length() == 0;
    }

    /**
     * {@code true} se o imóvel passa no filtro (D16). Usado pelo {@link ServidorSimulado} e pelo
     * cache offline, que filtram no app. "Perto de mim" não filtra: só ordena (D15).
     */
    public boolean aceita(Imovel i) {
        if (finalidade != null && !finalidade.equals(i.finalidade)) {
            return false;
        }
        if (tipo != null && !tipo.equals(i.tipo)) {
            return false;
        }
        if (cidade != null && !cidade.equalsIgnoreCase(i.cidade)) {
            return false;
        }
        if (precoMin != null && i.preco < precoMin) {
            return false;
        }
        if (precoMax != null && i.preco > precoMax) {
            return false;
        }
        return quartosMin == null || i.quartos >= quartosMin;
    }

    /** Opções de filtro ({@code GET /api/imoveis/filtros} → {@code { tipos, cidades }} — 6.9). */
    public static class Opcoes {

        public List<String> tipos = new ArrayList<>();
        public List<String> cidades = new ArrayList<>();

        /** Tipos e cidades presentes nos imóveis (simulado e cache offline); cidades em ordem alfabética. */
        public static Opcoes de(List<Imovel> imoveis) {
            Set<String> tipos = new LinkedHashSet<>();
            Set<String> cidades = new LinkedHashSet<>();
            for (String t : new String[]{Imovel.CASA, Imovel.APARTAMENTO, Imovel.TERRENO}) {
                for (Imovel i : imoveis) {
                    if (t.equals(i.tipo)) {
                        tipos.add(t);
                        break;
                    }
                }
            }
            for (Imovel i : imoveis) {
                if (i.cidade != null && !i.cidade.isEmpty()) {
                    cidades.add(i.cidade);
                }
            }
            Opcoes o = new Opcoes();
            o.tipos.addAll(tipos);
            o.cidades.addAll(cidades);
            Collections.sort(o.cidades, Collator.getInstance(Formatos.PT_BR));
            return o;
        }

        public static Opcoes deJson(JSONObject o) throws JSONException {
            Opcoes opcoes = new Opcoes();
            JSONArray tipos = o.optJSONArray("tipos");
            if (tipos != null) {
                for (int k = 0; k < tipos.length(); k++) {
                    opcoes.tipos.add(tipos.getString(k));
                }
            }
            JSONArray cidades = o.optJSONArray("cidades");
            if (cidades != null) {
                for (int k = 0; k < cidades.length(); k++) {
                    opcoes.cidades.add(cidades.getString(k));
                }
            }
            return opcoes;
        }

        public JSONObject paraJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("tipos", new JSONArray(tipos));
                o.put("cidades", new JSONArray(cidades));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return o;
        }
    }
}
