package br.com.talisma.imoveis.dados;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

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
}
