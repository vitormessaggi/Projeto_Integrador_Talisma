package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Cliente HTTP nativo (D8): {@link HttpURLConnection} + org.json, timeouts de 15 s e
 * {@code Authorization: Bearer <token>} quando há sessão. Chamar sempre fora da main thread
 * (via {@code Tarefas}). Rotas em {@link ApiRotas}; resposta crua em texto (vazia em 204),
 * convertida com {@link #objeto}/{@link #lista} e o {@code deJson} do modelo.
 * Erros: rede → {@link ApiException#semConexao}; status ≥ 400 → {@link ApiException} com a
 * {@code mensagem} do corpo padrão {@code { "status", "mensagem" }}; 401 com token enviado limpa a
 * {@link Sessao} e vira {@link ApiException#sessaoExpirada}.
 * <p>
 * Upload de foto (2.5.2): {@link #postMultipart} envia um arquivo em {@code multipart/form-data}
 * com tamanho fixo (streaming sem buffer) e informa o progresso do envio em {@link Progresso}.
 */
public class Api {

    private static final String TAG = "Api";
    private static final int TIMEOUT_MS = 15_000;
    private static final String FIM_LINHA = "\r\n";

    /** Progresso de um envio, de 0 a 100. Chamado na thread do envio (não na main thread). */
    public interface Progresso {
        void atualizar(int porcentagem);
    }

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

    /**
     * {@code POST} {@code multipart/form-data} com um único arquivo no campo {@code campo}
     * (ex.: {@code arquivo}, JPEG — 6.9). {@code progresso} recebe 0–100 conforme os bytes saem.
     */
    public String postMultipart(String rota, String campo, File arquivo, String tipoConteudo,
                                @Nullable Progresso progresso) throws ApiException {
        String limite = "----Talisma" + UUID.randomUUID().toString().replace("-", "");
        byte[] inicio = ("--" + limite + FIM_LINHA
                + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + arquivo.getName() + "\""
                + FIM_LINHA + "Content-Type: " + tipoConteudo + FIM_LINHA + FIM_LINHA)
                .getBytes(StandardCharsets.UTF_8);
        byte[] fim = (FIM_LINHA + "--" + limite + "--" + FIM_LINHA).getBytes(StandardCharsets.UTF_8);
        long tamanhoArquivo = arquivo.length();
        long total = inicio.length + tamanhoArquivo + fim.length;

        HttpURLConnection conexao = null;
        String token = Sessao.token(context);
        try {
            conexao = abrir("POST", rota, token);
            conexao.setDoOutput(true);
            conexao.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + limite);
            conexao.setFixedLengthStreamingMode(total);
            avisar(progresso, 0);
            try (OutputStream saida = conexao.getOutputStream();
                 InputStream entrada = new FileInputStream(arquivo)) {
                saida.write(inicio);
                byte[] buffer = new byte[8192];
                long enviados = 0;
                int ultimo = 0;
                int lidos;
                while ((lidos = entrada.read(buffer)) != -1) {
                    saida.write(buffer, 0, lidos);
                    enviados += lidos;
                    // Reserva os últimos 1% para a resposta do servidor.
                    int atual = (int) (enviados * 99 / Math.max(1, tamanhoArquivo));
                    if (atual != ultimo) {
                        ultimo = atual;
                        avisar(progresso, atual);
                    }
                }
                saida.write(fim);
            }
            String resposta = responder(conexao, "POST", rota, token);
            avisar(progresso, 100);
            return resposta;
        } catch (IOException e) {
            Log.w(TAG, "POST " + rota + " → sem conexão: " + e.getClass().getSimpleName());
            throw ApiException.semConexao(e);
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
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
        String token = Sessao.token(context);
        try {
            conexao = abrir(metodo, rota, token);
            if (corpo != null) {
                byte[] bytes = corpo.toString().getBytes(StandardCharsets.UTF_8);
                conexao.setDoOutput(true);
                conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexao.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream saida = conexao.getOutputStream()) {
                    saida.write(bytes);
                }
            }
            return responder(conexao, metodo, rota, token);
        } catch (IOException e) {
            Log.w(TAG, metodo + " " + rota + " → sem conexão: " + e.getClass().getSimpleName());
            throw ApiException.semConexao(e);
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    /** Abre a conexão com timeouts, {@code Accept} JSON e, se houver sessão, o token Bearer. */
    private static HttpURLConnection abrir(String metodo, String rota, @Nullable String token) throws IOException {
        HttpURLConnection conexao = (HttpURLConnection) new URL(ApiRotas.BASE + rota).openConnection();
        conexao.setConnectTimeout(TIMEOUT_MS);
        conexao.setReadTimeout(TIMEOUT_MS);
        conexao.setRequestProperty("Accept", "application/json");
        if (token != null) {
            conexao.setRequestProperty("Authorization", "Bearer " + token);
        }
        // O HttpURLConnection do Android (OkHttp interno) aceita PATCH.
        conexao.setRequestMethod(metodo);
        return conexao;
    }

    /** Lê a resposta; status ≥ 400 vira {@link ApiException} (401 com token = sessão expirada). */
    private String responder(HttpURLConnection conexao, String metodo, String rota, @Nullable String token)
            throws IOException, ApiException {
        int status = conexao.getResponseCode();
        if (status >= 400) {
            String erro = ler(conexao.getErrorStream());
            Log.w(TAG, metodo + " " + rota + " → " + status);
            if (status == ApiException.NAO_AUTORIZADO && token != null) {
                // 401 com token = sessão expirada (6.6, D35): limpa e avisa a tela.
                Sessao.limpar(context);
                throw ApiException.sessaoExpirada(mensagemDoErro(erro));
            }
            throw new ApiException(status, mensagemDoErro(erro));
        }
        return ler(conexao.getInputStream());
    }

    private static void avisar(@Nullable Progresso progresso, int porcentagem) {
        if (progresso != null) {
            progresso.atualizar(porcentagem);
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
