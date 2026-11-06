package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Alterar senha (nova, 6.5, D34): Senha atual, Nova senha, Confirmar → Toast e fecha. Exige login;
 * 401 (sessão expirada) abre o Login com retorno e repete ao voltar (6.6).
 */
public class AlterarSenhaActivity extends AppCompatActivity {

    private ContaRepository contaRepository;
    private TextInputLayout tilSenhaAtual;
    private TextInputLayout tilSenha;
    private TextInputLayout tilConfirmarSenha;
    private EditText edtSenhaAtual;
    private EditText edtSenha;
    private EditText edtConfirmarSenha;
    private Button btnSalvar;

    private final ActivityResultLauncher<Intent> login = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != RESULT_OK) {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alterar_senha);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        ((TextView) findViewById(R.id.txtTituloTela)).setText(R.string.alterar_senha_titulo);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        tilSenhaAtual = findViewById(R.id.tilSenhaAtual);
        tilSenha = findViewById(R.id.tilSenha);
        tilConfirmarSenha = findViewById(R.id.tilConfirmarSenha);
        edtSenhaAtual = findViewById(R.id.edtSenhaAtual);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);
        btnSalvar = findViewById(R.id.btnSalvar);

        btnSalvar.setOnClickListener(v -> salvar());
        edtConfirmarSenha.setOnEditorActionListener((v, actionId, event) -> {
            salvar();
            return true;
        });

        if (!Sessao.logado(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CLIENTE));
        }
    }

    private void salvar() {
        String senhaAtual = texto(edtSenhaAtual);
        String senha = texto(edtSenha);
        String confirmacao = texto(edtConfirmarSenha);
        tilSenhaAtual.setError(null);
        tilSenha.setError(null);
        tilConfirmarSenha.setError(null);
        boolean valido = true;
        if (Validacoes.vazio(senhaAtual)) {
            tilSenhaAtual.setError(getString(R.string.erro_campo_obrigatorio));
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
        if (!valido) {
            return;
        }
        btnSalvar.setEnabled(false);
        contaRepository.alterarSenha(senhaAtual, senha, new Tarefas.Callback<Void>() {
            @Override
            public void sucesso(Void dado) {
                Toast.makeText(AlterarSenhaActivity.this, R.string.alterar_senha_sucesso, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void erro(ApiException e) {
                btnSalvar.setEnabled(true);
                if (e.isSessaoExpirada()) {
                    login.launch(LoginActivity.intentSessaoExpirada(AlterarSenhaActivity.this, e));
                } else if (e.getStatus() == 400) {
                    tilSenhaAtual.setError(e.mensagemParaUsuario(AlterarSenhaActivity.this));
                } else {
                    Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(AlterarSenhaActivity.this),
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
