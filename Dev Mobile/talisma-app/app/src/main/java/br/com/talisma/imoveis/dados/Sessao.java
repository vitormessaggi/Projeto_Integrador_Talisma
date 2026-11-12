package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sessão do usuário logado (SharedPreferences "sessao": {@code token}, {@code usuario_json}).
 * Dura até tocar Sair ou receber 401 (D35). O CPF nunca é gravado aqui (D12): quem precisa dele
 * busca o perfil no servidor.
 */
public final class Sessao {

    private static final String TAG = "Sessao";
    private static final String ARQUIVO = "sessao";
    private static final String CHAVE_TOKEN = "token";
    private static final String CHAVE_USUARIO = "usuario_json";

    private Sessao() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static void salvar(Context context, String token, Usuario usuario) {
        JSONObject json = usuario.paraJson();
        json.remove("cpf");
        prefs(context).edit()
                .putString(CHAVE_TOKEN, Cofre.cifrar(token))
                .remove("refresh")
                .putString(CHAVE_USUARIO, json.toString())
                .apply();
    }

    /** Atualiza só os dados do usuário (ex.: após editar o perfil), mantendo o token. */
    public static void atualizarUsuario(Context context, Usuario usuario) {
        JSONObject json = usuario.paraJson();
        json.remove("cpf");
        prefs(context).edit().putString(CHAVE_USUARIO, json.toString()).apply();
    }

    @Nullable
    public static String token(Context context) {
        return Cofre.ler(prefs(context).getString(CHAVE_TOKEN, null));
    }

    @Nullable
    public static Usuario usuario(Context context) {
        String json = prefs(context).getString(CHAVE_USUARIO, null);
        if (json == null) {
            return null;
        }
        try {
            return Usuario.deJson(new JSONObject(json));
        } catch (JSONException e) {
            Log.w(TAG, "Usuário da sessão ilegível; limpando sessão");
            limpar(context);
            return null;
        }
    }

    public static boolean logado(Context context) {
        return token(context) != null;
    }

    public static String refresh(Context context) { return Cofre.ler(prefs(context).getString("refresh", null)); }

    public static void tokens(Context context, String acesso, String refresh) {
        prefs(context).edit().putString(CHAVE_TOKEN, Cofre.cifrar(acesso)).putString("refresh", Cofre.cifrar(refresh)).commit();
    }

    public static void limpar(Context context) {
        prefs(context).edit().clear().apply();
    }
}
