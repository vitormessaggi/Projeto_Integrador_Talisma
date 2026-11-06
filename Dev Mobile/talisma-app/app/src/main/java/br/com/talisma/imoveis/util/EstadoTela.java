package br.com.talisma.imoveis.util;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import br.com.talisma.imoveis.R;

/**
 * Estados das telas de dados (6.4, EAP 2.4.3): CARREGANDO, VAZIO, ERRO e OFFLINE sobre o layout
 * {@code res/layout/estado_tela.xml}, incluído na tela com
 * {@code <include android:id="@+id/estadoTela" layout="@layout/estado_tela" />}.
 * <p>
 * Ids obrigatórios no layout (a T02 redesenha o XML mantendo-os):
 * <ul>
 *   <li>{@code estadoTela} — raiz (começa {@code gone});</li>
 *   <li>{@code prgEstado} — {@code ProgressBar} (só em CARREGANDO);</li>
 *   <li>{@code imgEstado} — ícone (VAZIO, ERRO, OFFLINE);</li>
 *   <li>{@code txtEstado} — mensagem;</li>
 *   <li>{@code btnTentarNovamente} — botão "Tentar novamente" (ERRO e OFFLINE).</li>
 * </ul>
 * Uso: {@code EstadoTela.mostrar(estado, EstadoTela.Tipo.ERRO, e.mensagemParaUsuario(this));}
 * {@code EstadoTela.aoTentarNovamente(estado, this::carregar);} e {@code EstadoTela.esconder(estado)}
 * quando os dados chegarem.
 */
public final class EstadoTela {

    public enum Tipo { CARREGANDO, VAZIO, ERRO, OFFLINE }

    private EstadoTela() {
    }

    /**
     * Mostra o estado {@code tipo} em {@code view} (a raiz {@code estadoTela}). Sem {@code mensagem},
     * usa o padrão da 6.6: CARREGANDO sem texto, ERRO "Algo deu errado…", OFFLINE "Sem conexão…".
     */
    public static void mostrar(View view, Tipo tipo, @Nullable String mensagem) {
        View progresso = view.findViewById(R.id.prgEstado);
        View icone = view.findViewById(R.id.imgEstado);
        TextView texto = view.findViewById(R.id.txtEstado);
        View tentarNovamente = view.findViewById(R.id.btnTentarNovamente);

        String textoFinal = mensagem;
        if (textoFinal == null) {
            if (tipo == Tipo.ERRO) {
                textoFinal = view.getContext().getString(R.string.erro_servidor);
            } else if (tipo == Tipo.OFFLINE) {
                textoFinal = view.getContext().getString(R.string.erro_sem_conexao);
            }
        }

        progresso.setVisibility(tipo == Tipo.CARREGANDO ? View.VISIBLE : View.GONE);
        icone.setVisibility(tipo == Tipo.CARREGANDO ? View.GONE : View.VISIBLE);
        texto.setText(textoFinal);
        texto.setVisibility(textoFinal == null || textoFinal.isEmpty() ? View.GONE : View.VISIBLE);
        tentarNovamente.setVisibility(tipo == Tipo.ERRO || tipo == Tipo.OFFLINE ? View.VISIBLE : View.GONE);
        if (tipo == Tipo.CARREGANDO) {
            view.setContentDescription(view.getContext().getString(R.string.estado_carregando));
        } else {
            view.setContentDescription(null);
        }
        view.setVisibility(View.VISIBLE);
    }

    /** Esconde o estado (dados carregados com sucesso). */
    public static void esconder(View view) {
        view.setVisibility(View.GONE);
    }

    /** Ação do botão "Tentar novamente". */
    public static void aoTentarNovamente(View view, Runnable acao) {
        view.findViewById(R.id.btnTentarNovamente).setOnClickListener(v -> acao.run());
    }
}
