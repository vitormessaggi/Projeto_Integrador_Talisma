package br.com.talisma.imoveis;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Galeria (6.5, 2.2.2): fundo preto, foto em tela cheia com deslize horizontal (PagerSnapHelper),
 * contador "2/4" e botão fechar. Extras {@code imovel_id}, {@code posicao} e, opcionalmente,
 * {@code fotos} (evita nova consulta); sem {@code fotos}, busca o detalhe no repositório.
 */
public class GaleriaActivity extends AppCompatActivity {

    public static final String EXTRA_POSICAO = "posicao";
    public static final String EXTRA_FOTOS = "fotos";
    private static final String ESTADO_POSICAO = "posicaoAtual";

    private FotoAdapter adapter;
    private LinearLayoutManager layoutManager;
    private TextView txtContador;
    private int posicaoInicial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_galeria);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtContador = findViewById(R.id.txtContador);
        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());

        RecyclerView rvFotos = findViewById(R.id.rvFotos);
        layoutManager = (LinearLayoutManager) rvFotos.getLayoutManager();
        adapter = new FotoAdapter(FotoAdapter.Modo.GALERIA, null);
        rvFotos.setAdapter(adapter);
        new PagerSnapHelper().attachToRecyclerView(rvFotos);
        rvFotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    atualizarContador();
                }
            }
        });

        posicaoInicial = savedInstanceState != null
                ? savedInstanceState.getInt(ESTADO_POSICAO, 0)
                : getIntent().getIntExtra(EXTRA_POSICAO, 0);

        List<String> fotos = getIntent().getStringArrayListExtra(EXTRA_FOTOS);
        if (fotos != null) {
            mostrar(fotos);
        } else {
            new ImovelRepository(this).detalhe(getIntent().getLongExtra(DetalheImovelActivity.EXTRA_IMOVEL_ID, 0),
                    new Tarefas.Callback<Imovel>() {
                        @Override
                        public void sucesso(Imovel imovel) {
                            mostrar(imovel.fotos);
                        }

                        @Override
                        public void erro(ApiException e) {
                            mostrar(new ArrayList<>());
                        }
                    });
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_POSICAO, posicaoAtual());
    }

    private void mostrar(List<String> fotos) {
        adapter.substituir(fotos);
        if (posicaoInicial > 0 && posicaoInicial < fotos.size()) {
            layoutManager.scrollToPosition(posicaoInicial);
        }
        atualizarContador();
    }

    private int posicaoAtual() {
        int posicao = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (posicao == RecyclerView.NO_POSITION) {
            posicao = layoutManager.findFirstVisibleItemPosition();
        }
        return posicao == RecyclerView.NO_POSITION ? posicaoInicial : posicao;
    }

    private void atualizarContador() {
        int total = adapter.getItemCount();
        txtContador.setVisibility(total > 0 ? View.VISIBLE : View.INVISIBLE);
        if (total > 0) {
            txtContador.setText(getString(R.string.galeria_contador, posicaoAtual() + 1, total));
        }
    }
}
