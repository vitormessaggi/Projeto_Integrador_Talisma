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
    private static final Object RENOVACAO = new Object();
    private static final int TIMEOUT_MS = 15_000;
    private static final String FIM_LINHA = "\r\n";

    /** Progresso de um envio, de 0 a 100. Chamado na thread do envio (não na main thread). */
    public interface Progresso {
        void atualizar(int porcentagem);
    }

    private final Context context;
    private final Long usuarioEsperado;

    public Api(Context context) {
        this(context, null);
    }

    public Api(Context context, Long usuarioEsperado) {
        this.context = context.getApplicationContext();
        this.usuarioEsperado = usuarioEsperado;
    }

    public String get(String rota) throws ApiException {
        return requisitar("GET", rota, null);
    }

    public String post(String rota, @Nullable JSONObject corpo) throws ApiException {
        return requisitar("POST", rota, corpo);
    }

    public JSONObject perguntarLia(JSONObject pergunta) throws ApiException {
        return objeto(enviar("POST", ApiRotas.CHAT, pergunta, null, 30_000));
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
        String tokenAnterior = Sessao.token(context);
        try { return multipartUmaVez(rota, campo, arquivo, tipoConteudo, progresso); }
        catch (ApiException e) {
            if (e.getStatus() != 401 || tokenAnterior == null) throw e;
            renovar(tokenAnterior);
            try { return multipartUmaVez(rota, campo, arquivo, tipoConteudo, progresso); }
            catch (ApiException repetido) { if (repetido.getStatus() == 401) Sessao.limpar(context); throw repetido; }
        }
    }

    private String multipartUmaVez(String rota, String campo, File arquivo, String tipoConteudo, @Nullable Progresso progresso) throws ApiException {
        String limite = "----Talisma" + UUID.randomUUID().toString().replace("-", "");
        byte[] inicio = ("--" + limite + FIM_LINHA
                + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + arquivo.getName() + "\""
                + FIM_LINHA + "Content-Type: " + tipoConteudo + FIM_LINHA + FIM_LINHA)
                .getBytes(StandardCharsets.UTF_8);
        byte[] fim = (FIM_LINHA + "--" + limite + "--" + FIM_LINHA).getBytes(StandardCharsets.UTF_8);
        long tamanhoArquivo = arquivo.length();
        long total = inicio.length + tamanhoArquivo + fim.length;

        HttpURLConnection conexao = null;
        verificarConta();
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
        String anterior = Sessao.token(context);
        try { return umaVez(metodo, rota, corpo); }
        catch (ApiException e) {
            if (e.getStatus() != 401 || anterior == null || rota.startsWith("/api/v1/auth/")) throw e;
            renovar(anterior);
            try { return umaVez(metodo, rota, corpo); }
            catch (ApiException repetido) { if (repetido.getStatus() == 401) Sessao.limpar(context); throw repetido; }
        }
    }

    private void renovar(String anterior) throws ApiException {
        synchronized (RENOVACAO) {
            String atual = Sessao.token(context);
            if (atual != null && !atual.equals(anterior)) return;
            String refresh = Sessao.refresh(context);
            if (refresh == null) { Sessao.limpar(context); throw ApiException.sessaoExpirada(null); }
            try {
                JSONObject body = new JSONObject(); body.put("refreshToken", refresh);
                JSONObject tokens = objeto(umaVez("POST", "/api/v1/auth/renovar", body));
                // Logout or account switch must not resurrect the old session.
                if (!refresh.equals(Sessao.refresh(context))) throw ApiException.sessaoExpirada(null);
                Sessao.tokens(context, tokens.getString("accessToken"), tokens.getString("refreshToken"));
            } catch (JSONException e) { Sessao.limpar(context); throw ApiException.sessaoExpirada(null); }
            catch (ApiException e) { if ((e.getStatus() == 401 || e.getStatus() == 403) && refresh.equals(Sessao.refresh(context))) Sessao.limpar(context); throw e; }
        }
    }

    public void baixarFoto(String rota, File destino) throws ApiException {
        String anterior = Sessao.token(context);
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            verificarConta(); HttpURLConnection conexao = null;
            try {
                conexao = abrir("GET", rota, Sessao.token(context));
                if (conexao.getResponseCode() == 401 && tentativa == 0 && anterior != null) { renovar(anterior); continue; }
                if (conexao.getResponseCode() >= 400) { responder(conexao, "GET", rota, Sessao.token(context)); return; }
                try (InputStream entrada = conexao.getInputStream(); OutputStream saida = new java.io.FileOutputStream(destino)) {
                    byte[] buffer = new byte[8192]; int lidos;
                    while ((lidos = entrada.read(buffer)) != -1) saida.write(buffer, 0, lidos);
                }
                return;
            } catch (IOException erro) { throw ApiException.semConexao(erro); }
            finally { if (conexao != null) conexao.disconnect(); }
        }
    }

    public JSONArray todasPaginas(String rota) throws ApiException {
        JSONArray itens = new JSONArray();
        int pagina = 0;
        while (true) {
            JSONObject resposta = objeto(get(rota + (rota.contains("?") ? "&" : "?") + "page=" + pagina + "&size=100"));
            try {
                JSONArray lote = resposta.getJSONArray("itens");
                for (int i = 0; i < lote.length(); i++) itens.put(lote.get(i));
                if (++pagina >= resposta.getInt("totalPaginas")) return itens;
            } catch (JSONException e) { throw new ApiException(500, null, e); }
        }
    }

    private void verificarConta() throws ApiException {
        Usuario atual = Sessao.usuario(context);
        if (usuarioEsperado != null && (atual == null || atual.id != usuarioEsperado)) throw ApiException.sessaoExpirada(null);
    }

    private String umaVez(String metodo, String rota, @Nullable JSONObject corpo) throws ApiException {
        verificarConta();
        return enviar(metodo, rota, corpo, Sessao.token(context), TIMEOUT_MS);
    }

    private String enviar(String metodo, String rota, @Nullable JSONObject corpo, @Nullable String token,
                          int timeoutResposta) throws ApiException {
        HttpURLConnection conexao = null;
        try {
            conexao = abrir(metodo, rota, token);
            conexao.setReadTimeout(timeoutResposta);
            if (corpo != null) {
                byte[] bytes = corpo.toString().getBytes(StandardCharsets.UTF_8);
                conexao.setDoOutput(true);
                conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexao.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream saida = conexao.getOutputStream()) {
                    saida.write(bytes);
                }
            }
            if (ApiRotas.CHAT.equals(rota) && conexao.getResponseCode() != 200
                    && conexao.getResponseCode() < 400) {
                throw new ApiException(502, null);
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
        conexao.setInstanceFollowRedirects(!ApiRotas.CHAT.equals(rota));
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
                // Renewal is coordinated by the caller.
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
            return o.optString("detail", o.optString("mensagem", null));
        } catch (JSONException e) {
            return null;
        }
    }
}
