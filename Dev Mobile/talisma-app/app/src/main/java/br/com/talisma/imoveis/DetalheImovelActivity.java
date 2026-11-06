package br.com.talisma.imoveis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.FavoritoRepository;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.util.CarregadorImagem;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Detalhe do imóvel (6.5, 2.2.2): fotos com galeria, título, preço, características, descrição com
 * Ver mais/Ver menos, endereço (mapa osmdroid entra na Fase 5), compartilhar (D38), coração (2.3.1)
 * e CTA "Comprar agora" | "Alugar agora" → Solicitar visita (D4). Extra {@code imovel_id}.
 */
public class DetalheImovelActivity extends AppCompatActivity {

    public static final String EXTRA_IMOVEL_ID = "imovel_id";
    private static final int LINHAS_DESCRICAO = 4;
    private static final String ESTADO_EXPANDIDA = "descricaoExpandida";

    private ImovelRepository imovelRepository;
    private FavoritoRepository favoritoRepository;
    private long imovelId;
    private Imovel imovel;
    private boolean favorito;
    private boolean descricaoExpandida;

    private View estado;
    private View banner;
    private View conteudo;
    private ImageView imgPrincipal;
    private FotoAdapter miniaturas;
    private ImageButton btnFavorito;
    private TextView txtDescricao;
    private TextView txtVerMais;

