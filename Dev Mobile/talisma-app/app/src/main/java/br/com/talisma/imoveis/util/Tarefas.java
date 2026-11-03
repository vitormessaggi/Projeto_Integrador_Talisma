package br.com.talisma.imoveis.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.talisma.imoveis.dados.ApiException;

/**
 * Execução em segundo plano (D8): {@link ExecutorService} para o trabalho e {@link Handler} da
 * main thread para entregar o resultado à tela.
 */
public final class Tarefas {

    private static final String TAG = "Tarefas";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    /** Callback único dos Repositories; os dois métodos rodam na main thread. */
    public interface Callback<T> {
        void sucesso(T dado);

        void erro(ApiException e);
    }

    /** Trabalho executado fora da main thread (rede, banco, simulado). */
    public interface Trabalho<T> {
        T executar() throws ApiException;
    }

    private Tarefas() {
    }

    /** Roda {@code trabalho} em segundo plano e entrega o resultado em {@code callback} na main thread. */
    public static <T> void executar(Trabalho<T> trabalho, Callback<T> callback) {
        EXECUTOR.execute(() -> {
            try {
                T dado = trabalho.executar();
                PRINCIPAL.post(() -> callback.sucesso(dado));
            } catch (ApiException e) {
                PRINCIPAL.post(() -> callback.erro(e));
            } catch (RuntimeException e) {
                Log.e(TAG, "Falha inesperada em tarefa", e);
                ApiException erro = new ApiException(500, null, e);
                PRINCIPAL.post(() -> callback.erro(erro));
            }
        });
    }

    /** Roda {@code acao} em segundo plano, sem resultado. */
    public static void emSegundoPlano(Runnable acao) {
        EXECUTOR.execute(acao);
    }

    /** Roda {@code acao} na main thread. */
    public static void naPrincipal(Runnable acao) {
        PRINCIPAL.post(acao);
    }
}
