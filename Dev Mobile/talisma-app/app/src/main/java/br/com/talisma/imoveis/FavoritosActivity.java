package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.FavoritoRepository;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.util.BarraNavegacao;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Favoritos (6.5, D10, 2.3.1): lista local do SQLite com coração cheio; toque remove com
 * Snackbar "Removido · Desfazer"; vazio com "Explorar imóveis". Funciona sem login e offline.
 */
public class FavoritosActivity extends AppCompatActivity implements ImovelAdapter.Ouvinte {

    private FavoritoRepository favoritoRepository;
    private ImovelAdapter adapter;
    private View estado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favoritos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BarraNavegacao.configurar(this, R.id.nav_favoritos);

        favoritoRepository = new FavoritoRepository(this);
        estado = findViewById(R.id.estadoTela);
        RecyclerView rvFavoritos = findViewById(R.id.rvFavoritos);
        rvFavoritos.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.colunas_lista)));
        adapter = new ImovelAdapter(ImovelAdapter.Modo.LISTA, this);
        rvFavoritos.setAdapter(adapter);
        // O botão do estado vazio vira "Explorar imóveis" → aba Imóveis
        EstadoTela.aoTentarNovamente(estado, () -> {
            Intent intent = new Intent(this, ImoveisActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        if (adapter.getItemCount() == 0) {
            EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        }
        favoritoRepository.listar(new Tarefas.Callback<List<Imovel>>() {
            @Override
            public void sucesso(List<Imovel> favoritos) {
                Set<Long> ids = new HashSet<>();
                for (Imovel i : favoritos) {
                    ids.add(i.id);
                }
                adapter.setFavoritos(ids);
                adapter.substituir(favoritos);
                if (favoritos.isEmpty()) {
                    mostrarVazio();
                } else {
                    EstadoTela.esconder(estado);
                }
            }

            @Override
            public void erro(ApiException e) {
                EstadoTela.mostrar(estado, EstadoTela.Tipo.ERRO, e.mensagemParaUsuario(FavoritosActivity.this));
                ((TextView) estado.findViewById(R.id.btnTentarNovamente)).setText(R.string.favoritos_explorar);
            }
        });
    }

    /** Ícone coração + "Você ainda não salvou imóveis" + botão "Explorar imóveis" (6.5). */
    private void mostrarVazio() {
        EstadoTela.mostrar(estado, EstadoTela.Tipo.VAZIO, getString(R.string.favoritos_vazio));
        ((ImageView) estado.findViewById(R.id.imgEstado)).setImageResource(R.drawable.ic_favorito);
        TextView btn = estado.findViewById(R.id.btnTentarNovamente);
        btn.setText(R.string.favoritos_explorar);
        btn.setVisibility(View.VISIBLE);
    }

    @Override
    public void aoTocar(Imovel imovel) {
        startActivity(DetalheImovelActivity.intent(this, imovel.id));
    }

    @Override
    public void aoFavoritar(Imovel imovel, int posicao) {
        favoritoRepository.remover(imovel, new Tarefas.Callback<Boolean>() {
            @Override
            public void sucesso(Boolean favorito) {
                adapter.marcarFavorito(imovel.id, false);
                adapter.removerEm(posicao);
                if (adapter.getItemCount() == 0) {
                    mostrarVazio();
                }
                Snackbar.make(findViewById(R.id.main), R.string.favoritos_removido, Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.navInferior)
                        .setAction(R.string.favoritos_desfazer, v -> desfazer(imovel, posicao))
                        .show();
            }

            @Override
            public void erro(ApiException e) {
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(FavoritosActivity.this),
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.navInferior).show();
            }
        });
    }

    private void desfazer(Imovel imovel, int posicao) {
        favoritoRepository.adicionar(imovel, new Tarefas.Callback<Boolean>() {
            @Override
            public void sucesso(Boolean favorito) {
                adapter.marcarFavorito(imovel.id, true);
                adapter.inserirEm(posicao, imovel);
                EstadoTela.esconder(estado);
            }

            @Override
            public void erro(ApiException e) {
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(FavoritosActivity.this),
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.navInferior).show();
            }
        });
    }
}
