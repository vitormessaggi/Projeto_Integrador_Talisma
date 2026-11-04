package br.com.talisma.imoveis;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.FavoritoRepository;
import br.com.talisma.imoveis.dados.Filtro;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.dados.Pagina;
import br.com.talisma.imoveis.util.BarraNavegacao;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Localizacao;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Lista de imóveis (6.5, D16, D28, D32): abas À venda | Locação, chips que abrem sheets de filtro,
 * "Perto de mim" (D15), rolagem infinita de 10 em 10, buscas recentes/último filtro (2.3.3), banner
 * offline (D36) e estado salvo na rotação (D20).
 */
public class ImoveisActivity extends AppCompatActivity implements ImovelAdapter.Ouvinte {

    public static final String EXTRA_FILTRO = "filtro";

    private static final String TAG = "ImoveisActivity";
    private static final int ITENS_ANTES_DO_FIM = 3;
    private static final String ESTADO_FILTRO = "filtro";
    private static final String ESTADO_ITENS = "itens";
    private static final String ESTADO_PAGINA = "pagina";
    private static final String ESTADO_TOTAL_PAGINAS = "totalPaginas";
    private static final String ESTADO_OFFLINE = "offline";
    private static final String ESTADO_ORIGEM = "origem";

    private ImovelRepository imovelRepository;
    private FavoritoRepository favoritoRepository;
    private ImovelAdapter adapter;
    private GridLayoutManager layoutManager;
    private View estado;
    private View banner;
    private View prgMais;
    private MaterialButtonToggleGroup tgFinalidade;
    private Chip chipPreco;
    private Chip chipTipo;
    private Chip chipQuartos;
    private Chip chipCidade;
    private Chip chipPertoDeMim;

    private Filtro filtro = new Filtro();
    private int pagina;
    private int totalPaginas;
    private boolean offline;
    private boolean carregando;
    private boolean vazio;
    private boolean sincronizandoViews;
    @Nullable
    private Location origem;

