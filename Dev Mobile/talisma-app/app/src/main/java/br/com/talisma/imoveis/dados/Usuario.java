package br.com.talisma.imoveis.dados;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Usuário do app (6.8). O CPF nunca vai para log nem cache local (D12). */
public class Usuario {

    public static final String CLIENTE = "CLIENTE";
    public static final String CORRETOR = "CORRETOR";

    public long id;
    public String nome;
    public String email;
    @Nullable
    public String cpf;
    @Nullable
    public String telefone;
    /** {@link #CLIENTE} | {@link #CORRETOR} */
    public String perfil;

    public boolean isCorretor() {
        return CORRETOR.equals(perfil);
    }

    public static Usuario deJson(JSONObject o) throws JSONException {
        Usuario u = new Usuario();
        u.id = o.getLong("id");
        u.nome = texto(o, "nome");
        u.email = texto(o, "email");
        u.cpf = texto(o, "cpf");
        u.telefone = texto(o, "telefone");
        u.perfil = texto(o, "perfil");
        return u;
    }

    public JSONObject paraJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("nome", nome);
            o.put("email", email);
            o.put("cpf", cpf != null ? cpf : JSONObject.NULL);
            o.put("telefone", telefone != null ? telefone : JSONObject.NULL);
            o.put("perfil", perfil);
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
