package br.com.talisma.imoveis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Nova senha, passo 3 do Esqueci a senha (D29, D34, 6.5): extras e-mail e código; Nova senha + Confirmar →
 * redefine → {@code RESULT_OK} (o Login mostra "Senha alterada. Entre com a nova senha").
 */
public class NovaSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CODIGO = "codigo";

    private ContaRepository contaRepository;
    private TextInputLayout tilSenha;
    private TextInputLayout tilConfirmarSenha;
    private EditText edtSenha;
    private EditText edtConfirmarSenha;
    private Button btnSalvar;

    public static Intent intent(Context context, String email, String codigo) {
        return new Intent(context, NovaSenhaActivity.class)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_CODIGO, codigo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nova_senha);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        tilSenha = findViewById(R.id.tilSenha);
        tilConfirmarSenha = findViewById(R.id.tilConfirmarSenha);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);
        btnSalvar = findViewById(R.id.btnSalvar);

        btnSalvar.setOnClickListener(v -> salvar());
        edtConfirmarSenha.setOnEditorActionListener((v, actionId, event) -> {
            salvar();
            return true;
        });
    }

    private void salvar() {
        String senha = texto(edtSenha);
        String confirmacao = texto(edtConfirmarSenha);
        tilSenha.setError(null);
        tilConfirmarSenha.setError(null);
        boolean valido = true;
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
        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        contaRepository.redefinirSenha(email, codigo, senha, new Tarefas.Callback<Void>() {
            @Override
            public void sucesso(Void dado) {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void erro(ApiException e) {
                btnSalvar.setEnabled(true);
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(NovaSenhaActivity.this),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