    public static Intent intent(Context context, long imovelId) {
        return new Intent(context, DetalheImovelActivity.class).putExtra(EXTRA_IMOVEL_ID, imovelId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalhe_imovel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imovelRepository = new ImovelRepository(this);
        favoritoRepository = new FavoritoRepository(this);
        imovelId = getIntent().getLongExtra(EXTRA_IMOVEL_ID, 0);
        descricaoExpandida = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_EXPANDIDA, false);

        estado = findViewById(R.id.estadoTela);
        banner = findViewById(R.id.txtBannerOffline);
        conteudo = findViewById(R.id.scrollConteudo);
        imgPrincipal = findViewById(R.id.imgPrincipal);
        btnFavorito = findViewById(R.id.btnFavorito);
        txtDescricao = findViewById(R.id.txtDescricao);
        txtVerMais = findViewById(R.id.txtVerMais);

        RecyclerView rvMiniaturas = findViewById(R.id.rvMiniaturas);
        miniaturas = new FotoAdapter(FotoAdapter.Modo.MINIATURA, this::abrirGaleria);
        rvMiniaturas.setAdapter(miniaturas);
        imgPrincipal.setOnClickListener(v -> abrirGaleria(0));

        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnCompartilhar).setOnClickListener(v -> compartilhar());
        btnFavorito.setOnClickListener(v -> alternarFavorito());
        txtVerMais.setOnClickListener(v -> {
            descricaoExpandida = !descricaoExpandida;
            atualizarDescricao();
        });
        findViewById(R.id.btnInteresse).setOnClickListener(v -> startActivity(
                new Intent(this, SolicitarVisitaActivity.class).putExtra(EXTRA_IMOVEL_ID, imovelId)));
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        carregar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_EXPANDIDA, descricaoExpandida);
    }

    private void carregar() {
        conteudo.setVisibility(View.INVISIBLE);
        EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        imovelRepository.detalhe(imovelId, new Tarefas.Callback<Imovel>() {
            @Override
            public void sucesso(Imovel dado) {
                imovel = dado;
                mostrar();
            }

            @Override
            public void erro(ApiException e) {
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(DetalheImovelActivity.this));
            }
        });
        favoritoRepository.isFavorito(imovelId, new Tarefas.Callback<Boolean>() {
            @Override
            public void sucesso(Boolean dado) {
                favorito = dado;
                atualizarFavorito();
            }

            @Override
            public void erro(ApiException e) {
            }
        });
    }

    private void mostrar() {
        banner.setVisibility(imovel.offline ? View.VISIBLE : View.GONE);
        CarregadorImagem.carregar(imgPrincipal, imovel.capa());
        miniaturas.substituir(imovel.fotos);
        ((TextView) findViewById(R.id.txtTitulo)).setText(imovel.titulo);
        ((TextView) findViewById(R.id.txtPreco)).setText(Formatos.preco(this, imovel));
        ((TextView) findViewById(R.id.txtLocalizacao)).setText(ImovelAdapter.localizacao(this, imovel));
        ((TextView) findViewById(R.id.txtQuartos)).setText(getString(R.string.detalhe_quartos, imovel.quartos));
        ((TextView) findViewById(R.id.txtBanheiros)).setText(getString(R.string.detalhe_banheiros, imovel.banheiros));
        ((TextView) findViewById(R.id.txtVagas)).setText(getString(R.string.detalhe_vagas, imovel.vagas));
        ((TextView) findViewById(R.id.txtArea)).setText(imovel.area != null
                ? getString(R.string.detalhe_area, Formatos.area(this, imovel.area))
                : getString(R.string.detalhe_area_vazia));
        txtDescricao.setText(imovel.descricao);
        atualizarDescricao();
        TextView txtEndereco = findViewById(R.id.txtEndereco);
        String endereco = imovel.endereco != null && !imovel.endereco.isEmpty()
                ? imovel.endereco : ImovelAdapter.localizacao(this, imovel);
        txtEndereco.setText(endereco);
        Button btnInteresse = findViewById(R.id.btnInteresse);
        btnInteresse.setText(imovel.isLocacao() ? R.string.detalhe_alugar_agora : R.string.detalhe_comprar_agora);
        EstadoTela.esconder(estado);
        conteudo.setVisibility(View.VISIBLE);
    }

    /** 4 linhas com "Ver mais"; o link some quando a descrição cabe inteira. */
    private void atualizarDescricao() {
        txtDescricao.setMaxLines(descricaoExpandida ? Integer.MAX_VALUE : LINHAS_DESCRICAO);
        txtVerMais.setText(descricaoExpandida ? R.string.detalhe_ver_menos : R.string.detalhe_ver_mais);
        txtDescricao.post(() -> {
            boolean cabe = !descricaoExpandida && txtDescricao.getLayout() != null
                    && txtDescricao.getLayout().getLineCount() <= LINHAS_DESCRICAO
                    && txtDescricao.getLayout().getEllipsisCount(txtDescricao.getLayout().getLineCount() - 1) == 0;
            txtVerMais.setVisibility(cabe ? View.GONE : View.VISIBLE);
        });
    }

    private void atualizarFavorito() {
        btnFavorito.setImageResource(favorito ? R.drawable.ic_favorito_cheio : R.drawable.ic_favorito);
        btnFavorito.setContentDescription(getString(favorito ? R.string.desc_remover_favorito : R.string.desc_favoritar));
    }

    private void alternarFavorito() {
        if (imovel == null) {
            return;
        }
        favoritoRepository.alternar(imovel, new Tarefas.Callback<Boolean>() {
            @Override
            public void sucesso(Boolean dado) {
                favorito = dado;
                atualizarFavorito();
                Snackbar.make(findViewById(R.id.main), favorito ? R.string.favoritos_adicionado : R.string.favoritos_removido,
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.btnInteresse).show();
            }

            @Override
            public void erro(ApiException e) {
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(DetalheImovelActivity.this),
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    /** Compartilhar por Intent implícita ACTION_SEND (D38). */
    private void compartilhar() {
        if (imovel == null) {
            return;
        }
        String texto = getString(R.string.detalhe_compartilhar_texto, imovel.titulo,
                Formatos.preco(this, imovel), ImovelAdapter.localizacao(this, imovel));
        Intent enviar = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, imovel.titulo)
                .putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(enviar, getString(R.string.detalhe_compartilhar_titulo)));
    }

    private void abrirGaleria(int posicao) {
        if (imovel == null || imovel.fotos.isEmpty()) {
            return;
        }
        startActivity(new Intent(this, GaleriaActivity.class)
                .putExtra(EXTRA_IMOVEL_ID, imovelId)
                .putExtra(GaleriaActivity.EXTRA_POSICAO, posicao)
                .putStringArrayListExtra(GaleriaActivity.EXTRA_FOTOS, new ArrayList<>(imovel.fotos)));
    }
}
