package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.MascaraTexto;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Dados pessoais (nova, 6.5, D12, D30): carrega o perfil do servidor (estado_tela); Nome, E-mail (somente
 * leitura), CPF (mascarado "***.***.***-12" quando já existe, com "Editar" só se vazio) e Telefone/WhatsApp
 * com máscara; Salvar → Toast e fecha. Exige login; 401 abre o Login com retorno.
 */
public class DadosPessoaisActivity extends AppCompatActivity {

    private ContaRepository contaRepository;
    private View estado;
    private View conteudo;
    private TextInputLayout tilNome;
    private TextInputLayout tilCpf;
    private TextInputLayout tilTelefone;
    private EditText edtNome;
    private EditText edtEmail;
    private EditText edtCpf;
    private EditText edtTelefone;
    private Button btnSalvar;
    private static final String ESTADO_CARREGADO = "carregado";
    private static final String ESTADO_CPF_INFORMADO = "cpfInformado";

    private boolean cpfJaInformado;
    private boolean mascaraCpfAplicada;
    private boolean carregado;

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
        setContentView(R.layout.activity_dados_pessoais);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        ((TextView) findViewById(R.id.txtTituloTela)).setText(R.string.dados_titulo);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        estado = findViewById(R.id.estadoTela);
        conteudo = findViewById(R.id.scrollConteudo);
        tilNome = findViewById(R.id.tilNome);
        tilCpf = findViewById(R.id.tilCpf);
        tilTelefone = findViewById(R.id.tilTelefone);
        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        edtCpf = findViewById(R.id.edtCpf);
        edtTelefone = findViewById(R.id.edtTelefone);
        btnSalvar = findViewById(R.id.btnSalvar);

        MascaraTexto.telefone(edtTelefone);
        btnSalvar.setOnClickListener(v -> salvar());
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        // Na rotação os campos já preenchidos são restaurados pelo próprio EditText (D20)
        carregado = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_CARREGADO, false);
        if (carregado) {
            cpfJaInformado = savedInstanceState.getBoolean(ESTADO_CPF_INFORMADO, false);
            configurarCpf();
        }
        if (!Sessao.logado(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CLIENTE));
        } else if (!carregado) {
            carregar();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_CARREGADO, carregado);
        outState.putBoolean(ESTADO_CPF_INFORMADO, cpfJaInformado);
    }

    private void carregar() {
        conteudo.setVisibility(View.INVISIBLE);
        EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        contaRepository.perfil(new Tarefas.Callback<Usuario>() {
            @Override
            public void sucesso(Usuario usuario) {
                carregado = true;
                preencher(usuario);
                EstadoTela.esconder(estado);
                conteudo.setVisibility(View.VISIBLE);
            }

            @Override
            public void erro(ApiException e) {
                if (e.isSessaoExpirada()) {
                    login.launch(LoginActivity.intentSessaoExpirada(DadosPessoaisActivity.this, e));
                    return;
                }
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(DadosPessoaisActivity.this));
            }
        });
    }

    private void preencher(Usuario usuario) {
        edtNome.setText(usuario.nome);
        edtEmail.setText(usuario.email);
        edtTelefone.setText(Formatos.telefone(usuario.telefone));
        cpfJaInformado = !Validacoes.vazio(usuario.cpf);
        // CPF só mascarado, nunca editável nem gravado localmente (D12)
        edtCpf.setText(cpfJaInformado ? Formatos.cpfOculto(usuario.cpf) : "");
        configurarCpf();
    }

    /** CPF já informado: campo desabilitado com "***.***.***-12"; vazio: editável com máscara e "Editar". */
    private void configurarCpf() {
        tilCpf.setEnabled(!cpfJaInformado);
        tilCpf.setHelperText(cpfJaInformado ? null : getString(R.string.dados_cpf_editar));
        if (!cpfJaInformado && !mascaraCpfAplicada) {
            mascaraCpfAplicada = true;
            MascaraTexto.cpf(edtCpf);
        }
    }

    private void salvar() {
        String nome = texto(edtNome);
        String cpf = cpfJaInformado ? null : texto(edtCpf);
        String telefone = texto(edtTelefone);
        tilNome.setError(null);
        tilCpf.setError(null);
        tilTelefone.setError(null);
        boolean valido = true;
        if (Validacoes.vazio(nome)) {
            tilNome.setError(getString(R.string.conta_erro_nome_obrigatorio));
            valido = false;
        }
        if (!Validacoes.vazio(cpf) && !Validacoes.cpf(cpf)) {
            tilCpf.setError(getString(R.string.conta_erro_cpf_invalido));
            valido = false;
        }
        if (!Validacoes.vazio(telefone) && !Validacoes.telefone(telefone)) {
            tilTelefone.setError(getString(R.string.conta_erro_telefone_invalido));
            valido = false;
        }
        if (!valido) {
            return;
        }
        btnSalvar.setEnabled(false);
        contaRepository.salvarPerfil(nome, Validacoes.vazio(cpf) ? null : cpf,
                Validacoes.vazio(telefone) ? null : Formatos.digitos(telefone), new Tarefas.Callback<Usuario>() {
                    @Override
                    public void sucesso(Usuario usuario) {
                        Toast.makeText(DadosPessoaisActivity.this, R.string.dados_salvos, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void erro(ApiException e) {
                        btnSalvar.setEnabled(true);
                        if (e.isSessaoExpirada()) {
                            login.launch(LoginActivity.intentSessaoExpirada(DadosPessoaisActivity.this, e));
                        } else {
                            Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(DadosPessoaisActivity.this),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
