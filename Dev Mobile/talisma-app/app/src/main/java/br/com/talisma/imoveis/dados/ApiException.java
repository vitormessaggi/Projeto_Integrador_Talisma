package br.com.talisma.imoveis.dados;

import android.content.Context;

import androidx.annotation.Nullable;

import br.com.talisma.imoveis.R;

/**
 * Erro de uma operação de dados (API real ou {@link ServidorSimulado}): status HTTP + mensagem.
 * Status {@link #SEM_CONEXAO} (0) = falha de rede, sem resposta do servidor.
 * <p>
 * Ponto para a UI (6.6, D35): {@link #isSessaoExpirada()} = 401 numa chamada autenticada; a sessão
 * já foi limpa e a tela deve abrir o Login com retorno e a mensagem "Sua sessão expirou"
 * ({@link #mensagemParaUsuario}). No login, 401 é só credencial inválida (não expira nada).
 */
public class ApiException extends Exception {

    public static final int SEM_CONEXAO = 0;
    /** Não autorizado: credencial inválida (login) ou sessão expirada (chamada autenticada). */
    public static final int NAO_AUTORIZADO = 401;
    /** Login com e-mail ainda não verificado → VerificarEmail (modo CADASTRO). */
    public static final int EMAIL_NAO_VERIFICADO = 403;
    /** E-mail já cadastrado. */
    public static final int CONFLITO = 409;
    /** Conta bloqueada após 3 tentativas (RN-002). */
    public static final int CONTA_BLOQUEADA = 423;
    /** Erro do próprio app (sem HTTP): conta com perfil diferente do modo do Login (6.5). */
    public static final int PERFIL_INCOMPATIVEL = -1;

    private final int status;
    @Nullable
    private final String mensagemServidor;
    private boolean sessaoExpirada;

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

    /** 401 de uma chamada autenticada: quem cria já limpou a {@link Sessao}. */
    public static ApiException sessaoExpirada(@Nullable String mensagemServidor) {
        ApiException e = new ApiException(NAO_AUTORIZADO, mensagemServidor);
        e.sessaoExpirada = true;
        return e;
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

    /** Sessão expirada (401 autenticado): a tela abre o Login com "Sua sessão expirou". */
    public boolean isSessaoExpirada() {
        return sessaoExpirada;
    }

    /** Login de conta bloqueada (RN-002): a tela mostra o diálogo de bloqueio. */
    public boolean isContaBloqueada() {
        return status == CONTA_BLOQUEADA;
    }

    /** Login com e-mail não verificado: a tela abre VerificarEmail (modo CADASTRO). */
    public boolean isEmailNaoVerificado() {
        return status == EMAIL_NAO_VERIFICADO;
    }

    /**
     * Mensagem para mostrar ao usuário (6.6): rede → "Sem conexão…"; 4xx → mensagem da API;
     * 5xx ou 4xx sem mensagem → "Algo deu errado…"; sessão expirada → "Sua sessão expirou…".
     */
    public String mensagemParaUsuario(Context context) {
        if (isSemConexao()) {
            return context.getString(R.string.erro_sem_conexao);
        }
        if (sessaoExpirada) {
            return context.getString(R.string.sessao_expirada);
        }
        boolean erroDoCliente = (status >= 400 && status < 500) || status == PERFIL_INCOMPATIVEL;
        if (erroDoCliente && mensagemServidor != null && !mensagemServidor.isEmpty()) {
            return mensagemServidor;
        }
        return context.getString(R.string.erro_servidor);
    }
}
