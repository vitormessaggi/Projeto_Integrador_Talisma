package br.com.talisma.imoveis.dados;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Imóvel publicado (6.8). Conversão JSON ↔ modelo em {@link #deJson}/{@link #paraJson} (D25). */
public class Imovel {

    public static final String VENDA = "VENDA";
    public static final String LOCACAO = "LOCACAO";
    public static final String CASA = "CASA";
    public static final String APARTAMENTO = "APARTAMENTO";
    public static final String TERRENO = "TERRENO";

    public long id;
    public long ofertaId;
    public JSONArray ofertas = new JSONArray();
    public String titulo;
    /** {@link #VENDA} | {@link #LOCACAO} */
    public String finalidade;
    /** {@link #CASA} | {@link #APARTAMENTO} | {@link #TERRENO} */
    public String tipo;
    public double preco;
    public String bairro;
    public String cidade;
    public String uf;
    public String endereco;
    public int quartos;
    public int banheiros;
    public int vagas;
    @Nullable
    public Double area;
    public String descricao;
    @Nullable
    public Double latitude;
    @Nullable
    public Double longitude;
    public boolean destaque;
    public long corretorId;
    /** Nome do arquivo em assets/fake/fotos, caminho local absoluto ou URL (ver CarregadorImagem). */
    public List<String> fotos = new ArrayList<>();
    /** Calculada no app ("Perto de mim" — D15); não vem nem vai para o JSON. */
    @Nullable
    public Float distanciaKm;
    /** {@code true} quando veio do cache porque a rede falhou (banner offline — D36); fora do JSON. */
    public boolean offline;

    public void selecionarOferta(String preferida) {
        JSONObject escolhida = ofertas.optJSONObject(0);
        for (int n = 0; n < ofertas.length(); n++) {
            JSONObject oferta = ofertas.optJSONObject(n);
            if (oferta != null && oferta.optString("finalidade").equals(preferida)) escolhida = oferta;
        }
        if (escolhida != null) {
            ofertaId = escolhida.optLong("id", 0);
            finalidade = escolhida.optString("finalidade"); preco = escolhida.optDouble("preco");
        }
    }

    public boolean isLocacao() {
        return LOCACAO.equals(finalidade);
    }

    @Nullable
    public String capa() {
        return fotos.isEmpty() ? null : fotos.get(0);
    }

    public static Imovel deJson(JSONObject o) throws JSONException {
        JSONObject detalhe = o;
        if (o.has("imovel")) {
            JSONObject resumo = o.getJSONObject("imovel");
            JSONObject dados = o.optJSONObject("dados");
            o = dados == null ? new JSONObject(resumo.toString()) : new JSONObject(dados.toString());
            o.put("id", resumo.getLong("id"));
            if (dados == null) {
                o.put("descricao", detalhe.optString("descricao", ""));
                o.put("latitude", detalhe.opt("latitude")); o.put("longitude", detalhe.opt("longitude"));
            } else {
                JSONArray fotosGestao = new JSONArray();
                JSONArray ids = detalhe.optJSONArray("fotos");
                for (int n = 0; ids != null && n < ids.length(); n++) fotosGestao.put(ApiRotas.BASE + ApiRotas.IMOVEIS_CORRETOR + "/" + resumo.getLong("id") + "/fotos/" + ids.getLong(n));
                o.put("fotos", fotosGestao);
            }
        }
        Imovel i = new Imovel();
        i.id = o.getLong("id");
        i.titulo = texto(o, "titulo");
        i.finalidade = texto(o, "finalidade");
        i.tipo = texto(o, "tipo");
        i.preco = o.optDouble("preco", 0);
        i.bairro = texto(o, "bairro");
        i.cidade = texto(o, "cidade");
        i.uf = texto(o, "uf");
        i.endereco = texto(o, "endereco");
        i.quartos = o.optInt("quartos", 0);
        i.banheiros = o.optInt("banheiros", 0);
        i.vagas = o.optInt("vagas", 0);
        i.area = numero(o, o.has("areaTotal") ? "areaTotal" : "area");
        i.descricao = texto(o, "descricao");
        i.latitude = numero(o, "latitude");
        i.longitude = numero(o, "longitude");
        i.destaque = o.optBoolean("destaque", false);
        i.corretorId = o.optLong("corretorId", 0);
        JSONArray fotos = o.optJSONArray("fotos");
        if (fotos != null) {
            for (int k = 0; k < fotos.length(); k++) {
                String foto = fotos.getString(k);
                i.fotos.add(foto.startsWith("/api/") ? ApiRotas.BASE + foto : foto);
            }
        }
        i.ofertas = o.optJSONArray("ofertas") == null ? new JSONArray() : o.getJSONArray("ofertas");
        i.ofertaId = o.optLong("ofertaId", 0);
        i.selecionarOferta(i.finalidade);
        if (!o.isNull("distanciaKm")) i.distanciaKm = (float) o.getDouble("distanciaKm");
        return i;
    }

    public JSONObject paraJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("ofertas", ofertas);
            o.put("ofertaId", ofertaId);
            o.put("distanciaKm", distanciaKm);
            o.put("titulo", titulo);
            o.put("finalidade", finalidade);
            o.put("tipo", tipo);
            o.put("preco", preco);
            o.put("bairro", bairro);
            o.put("cidade", cidade);
            o.put("uf", uf);
            o.put("endereco", endereco);
            o.put("quartos", quartos);
            o.put("banheiros", banheiros);
            o.put("vagas", vagas);
            o.put("area", area != null ? area : JSONObject.NULL);
            o.put("descricao", descricao);
            o.put("latitude", latitude != null ? latitude : JSONObject.NULL);
            o.put("longitude", longitude != null ? longitude : JSONObject.NULL);
            o.put("destaque", destaque);
            o.put("corretorId", corretorId);
            o.put("fotos", new JSONArray(fotos));
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    @Nullable
    private static String texto(JSONObject o, String chave) {
        return o.isNull(chave) ? null : o.optString(chave, null);
    }

    @Nullable
    private static Double numero(JSONObject o, String chave) throws JSONException {
        return o.isNull(chave) ? null : o.getDouble(chave);
    }
}
