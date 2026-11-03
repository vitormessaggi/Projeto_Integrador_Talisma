package br.com.talisma.imoveis.dados;

import android.content.Context;

import androidx.annotation.Nullable;

import br.com.talisma.imoveis.R;

/**
 * Erro de uma operação de dados (API real ou {@link ServidorSimulado}): status HTTP + mensagem.
 * Status {@link #SEM_CONEXAO} (0) = falha de rede, sem resposta do servidor.
 */
public class ApiException extends Exception {

    public static final int SEM_CONEXAO = 0;

    private final int status;
    @Nullable
    private final String mensagemServidor;

    public ApiException(int status, @Nullable String mensagemServidor) {
        super("HTTP " + status + (mensagemServidor != null ? ": " + mensagemServidor : ""));
        this.status = status;
        this.mensagemServidor = mensagemServidor;
    }

    public ApiException(int status, @Nullable String mensagemServidor, Throwable causa) {
        this(status, mensagemServidor);
        initCause(causa);
    }

    public static ApiException semConexao(Throwable causa) {
        return new ApiException(SEM_CONEXAO, null, causa);
    }

    public int getStatus() {
        return status;
    }

    @Nullable
    public String getMensagemServidor() {
        return mensagemServidor;
    }

    public boolean isSemConexao() {
        return status == SEM_CONEXAO;
    }

    /**
     * Mensagem para mostrar ao usuário (6.6): rede → "Sem conexão…"; 4xx → mensagem da API;
     * 5xx ou 4xx sem mensagem → "Algo deu errado…".
     */
    public String mensagemParaUsuario(Context context) {
        if (isSemConexao()) {
            return context.getString(R.string.erro_sem_conexao);
        }
        if (status >= 400 && status < 500 && mensagemServidor != null && !mensagemServidor.isEmpty()) {
            return mensagemServidor;
        }
        return context.getString(R.string.erro_servidor);
    }
}
