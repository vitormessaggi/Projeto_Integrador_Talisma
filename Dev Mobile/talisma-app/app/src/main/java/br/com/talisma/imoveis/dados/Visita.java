package br.com.talisma.imoveis.dados;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Solicitação de visita (6.8, D30, D37). */
public class Visita {

    public static final String MANHA = "MANHA";
    public static final String TARDE = "TARDE";
    public static final String NOITE = "NOITE";

    public static final String PENDENTE = "PENDENTE";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CONCLUIDA = "CONCLUIDA";
    public static final String CANCELADA = "CANCELADA";

    public long id;
    public long imovelId;
    public String imovelTitulo;
    @Nullable
    public String imovelFoto;
    /** yyyy-MM-dd */
    public String data;
    /** {@link #MANHA} | {@link #TARDE} | {@link #NOITE} */
    public String periodo;
    public String telefone;
    @Nullable
    public String mensagem;
    /** {@link #PENDENTE} | {@link #CONFIRMADA} | {@link #CONCLUIDA} | {@link #CANCELADA} */
    public String status;
    public String criadaEm;

    /** Cancelar só enquanto Pendente ou Confirmada (D37). */
    public boolean podeCancelar() {
        return PENDENTE.equals(status) || CONFIRMADA.equals(status);
    }

    public static Visita deJson(JSONObject o) throws JSONException {
        Visita v = new Visita();
        v.id = o.getLong("id");
        v.imovelId = o.getLong("imovelId");
        v.imovelTitulo = texto(o, "imovelTitulo");
        v.imovelFoto = texto(o, "imovelFoto");
        v.data = texto(o, "data");
        v.periodo = texto(o, "periodo");
        v.telefone = texto(o, "telefone");
        v.mensagem = texto(o, "mensagem");
        v.status = texto(o, "status");
        v.criadaEm = texto(o, "criadaEm");
        return v;
    }

    public JSONObject paraJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("imovelId", imovelId);
            o.put("imovelTitulo", imovelTitulo);
            o.put("imovelFoto", imovelFoto != null ? imovelFoto : JSONObject.NULL);
            o.put("data", data);
            o.put("periodo", periodo);
            o.put("telefone", telefone);
            o.put("mensagem", mensagem != null ? mensagem : JSONObject.NULL);
            o.put("status", status);
            o.put("criadaEm", criadaEm);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    @Nullable
    private static String texto(JSONObject o, String chave) {
        return o.isNull(chave) ? null : o.optString(chave, null);
    }
}
