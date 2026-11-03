package br.com.talisma.imoveis.dados;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

/** Foto nova do corretor aguardando/enviando para a API (6.8, 2.5.2). */
public class FotoEnvio {

    public static final String AGUARDANDO = "AGUARDANDO";
    public static final String ENVIANDO = "ENVIANDO";
    public static final String ENVIADA = "ENVIADA";
    public static final String FALHOU = "FALHOU";

    public Uri uri;
    /** {@link #AGUARDANDO} | {@link #ENVIANDO} | {@link #ENVIADA} | {@link #FALHOU} */
    public String estado = AGUARDANDO;
    /** 0–100 */
    public int progresso;

    public FotoEnvio(Uri uri) {
        this.uri = uri;
    }

    /** Usado para guardar as fotos pendentes em {@code onSaveInstanceState} (D20). */
    public static FotoEnvio deJson(JSONObject o) throws JSONException {
        FotoEnvio f = new FotoEnvio(Uri.parse(o.getString("uri")));
        f.estado = o.optString("estado", AGUARDANDO);
        f.progresso = o.optInt("progresso", 0);
        return f;
    }

    public JSONObject paraJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("uri", uri.toString());
            o.put("estado", estado);
            o.put("progresso", progresso);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }
}
