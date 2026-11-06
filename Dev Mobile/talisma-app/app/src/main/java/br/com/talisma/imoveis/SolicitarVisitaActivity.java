package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.dados.Visita;
import br.com.talisma.imoveis.dados.VisitaRepository;
import br.com.talisma.imoveis.util.CarregadorImagem;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.MascaraTexto;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Solicitar visita (D4, D9, D30, 6.5). Extra {@code imovel_id}. Sem login abre o Login com retorno e
 * continua; carrega o imóvel (resumo) e o perfil (telefone pré-preenchido; CPF só se faltar — D12) com
 * estado_tela. Data a partir de amanhã, período Manhã/Tarde/Noite, telefone obrigatório, mensagem até 300.
 * Sucesso: diálogo "Solicitação enviada!" com "Ver meus contatos" | "OK" (gera o lead — RN-004).
 */
public class SolicitarVisitaActivity extends AppCompatActivity {

    private static final String TAG_DATA = "dataVisita";
    private static final String ESTADO_DATA = "dataSelecionada";
    private static final String ESTADO_PEDE_CPF = "pedeCpf";
    private static final String ESTADO_CARREGADO = "carregado";

    private ImovelRepository imovelRepository;
    private ContaRepository contaRepository;
    private VisitaRepository visitaRepository;
    private long imovelId;
    private Imovel imovel;
    /** Data escolhida em milissegundos UTC (o MaterialDatePicker trabalha em UTC); 0 = nenhuma. */
    private long dataSelecionada;
    private boolean pedeCpf;
    private boolean carregado;

    private View estado;
    private View conteudo;
    private TextInputLayout tilData;
    private TextInputLayout tilTelefone;
    private TextInputLayout tilCpf;
    private EditText edtData;
    private EditText edtTelefone;
    private EditText edtCpf;
    private EditText edtMensagem;
    private ChipGroup grupoPeriodo;
    private View txtErroPeriodo;
    private Button btnEnviar;