    private ActivityResultLauncher<String[]> pedidoLocalizacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_imoveis);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BarraNavegacao.configurar(this, R.id.nav_imoveis);

        imovelRepository = new ImovelRepository(this);
        favoritoRepository = new FavoritoRepository(this);
        estado = findViewById(R.id.estadoTela);
        banner = findViewById(R.id.txtBannerOffline);
        prgMais = findViewById(R.id.prgMais);
        tgFinalidade = findViewById(R.id.tgFinalidade);
        chipPreco = findViewById(R.id.chipPreco);
        chipTipo = findViewById(R.id.chipTipo);
        chipQuartos = findViewById(R.id.chipQuartos);
        chipCidade = findViewById(R.id.chipCidade);
        chipPertoDeMim = findViewById(R.id.chipPertoDeMim);

        RecyclerView rvImoveis = findViewById(R.id.rvImoveis);
        layoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.colunas_lista));
        rvImoveis.setLayoutManager(layoutManager);
        adapter = new ImovelAdapter(ImovelAdapter.Modo.LISTA, this);
        rvImoveis.setAdapter(adapter);
        rvImoveis.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && !carregando && pagina + 1 < totalPaginas
                        && layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - ITENS_ANTES_DO_FIM) {
                    carregar(pagina + 1);
                }
            }
        });

        tgFinalidade.addOnButtonCheckedListener((grupo, id, checked) -> {
            if (checked && !sincronizandoViews) {
                filtro.finalidade = id == R.id.btnLocacao ? Imovel.LOCACAO : Imovel.VENDA;
                aplicarFiltro();
            }
        });
        chipPreco.setOnClickListener(v -> abrirSheetPreco());
        chipTipo.setOnClickListener(v -> abrirSheetTipo());
        chipQuartos.setOnClickListener(v -> abrirSheetQuartos());
        chipCidade.setOnClickListener(v -> abrirSheetCidade());
        chipPertoDeMim.setOnCheckedChangeListener((chip, checked) -> {
            if (!sincronizandoViews) {
                alternarPertoDeMim(checked);
            }
        });
        EstadoTela.aoTentarNovamente(estado, () -> {
            if (vazio) {
                limparFiltros();
            } else {
                carregar(pagina);
            }
        });

        pedidoLocalizacao = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::aoResponderLocalizacao);

        if (savedInstanceState != null) {
            restaurar(savedInstanceState);
        } else if (!aplicarFiltroDaIntent(getIntent())) {
            // Sem extra: usa o último filtro salvo (2.3.3)
            imovelRepository.ultimoFiltro(new Tarefas.Callback<Filtro>() {
                @Override
                public void sucesso(Filtro ultimo) {
                    filtro = ultimo != null ? ultimo : new Filtro();
                    filtro.pertoDeMim = false; // precisa de localização nova; o chip religa
                    if (filtro.finalidade == null) {
                        filtro.finalidade = Imovel.VENDA;
                    }
                    atualizarViews();
                    recarregar();
                }

                @Override
                public void erro(ApiException e) {
                    filtro = new Filtro();
                    filtro.finalidade = Imovel.VENDA;
                    atualizarViews();
                    recarregar();
                }
            });
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        aplicarFiltroDaIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_FILTRO, filtro.paraJson().toString());
        JSONArray itens = new JSONArray();
        for (Imovel i : adapter.getItens()) {
            JSONObject o = i.paraJson();
            try {
                o.putOpt("distanciaKm", i.distanciaKm);
            } catch (JSONException e) {
                Log.w(TAG, "Distância não salva", e);
            }
            itens.put(o);
        }
        outState.putString(ESTADO_ITENS, itens.toString());
        outState.putInt(ESTADO_PAGINA, pagina);
        outState.putInt(ESTADO_TOTAL_PAGINAS, totalPaginas);
        outState.putBoolean(ESTADO_OFFLINE, offline);
        outState.putParcelable(ESTADO_ORIGEM, origem);
    }

    private void restaurar(Bundle estadoSalvo) {
        try {
            filtro = Filtro.deJson(new JSONObject(estadoSalvo.getString(ESTADO_FILTRO, "{}")));
            List<Imovel> itens = new ArrayList<>();
            JSONArray lista = new JSONArray(estadoSalvo.getString(ESTADO_ITENS, "[]"));
            for (int k = 0; k < lista.length(); k++) {
                JSONObject o = lista.getJSONObject(k);
                Imovel i = Imovel.deJson(o);
                i.distanciaKm = o.isNull("distanciaKm") ? null : (float) o.getDouble("distanciaKm");
                itens.add(i);
            }
            pagina = estadoSalvo.getInt(ESTADO_PAGINA, 0);
            totalPaginas = estadoSalvo.getInt(ESTADO_TOTAL_PAGINAS, 0);
            offline = estadoSalvo.getBoolean(ESTADO_OFFLINE, false);
            origem = BundleCompat.getParcelable(estadoSalvo, ESTADO_ORIGEM, Location.class);
            atualizarViews();
            banner.setVisibility(offline ? View.VISIBLE : View.GONE);
            adapter.substituir(itens);
            if (itens.isEmpty()) {
                recarregar();
            }
        } catch (JSONException e) {
            Log.w(TAG, "Estado ilegível; recarregando", e);
            filtro = new Filtro();
            filtro.finalidade = Imovel.VENDA;
            atualizarViews();
            recarregar();
        }
    }

    /** Aplica o filtro vindo da Home (extra {@code filtro} em JSON). Devolve false se não houver extra. */
    private boolean aplicarFiltroDaIntent(Intent intent) {
        String json = intent != null ? intent.getStringExtra(EXTRA_FILTRO) : null;
        if (json == null) {
            return false;
        }
        intent.removeExtra(EXTRA_FILTRO);
        try {
            filtro = Filtro.deJson(new JSONObject(json));
        } catch (JSONException e) {
            Log.w(TAG, "Filtro da Home ilegível", e);
            filtro = new Filtro();
        }
        if (filtro.finalidade == null) {
            filtro.finalidade = Imovel.VENDA;
        }
        aplicarFiltro();
        return true;
    }

    // ---------------------------------------------------------------- filtro e chips

    private void aplicarFiltro() {
        imovelRepository.salvarBusca(filtro);
        atualizarViews();
        recarregar();
    }

    private void limparFiltros() {
        String finalidade = filtro.finalidade;
        filtro = new Filtro();
        filtro.finalidade = finalidade;
        origem = null;
        aplicarFiltro();
    }

    /** Sincroniza abas e chips com o filtro atual (rótulo com o valor — D32). */
    private void atualizarViews() {
        sincronizandoViews = true;
        tgFinalidade.check(Imovel.LOCACAO.equals(filtro.finalidade) ? R.id.btnLocacao : R.id.btnVenda);

        String preco = null;
        if (filtro.precoMin != null && filtro.precoMax != null) {
            preco = getString(R.string.filtro_preco_faixa, Formatos.moeda(filtro.precoMin), Formatos.moeda(filtro.precoMax));
        } else if (filtro.precoMin != null) {
            preco = getString(R.string.filtro_preco_a_partir, Formatos.moeda(filtro.precoMin));
        } else if (filtro.precoMax != null) {
            preco = getString(R.string.filtro_preco_ate, Formatos.moeda(filtro.precoMax));
        }
        configurarChip(chipPreco, preco, R.string.filtro_preco);
        configurarChip(chipTipo, filtro.tipo != null ? tipoRotulo(filtro.tipo) : null, R.string.filtro_tipo);
        configurarChip(chipQuartos, filtro.quartosMin != null
                ? getString(R.string.filtro_quartos_valor, filtro.quartosMin) : null, R.string.filtro_quartos);
        configurarChip(chipCidade, filtro.cidade, R.string.filtro_cidade);
        chipPertoDeMim.setChecked(filtro.pertoDeMim);
        sincronizandoViews = false;
    }

    private void configurarChip(Chip chip, @Nullable String valor, int rotuloPadrao) {
        chip.setChecked(valor != null);
        chip.setText(valor != null ? valor : getString(rotuloPadrao));
    }

    private String tipoRotulo(String tipo) {
        switch (tipo) {
            case Imovel.CASA:
                return getString(R.string.tipo_casa);
            case Imovel.APARTAMENTO:
                return getString(R.string.tipo_apartamento);
            case Imovel.TERRENO:
                return getString(R.string.tipo_terreno);
            default:
                return tipo;
        }
    }

    // ---------------------------------------------------------------- sheets (D32)

    private BottomSheetDialog novoSheet(int layout) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(layout);
        return sheet;
    }

    private void abrirSheetPreco() {
        atualizarViews(); // o toque marcou o chip; volta ao estado do filtro
        BottomSheetDialog sheet = novoSheet(R.layout.sheet_preco);
        TextInputEditText edtMin = sheet.findViewById(R.id.edtPrecoMin);
        TextInputEditText edtMax = sheet.findViewById(R.id.edtPrecoMax);
        TextView txtErro = sheet.findViewById(R.id.txtErroPreco);
        Button btnLimpar = sheet.findViewById(R.id.btnLimpar);
        Button btnAplicar = sheet.findViewById(R.id.btnAplicar);
        if (edtMin == null || edtMax == null || txtErro == null || btnLimpar == null || btnAplicar == null) {
            return;
        }
        if (filtro.precoMin != null) {
            edtMin.setText(String.valueOf(filtro.precoMin.longValue()));
        }
        if (filtro.precoMax != null) {
            edtMax.setText(String.valueOf(filtro.precoMax.longValue()));
        }
        btnLimpar.setOnClickListener(v -> {
            filtro.precoMin = null;
            filtro.precoMax = null;
            sheet.dismiss();
            aplicarFiltro();
        });
        btnAplicar.setOnClickListener(v -> {
            Double min = numero(edtMin.getText() != null ? edtMin.getText().toString() : "");
            Double max = numero(edtMax.getText() != null ? edtMax.getText().toString() : "");
            if (!Validacoes.faixaPreco(min, max)) {
                txtErro.setVisibility(View.VISIBLE); // RN-003
                return;
            }
            filtro.precoMin = min;
            filtro.precoMax = max;
            sheet.dismiss();
            aplicarFiltro();
        });
        sheet.setOnDismissListener(d -> atualizarViews());
        sheet.show();
    }

    @Nullable
    private static Double numero(String texto) {
        String digitos = Formatos.digitos(texto);
        return digitos.isEmpty() ? null : Double.valueOf(digitos);
    }

    private void abrirSheetTipo() {
        atualizarViews(); // o toque marcou o chip; volta ao estado do filtro
        BottomSheetDialog sheet = novoSheet(R.layout.sheet_tipo);
        RadioGroup rgTipo = sheet.findViewById(R.id.rgTipo);
        Button btnLimpar = sheet.findViewById(R.id.btnLimpar);
        Button btnAplicar = sheet.findViewById(R.id.btnAplicar);
        if (rgTipo == null || btnLimpar == null || btnAplicar == null) {
            return;
        }
        if (Imovel.CASA.equals(filtro.tipo)) {
            rgTipo.check(R.id.rbCasa);
        } else if (Imovel.APARTAMENTO.equals(filtro.tipo)) {
            rgTipo.check(R.id.rbApartamento);
        } else if (Imovel.TERRENO.equals(filtro.tipo)) {
            rgTipo.check(R.id.rbTerreno);
        }
        btnLimpar.setOnClickListener(v -> {
            filtro.tipo = null;
            sheet.dismiss();
            aplicarFiltro();
        });
        btnAplicar.setOnClickListener(v -> {
            int id = rgTipo.getCheckedRadioButtonId();
            filtro.tipo = id == R.id.rbCasa ? Imovel.CASA
                    : id == R.id.rbApartamento ? Imovel.APARTAMENTO
                    : id == R.id.rbTerreno ? Imovel.TERRENO : null;
            sheet.dismiss();
            aplicarFiltro();
        });
        sheet.setOnDismissListener(d -> atualizarViews());
        sheet.show();
    }

    private void abrirSheetQuartos() {
        atualizarViews(); // o toque marcou o chip; volta ao estado do filtro
        BottomSheetDialog sheet = novoSheet(R.layout.sheet_quartos);
        ChipGroup grupo = sheet.findViewById(R.id.grupoQuartos);
        Button btnLimpar = sheet.findViewById(R.id.btnLimpar);
        Button btnAplicar = sheet.findViewById(R.id.btnAplicar);
        if (grupo == null || btnLimpar == null || btnAplicar == null) {
            return;
        }
        int[] ids = {R.id.chipQuarto1, R.id.chipQuarto2, R.id.chipQuarto3, R.id.chipQuarto4};
        if (filtro.quartosMin != null && filtro.quartosMin >= 1) {
            grupo.check(ids[Math.min(filtro.quartosMin, ids.length) - 1]);
        }
        btnLimpar.setOnClickListener(v -> {
            filtro.quartosMin = null;
            sheet.dismiss();
            aplicarFiltro();
        });
        btnAplicar.setOnClickListener(v -> {
            int marcado = grupo.getCheckedChipId();
            filtro.quartosMin = null;
            for (int k = 0; k < ids.length; k++) {
                if (ids[k] == marcado) {
                    filtro.quartosMin = k + 1;
                }
            }
            sheet.dismiss();
            aplicarFiltro();
        });
        sheet.setOnDismissListener(d -> atualizarViews());
        sheet.show();
    }

    private void abrirSheetCidade() {
        atualizarViews(); // o toque marcou o chip; volta ao estado do filtro
        BottomSheetDialog sheet = novoSheet(R.layout.sheet_cidade);
        RadioGroup rgCidades = sheet.findViewById(R.id.rgCidades);
        View prgCidades = sheet.findViewById(R.id.prgCidades);
        Button btnLimpar = sheet.findViewById(R.id.btnLimpar);
        Button btnAplicar = sheet.findViewById(R.id.btnAplicar);
        if (rgCidades == null || prgCidades == null || btnLimpar == null || btnAplicar == null) {
            return;
        }
        imovelRepository.cidadesRecentes(new Tarefas.Callback<List<String>>() {
            @Override
            public void sucesso(List<String> recentes) {
                if (!recentes.isEmpty()) {
                    adicionarCabecalho(rgCidades, R.string.filtro_cidade_recentes);
                    for (String c : recentes) {
                        adicionarCidade(rgCidades, c);
                    }
                    adicionarCabecalho(rgCidades, R.string.filtro_cidade_todas);
                }
                imovelRepository.opcoes(new Tarefas.Callback<Filtro.Opcoes>() {
                    @Override
                    public void sucesso(Filtro.Opcoes opcoes) {
                        prgCidades.setVisibility(View.GONE);
                        for (String c : opcoes.cidades) {
                            adicionarCidade(rgCidades, c);
                        }
                    }

                    @Override
                    public void erro(ApiException e) {
                        prgCidades.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void erro(ApiException e) {
                prgCidades.setVisibility(View.GONE);
            }
        });
        btnLimpar.setOnClickListener(v -> {
            filtro.cidade = null;
            sheet.dismiss();
            aplicarFiltro();
        });
        btnAplicar.setOnClickListener(v -> {
            RadioButton marcado = rgCidades.findViewById(rgCidades.getCheckedRadioButtonId());
            filtro.cidade = marcado != null ? (String) marcado.getTag() : null;
            sheet.dismiss();
            aplicarFiltro();
        });
        sheet.setOnDismissListener(d -> atualizarViews());
        sheet.show();
    }

    private void adicionarCabecalho(RadioGroup grupo, int texto) {
        TextView cabecalho = new TextView(this, null, 0, R.style.Texto_Rotulo);
        cabecalho.setText(texto);
        cabecalho.setPadding(0, getResources().getDimensionPixelSize(R.dimen.espaco_12), 0,
                getResources().getDimensionPixelSize(R.dimen.espaco_4));
        grupo.addView(cabecalho, new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void adicionarCidade(RadioGroup grupo, String cidade) {
        RadioButton rb = new RadioButton(this);
        rb.setId(View.generateViewId());
        rb.setText(cidade);
        rb.setTag(cidade);
        rb.setMinHeight(getResources().getDimensionPixelSize(R.dimen.altura_minima_toque));
        grupo.addView(rb, new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        if (cidade.equalsIgnoreCase(filtro.cidade) && grupo.getCheckedRadioButtonId() == View.NO_ID) {
            rb.setChecked(true);
        }
    }

    // ---------------------------------------------------------------- Perto de mim (D15)

    private void alternarPertoDeMim(boolean ligar) {
        if (!ligar) {
            filtro.pertoDeMim = false;
            origem = null;
            aplicarFiltro();
            return;
        }
        if (!Localizacao.temPermissao(this)) {
            pedidoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        filtro.pertoDeMim = true;
        aplicarFiltro();
    }

    private void aoResponderLocalizacao(Map<String, Boolean> resultado) {
        if (Localizacao.temPermissao(this)) {
            filtro.pertoDeMim = true;
            aplicarFiltro();
            return;
        }
        desligarPertoDeMim();
        Snackbar.make(findViewById(R.id.main), R.string.localizacao_negada, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.navInferior)
                .setAction(R.string.localizacao_configuracoes, v -> startActivity(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null))))
                .show();
    }

    private void desligarPertoDeMim() {
        filtro.pertoDeMim = false;
        origem = null;
        atualizarViews();
    }

    // ---------------------------------------------------------------- carregamento (D28, D36)

    private void recarregar() {
        pagina = 0;
        totalPaginas = 0;
        adapter.substituir(new ArrayList<>());
        carregar(0);
    }

    private void carregar(int p) {
        if (carregando) {
            return;
        }
        carregando = true;
        vazio = false;
        if (p == 0) {
            EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        } else {
            prgMais.setVisibility(View.VISIBLE);
        }
        if (filtro.pertoDeMim && origem == null) {
            Localizacao.obter(this, new Tarefas.Callback<Location>() {
                @Override
                public void sucesso(Location localizacao) {
                    if (localizacao == null) {
                        carregando = false;
                        desligarPertoDeMim();
                        Snackbar.make(findViewById(R.id.main), R.string.localizacao_indisponivel, Snackbar.LENGTH_LONG)
                                .setAnchorView(R.id.navInferior).show();
                        carregar(p);
                        return;
                    }
                    origem = localizacao;
                    listar(p);
                }

                @Override
                public void erro(ApiException e) {
                    carregando = false;
                    desligarPertoDeMim();
                    carregar(p);
                }
            });
            return;
        }
        listar(p);
    }

    private void listar(int p) {
        imovelRepository.listar(filtro, p, origem, new Tarefas.Callback<Pagina<Imovel>>() {
            @Override
            public void sucesso(Pagina<Imovel> resultado) {
                carregando = false;
                prgMais.setVisibility(View.GONE);
                pagina = resultado.pagina;
                totalPaginas = resultado.totalPaginas;
                offline = resultado.offline;
                banner.setVisibility(offline ? View.VISIBLE : View.GONE);
                if (p == 0) {
                    adapter.substituir(resultado.itens);
                } else {
                    adapter.acrescentar(resultado.itens);
                }
                if (adapter.getItemCount() == 0) {
                    mostrarVazio();
                } else {
                    EstadoTela.esconder(estado);
                }
            }

            @Override
            public void erro(ApiException e) {
                carregando = false;
                prgMais.setVisibility(View.GONE);
                if (p == 0) {
                    EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                            e.mensagemParaUsuario(ImoveisActivity.this));
                    ((TextView) estado.findViewById(R.id.btnTentarNovamente)).setText(R.string.estado_tentar_novamente);
                } else {
                    Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(ImoveisActivity.this),
                            Snackbar.LENGTH_LONG).setAnchorView(R.id.navInferior).show();
                }
            }
        });
    }

    /** Vazio: "Nenhum imóvel encontrado com esses filtros" + botão "Limpar filtros" (6.5). */
    private void mostrarVazio() {
        vazio = true;
        EstadoTela.mostrar(estado, EstadoTela.Tipo.VAZIO, getString(R.string.imoveis_vazio));
        TextView btn = estado.findViewById(R.id.btnTentarNovamente);
        btn.setText(R.string.imoveis_limpar_filtros);
        btn.setVisibility(View.VISIBLE);
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
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(ImoveisActivity.this),
                        Snackbar.LENGTH_SHORT).setAnchorView(R.id.navInferior).show();
            }
        });
    }
}
