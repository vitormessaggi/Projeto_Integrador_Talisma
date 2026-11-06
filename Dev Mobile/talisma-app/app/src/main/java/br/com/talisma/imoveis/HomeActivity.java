package br.com.talisma.imoveis;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.FavoritoRepository;
import br.com.talisma.imoveis.dados.Filtro;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.dados.Preferencias;
import br.com.talisma.imoveis.util.BarraNavegacao;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Tela inicial (LAUNCHER, 6.5): busca rápida (Comprar | Alugar, tipo, cidade) que abre Imóveis com o
 * filtro, carrossel de destaques com coração (2.3.1) e, na 1ª abertura, o pedido de localização (D15).
 */
public class HomeActivity extends AppCompatActivity implements ImovelAdapter.Ouvinte {

    private static final String[] TIPOS = {null, Imovel.CASA, Imovel.APARTAMENTO, Imovel.TERRENO};

    private ImovelRepository imovelRepository;
    private FavoritoRepository favoritoRepository;
    private ImovelAdapter adapter;
    private View estado;
    private View banner;
    /** Modo (demonstração ou API) dos destaques exibidos; se mudar no Perfil, recarrega ao voltar. */
    private boolean modoCarregado;
    private Spinner spnCidade;
    private MaterialButtonToggleGroup tgFinalidade;
    private Spinner spnTipo;
    private final List<String> cidades = new ArrayList<>();

    private ActivityResultLauncher<String[]> pedidoLocalizacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BarraNavegacao.configurar(this, R.id.nav_inicio);

        imovelRepository = new ImovelRepository(this);
        favoritoRepository = new FavoritoRepository(this);
        estado = findViewById(R.id.estadoTela);
        banner = findViewById(R.id.txtBannerOffline);
        tgFinalidade = findViewById(R.id.tgFinalidade);
        spnTipo = findViewById(R.id.spnTipo);
        spnCidade = findViewById(R.id.spnCidade);

        RecyclerView rvDestaques = findViewById(R.id.rvDestaques);
        adapter = new ImovelAdapter(ImovelAdapter.Modo.DESTAQUE, this);
        rvDestaques.setAdapter(adapter);

        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscar());
        TextView txtVerTodos = findViewById(R.id.txtVerTodos);
        txtVerTodos.setOnClickListener(v -> abrirImoveis(new Filtro()));
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        // Spinner de cidades: "Selecione" + cidades do repositório; a seleção sobrevive à rotação pelo id.
        ArrayAdapter<String> adapterCidades = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cidades);
        adapterCidades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cidades.add(getString(R.string.opcao_selecione));
        spnCidade.setAdapter(adapterCidades);

        pedidoLocalizacao = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::aoResponderLocalizacao);
        if (savedInstanceState == null && !Preferencias.localizacaoPedida(this)) {
            explicarLocalizacao();
        }
        carregar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (modoCarregado != Preferencias.modoDemonstracao(this)) {
            carregar(); // o Modo demonstração foi alternado no Perfil: os destaques vêm da outra fonte (6.7)
        }
        favoritoRepository.ids(new Tarefas.Callback<Set<Long>>() {
            @Override
            public void sucesso(Set<Long> ids) {
                adapter.setFavoritos(ids);
            }

            @Override
            public void erro(ApiException e) {
            }
        });
    }

    // ---------------------------------------------------------------- localização (D15, 2.1.3)

    private void explicarLocalizacao() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.localizacao_titulo)
                .setMessage(R.string.localizacao_explicacao)
                .setPositiveButton(R.string.localizacao_continuar, (d, w) -> pedidoLocalizacao.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}))
                .setNegativeButton(R.string.localizacao_agora_nao, (d, w) -> Preferencias.setLocalizacaoPedida(this, true))
                .setOnCancelListener(d -> Preferencias.setLocalizacaoPedida(this, true))
                .show();
    }

    private void aoResponderLocalizacao(Map<String, Boolean> resultado) {
        Preferencias.setLocalizacaoPedida(this, true);
        // Negar não quebra nada: o chip "Perto de mim" pede de novo quando for usado (D15).
    }

    // ---------------------------------------------------------------- busca

    private void buscar() {
        Filtro filtro = new Filtro();
        filtro.finalidade = tgFinalidade.getCheckedButtonId() == R.id.btnAlugar ? Imovel.LOCACAO : Imovel.VENDA;
        int tipo = spnTipo.getSelectedItemPosition();
        if (tipo > 0 && tipo < TIPOS.length) {
            filtro.tipo = TIPOS[tipo];
        }
        int cidade = spnCidade.getSelectedItemPosition();
        if (cidade > 0 && cidade < cidades.size()) {
            filtro.cidade = cidades.get(cidade);
        }
        abrirImoveis(filtro);
    }

    private void abrirImoveis(Filtro filtro) {
        Intent intent = new Intent(this, ImoveisActivity.class);
        intent.putExtra(ImoveisActivity.EXTRA_FILTRO, filtro.paraJson().toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    // ---------------------------------------------------------------- destaques e opções

    private void carregar() {
        modoCarregado = Preferencias.modoDemonstracao(this);
        EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        imovelRepository.destaques(new Tarefas.Callback<List<Imovel>>() {
            @Override
            public void sucesso(List<Imovel> destaques) {
                boolean offline = !destaques.isEmpty() && destaques.get(0).offline;
                banner.setVisibility(offline ? View.VISIBLE : View.GONE);
                adapter.substituir(destaques);
                if (destaques.isEmpty()) {
                    EstadoTela.mostrar(estado, EstadoTela.Tipo.VAZIO, getString(R.string.home_destaques_vazio));
                } else {
                    EstadoTela.esconder(estado);
                }
            }

            @Override
            public void erro(ApiException e) {
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(HomeActivity.this));
            }
        });
        imovelRepository.opcoes(new Tarefas.Callback<Filtro.Opcoes>() {
            @Override
            public void sucesso(Filtro.Opcoes opcoes) {
                int selecionada = spnCidade.getSelectedItemPosition();
                String cidadeAtual = selecionada > 0 && selecionada < cidades.size() ? cidades.get(selecionada) : null;
                cidades.clear();
                cidades.add(getString(R.string.opcao_selecione));
                cidades.addAll(opcoes.cidades);
                ((ArrayAdapter<?>) spnCidade.getAdapter()).notifyDataSetChanged();
                if (cidadeAtual != null && cidades.indexOf(cidadeAtual) > 0) {
                    spnCidade.setSelection(cidades.indexOf(cidadeAtual));
                }
            }

            @Override
            public void erro(ApiException e) {
                // Sem opções a cidade fica em "Selecione"; a busca continua funcionando.
            }
        });
    }

    // ---------------------------------------------------------------- cartões

    @Override
    public void aoTocar(Imovel imovel) {
        startActivity(DetalheImovelActivity.intent(this, imovel.id));
    }

    @Override
    public void aoFavoritar(Imovel imovel, int posicao) {
        favoritoRepository.alternar(imovel, new Tarefas.Callback<Boolean>() {
            @Override
            public void sucesso(Boolean favorito) {
                adapter.marcarFavorito(imovel.id, favorito);
                if (favorito) {
                    Snackbar.make(findViewById(R.id.main), R.string.favoritos_adicionado, Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.navInferior).show();
                }
            }

            @Override
            public void erro(ApiException e) {
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(HomeActivity.this),
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.navInferior).show();
            }
        });
    }
}