    private final ActivityResultLauncher<Intent> login = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    carregar();
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitar_visita);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imovelRepository = new ImovelRepository(this);
        contaRepository = new ContaRepository(this);
        visitaRepository = new VisitaRepository(this);
        imovelId = getIntent().getLongExtra(DetalheImovelActivity.EXTRA_IMOVEL_ID, 0);

        ((TextView) findViewById(R.id.txtTituloTela)).setText(R.string.visita_titulo);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        estado = findViewById(R.id.estadoTela);
        conteudo = findViewById(R.id.scrollConteudo);
        tilData = findViewById(R.id.tilData);
        tilTelefone = findViewById(R.id.tilTelefone);
        tilCpf = findViewById(R.id.tilCpf);
        edtData = findViewById(R.id.edtData);
        edtTelefone = findViewById(R.id.edtTelefone);
        edtCpf = findViewById(R.id.edtCpf);
        edtMensagem = findViewById(R.id.edtMensagem);
        grupoPeriodo = findViewById(R.id.grupoPeriodo);
        txtErroPeriodo = findViewById(R.id.txtErroPeriodo);
        btnEnviar = findViewById(R.id.btnEnviarSolicitacao);

        MascaraTexto.telefone(edtTelefone);
        MascaraTexto.cpf(edtCpf);
        edtData.setOnClickListener(v -> escolherData());
        tilData.setEndIconOnClickListener(v -> escolherData());
        grupoPeriodo.setOnCheckedStateChangeListener((g, ids) -> txtErroPeriodo.setVisibility(View.GONE));
        btnEnviar.setOnClickListener(v -> enviar());
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        if (savedInstanceState != null) {
            dataSelecionada = savedInstanceState.getLong(ESTADO_DATA, 0);
            pedeCpf = savedInstanceState.getBoolean(ESTADO_PEDE_CPF, false);
            carregado = savedInstanceState.getBoolean(ESTADO_CARREGADO, false);
        }
        MaterialDatePicker<?> pickerAberto = (MaterialDatePicker<?>) getSupportFragmentManager().findFragmentByTag(TAG_DATA);
        if (pickerAberto != null) {
            ligarPicker((MaterialDatePicker<Long>) pickerAberto);
        }

        if (!Sessao.logado(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CLIENTE));
        } else {
            carregar();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ESTADO_DATA, dataSelecionada);
        outState.putBoolean(ESTADO_PEDE_CPF, pedeCpf);
        outState.putBoolean(ESTADO_CARREGADO, carregado);
    }

    /** Imóvel (resumo) e perfil (telefone, CPF). Na rotação os campos digitados são mantidos (D20). */
    private void carregar() {
        conteudo.setVisibility(View.INVISIBLE);
        EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        imovelRepository.detalhe(imovelId, new Tarefas.Callback<Imovel>() {
            @Override
            public void sucesso(Imovel dado) {
                imovel = dado;
                mostrarImovel();
                carregarPerfil();
            }

            @Override
            public void erro(ApiException e) {
                mostrarErro(e);
            }
        });
    }

    private void carregarPerfil() {
        if (carregado) {
            mostrarFormulario();
            return;
        }
        contaRepository.perfil(new Tarefas.Callback<Usuario>() {
            @Override
            public void sucesso(Usuario usuario) {
                carregado = true;
                pedeCpf = Validacoes.vazio(usuario.cpf);
                if (!Validacoes.vazio(usuario.telefone) && edtTelefone.length() == 0) {
                    edtTelefone.setText(Formatos.telefone(usuario.telefone));
                }
                mostrarFormulario();
            }

            @Override
            public void erro(ApiException e) {
                mostrarErro(e);
            }
        });
    }

    private void mostrarErro(ApiException e) {
        if (e.isSessaoExpirada()) {
            login.launch(LoginActivity.intentSessaoExpirada(this, e));
            return;
        }
        EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                e.mensagemParaUsuario(this));
    }

    private void mostrarImovel() {
        ((TextView) findViewById(R.id.txtTitulo)).setText(imovel.titulo);
        ((TextView) findViewById(R.id.txtPreco)).setText(Formatos.preco(this, imovel));
        CarregadorImagem.carregar(findViewById(R.id.imgCapa), imovel.capa());
    }

    private void mostrarFormulario() {
        findViewById(R.id.txtRotuloCpf).setVisibility(pedeCpf ? View.VISIBLE : View.GONE);
        tilCpf.setVisibility(pedeCpf ? View.VISIBLE : View.GONE);
        atualizarData();
        EstadoTela.esconder(estado);
        conteudo.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- data

    private void escolherData() {
        long amanha = amanhaUtc();
        CalendarConstraints limites = new CalendarConstraints.Builder()
                .setStart(amanha)
                .setValidator(DateValidatorPointForward.from(amanha))
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.visita_data)
                .setCalendarConstraints(limites)
                .setSelection(dataSelecionada > 0 ? dataSelecionada : amanha)
                .build();
        ligarPicker(picker);
        picker.show(getSupportFragmentManager(), TAG_DATA);
    }

    private void ligarPicker(MaterialDatePicker<Long> picker) {
        picker.addOnPositiveButtonClickListener(selecao -> {
            dataSelecionada = selecao;
            tilData.setError(null);
            atualizarData();
        });
    }

    private void atualizarData() {
        edtData.setText(dataSelecionada > 0 ? Formatos.data(dataIso()) : "");
    }

    /** Meia-noite UTC de amanhã (o picker usa datas em UTC). */
    private static long amanhaUtc() {
        Calendar hoje = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH));
        utc.add(Calendar.DAY_OF_MONTH, 1);
        return utc.getTimeInMillis();
    }

    private String dataIso() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(dataSelecionada);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return Formatos.dataIso(new Date(local.getTimeInMillis()));
    }

    // ---------------------------------------------------------------- envio

    private String periodo() {
        int id = grupoPeriodo.getCheckedChipId();
        if (id == R.id.chipManha) {
            return Visita.MANHA;
        }
        if (id == R.id.chipTarde) {
            return Visita.TARDE;
        }
        if (id == R.id.chipNoite) {
            return Visita.NOITE;
        }
        return null;
    }

    private void enviar() {
        String periodo = periodo();
        String telefone = texto(edtTelefone);
        String cpf = pedeCpf ? texto(edtCpf) : null;
        String mensagem = texto(edtMensagem);
        tilData.setError(null);
        tilTelefone.setError(null);
        tilCpf.setError(null);
        txtErroPeriodo.setVisibility(View.GONE);

        boolean valido = true;
        if (dataSelecionada <= 0) {
            tilData.setError(getString(R.string.visita_data_obrigatoria));
            valido = false;
        }
        if (periodo == null) {
            txtErroPeriodo.setVisibility(View.VISIBLE);
            valido = false;
        }
        if (!Validacoes.telefone(telefone)) {
            tilTelefone.setError(getString(R.string.conta_erro_telefone_invalido));
            valido = false;
        }
        if (pedeCpf && !Validacoes.cpf(cpf)) {
            tilCpf.setError(getString(Validacoes.vazio(cpf) ? R.string.visita_erro_cpf_obrigatorio
                    : R.string.conta_erro_cpf_invalido));
            valido = false;
        }
        if (!valido) {
            return;
        }

        btnEnviar.setEnabled(false);
        visitaRepository.solicitar(imovelId, dataIso(), periodo, Formatos.digitos(telefone), cpf, mensagem,
                new Tarefas.Callback<Visita>() {
                    @Override
                    public void sucesso(Visita visita) {
                        mostrarSucesso();
                    }

                    @Override
                    public void erro(ApiException e) {
                        btnEnviar.setEnabled(true);
                        if (e.isSessaoExpirada()) {
                            login.launch(LoginActivity.intentSessaoExpirada(SolicitarVisitaActivity.this, e));
                        } else {
                            Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(SolicitarVisitaActivity.this),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /** Diálogo com check, "Solicitação enviada!" e "Ver meus contatos" | "OK"; os dois fecham a tela. */
    private void mostrarSucesso() {
        View corpo = getLayoutInflater().inflate(R.layout.dialog_visita_enviada, null);
        ((ImageView) corpo.findViewById(R.id.imgSucesso)).setImageResource(R.drawable.ic_check);
        new MaterialAlertDialogBuilder(this)
                .setView(corpo)
                .setCancelable(false)
                .setPositiveButton(R.string.acao_ok, (d, w) -> finish())
                .setNeutralButton(R.string.visita_ver_contatos, (d, w) -> {
                    startActivity(new Intent(this, MeusContatosActivity.class));
                    finish();
                })
                .show();
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
