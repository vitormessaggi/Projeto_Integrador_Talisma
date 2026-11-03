package br.com.talisma.imoveis.dados;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Página de resultados no formato do Spring (D28): {@code content}, {@code number}, {@code size},
 * {@code totalPages}. Página começa em 0; tamanho padrão {@link #TAMANHO_PADRAO}.
 */
public class Pagina<T> {

    public static final int TAMANHO_PADRAO = 10;

    public List<T> itens = new ArrayList<>();
    public int pagina;
    public int tamanho = TAMANHO_PADRAO;
    public int totalPaginas;

    /** Converte um item do {@code content}. */
    public interface Conversor<T> {
        T deJson(JSONObject o) throws JSONException;
    }

    /** Converte um item para o {@code content}. */
    public interface Serializador<T> {
        JSONObject paraJson(T item);
    }

    public boolean temProxima() {
        return pagina + 1 < totalPaginas;
    }

    public static <T> Pagina<T> deJson(JSONObject o, Conversor<T> conversor) throws JSONException {
        Pagina<T> p = new Pagina<>();
        JSONArray content = o.optJSONArray("content");
        if (content != null) {
            for (int i = 0; i < content.length(); i++) {
                p.itens.add(conversor.deJson(content.getJSONObject(i)));
            }
        }
        p.pagina = o.optInt("number", 0);
        p.tamanho = o.optInt("size", TAMANHO_PADRAO);
        p.totalPaginas = o.optInt("totalPages", 0);
        return p;
    }

    public JSONObject paraJson(Serializador<T> serializador) {
        JSONObject o = new JSONObject();
        try {
            JSONArray content = new JSONArray();
            for (T item : itens) {
                content.put(serializador.paraJson(item));
            }
            o.put("content", content);
            o.put("number", pagina);
            o.put("size", tamanho);
            o.put("totalPages", totalPaginas);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }
}
