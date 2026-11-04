package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.util.MascaraTexto;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Cadastro (protótipo Cadastro, 6.5, D12, D34): Nome, CPF opcional (máscara + dígitos verificadores),
 * E-mail, Senha (mín. 6), Confirmar senha e aceite dos Termos → VerificarEmail(CADASTRO). Devolve
 * {@code RESULT_OK} quando o fluxo termina logado. E-mail já cadastrado (409): erro no campo.
 */
public class CadastroActivity extends AppCompatActivity {

    private ContaRepository contaRepository;
    private TextInputLayout tilNome;
    private TextInputLayout tilCpf;
    private TextInputLayout tilEmail;
    private TextInputLayout tilSenha;
    private TextInputLayout tilConfirmarSenha;
    private EditText edtNome;
    private EditText edtCpf;
    private EditText edtEmail;
    private EditText edtSenha;
    private EditText edtConfirmarSenha;
    private CheckBox chkTermos;
    private View txtErroTermos;
    private Button btnCadastrar;

    private final ActivityResultLauncher<Intent> verificarEmail = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        tilNome = findViewById(R.id.tilNome);
        tilCpf = findViewById(R.id.tilCpf);
        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        tilConfirmarSenha = findViewById(R.id.tilConfirmarSenha);
        edtNome = findViewById(R.id.edtNome);
        edtCpf = findViewById(R.id.edtCpf);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);
        chkTermos = findViewById(R.id.chkTermos);
        txtErroTermos = findViewById(R.id.txtErroTermos);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        MascaraTexto.cpf(edtCpf);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.txtVerTermos).setOnClickListener(v -> startActivity(new Intent(this, TermosActivity.class)));
        chkTermos.setOnCheckedChangeListener((b, marcado) -> {
            if (marcado) {
                txtErroTermos.setVisibility(View.GONE);
            }
        });
        btnCadastrar.setOnClickListener(v -> cadastrar());
        edtConfirmarSenha.setOnEditorActionListener((v, actionId, event) -> {
            cadastrar();
            return true;
        });
    }

    private void cadastrar() {
        String nome = texto(edtNome);
        String cpf = texto(edtCpf);
        String email = texto(edtEmail);
        String senha = texto(edtSenha);
        String confirmacao = texto(edtConfirmarSenha);
        tilNome.setError(null);
        tilCpf.setError(null);
        tilEmail.setError(null);
        tilSenha.setError(null);
        tilConfirmarSenha.setError(null);
        txtErroTermos.setVisibility(View.GONE);

        boolean valido = true;
        if (Validacoes.vazio(nome)) {
            tilNome.setError(getString(R.string.conta_erro_nome_obrigatorio));
            valido = false;
        }
        if (!Validacoes.vazio(cpf) && !Validacoes.cpf(cpf)) {
            tilCpf.setError(getString(R.string.conta_erro_cpf_invalido));
            valido = false;
        }
        if (!Validacoes.email(email)) {
            tilEmail.setError(getString(R.string.conta_erro_email_invalido));
            valido = false;
        }
        if (!Validacoes.senha(senha)) {
            tilSenha.setError(getString(R.string.conta_erro_senha_curta));
            valido = false;
        }
        if (!Validacoes.senhasIguais(senha, confirmacao)) {
            tilConfirmarSenha.setError(getString(R.string.erro_senhas_diferentes));
            valido = false;
        }
        if (!chkTermos.isChecked()) {
            txtErroTermos.setVisibility(View.VISIBLE);
            valido = false;
        }
        if (!valido) {
            return;
        }

        btnCadastrar.setEnabled(false);
        contaRepository.cadastrar(nome, Validacoes.vazio(cpf) ? null : cpf, email, senha, new Tarefas.Callback<Void>() {
            @Override
            public void sucesso(Void dado) {
                btnCadastrar.setEnabled(true);
                verificarEmail.launch(VerificarEmailActivity.intent(CadastroActivity.this,
                        VerificarEmailActivity.MODO_CADASTRO, email));
            }

            @Override
            public void erro(ApiException e) {
                btnCadastrar.setEnabled(true);
                if (e.getStatus() == ApiException.CONFLITO) {
                    tilEmail.setError(e.mensagemParaUsuario(CadastroActivity.this));
                } else {
                    Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(CadastroActivity.this),
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
