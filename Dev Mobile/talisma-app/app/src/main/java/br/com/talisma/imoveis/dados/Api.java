package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP nativo (D8): {@link HttpURLConnection} + org.json, timeouts de 15 s e
 * {@code Authorization: Bearer <token>} quando há sessão. Chamar sempre fora da main thread
 * (via {@code Tarefas}). Rotas em {@link ApiRotas}; resposta crua em texto (vazia em 204),
 * convertida com {@link #objeto}/{@link #lista} e o {@code deJson} do modelo.
 * Erros: rede → {@link ApiException#semConexao}; status ≥ 400 → {@link ApiException} com a
 * {@code mensagem} do corpo padrão {@code { "status", "mensagem" }}.
 */
public class Api {

    private static final String TAG = "Api";
    private static final int TIMEOUT_MS = 15_000;

    private final Context context;

    public Api(Context context) {
        this.context = context.getApplicationContext();
    }

    public String get(String rota) throws ApiException {
        return requisitar("GET", rota, null);
    }

    public String post(String rota, @Nullable JSONObject corpo) throws ApiException {
        return requisitar("POST", rota, corpo);
    }

    public String put(String rota, @Nullable JSONObject corpo) throws ApiException {
        return requisitar("PUT", rota, corpo);
    }

    public String patch(String rota, @Nullable JSONObject corpo) throws ApiException {
        return requisitar("PATCH", rota, corpo);
    }

    public String delete(String rota) throws ApiException {
        return requisitar("DELETE", rota, null);
    }

    /** Converte a resposta em objeto JSON (erro de formato vira ApiException 500). */
    public static JSONObject objeto(String resposta) throws ApiException {
        try {
            return new JSONObject(resposta);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    /** Converte a resposta em lista JSON (erro de formato vira ApiException 500). */
    public static JSONArray lista(String resposta) throws ApiException {
        try {
            return new JSONArray(resposta);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }

    private String requisitar(String metodo, String rota, @Nullable JSONObject corpo) throws ApiException {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(ApiRotas.BASE + rota).openConnection();
            conexao.setConnectTimeout(TIMEOUT_MS);
            conexao.setReadTimeout(TIMEOUT_MS);
            conexao.setRequestProperty("Accept", "application/json");
            String token = Sessao.token(context);
            if (token != null) {
                conexao.setRequestProperty("Authorization", "Bearer " + token);
            }
            // O HttpURLConnection do Android (OkHttp interno) aceita PATCH.
            conexao.setRequestMethod(metodo);
            if (corpo != null) {
                byte[] bytes = corpo.toString().getBytes(StandardCharsets.UTF_8);
                conexao.setDoOutput(true);
                conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexao.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream saida = conexao.getOutputStream()) {
                    saida.write(bytes);
                }
            }

            int status = conexao.getResponseCode();
            if (status >= 400) {
                String erro = ler(conexao.getErrorStream());
                Log.w(TAG, metodo + " " + rota + " → " + status);
                throw new ApiException(status, mensagemDoErro(erro));
            }
            return ler(conexao.getInputStream());
        } catch (IOException e) {
            Log.w(TAG, metodo + " " + rota + " → sem conexão: " + e.getClass().getSimpleName());
            throw ApiException.semConexao(e);
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    private static String ler(@Nullable InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
        try (InputStream in = entrada; ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                saida.write(buffer, 0, lidos);
            }
            return saida.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Nullable
    private static String mensagemDoErro(String corpo) {
        if (corpo == null || corpo.isEmpty()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(corpo);
            return o.isNull("mensagem") ? null : o.optString("mensagem", null);
        } catch (JSONException e) {
            return null;
        }
    }
}
