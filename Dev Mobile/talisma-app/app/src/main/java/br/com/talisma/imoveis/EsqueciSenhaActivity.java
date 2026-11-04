package br.com.talisma.imoveis;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Esqueci a senha, passo 1 (D29, 6.5): e-mail → envia o código → VerificarEmail(REDEFINICAO) → NovaSenha.
 * Devolve {@code RESULT_OK} ao Login quando a senha for alterada (Snackbar lá).
 */
public class EsqueciSenhaActivity extends AppCompatActivity {

    private ContaRepository contaRepository;
    private TextInputLayout tilEmail;
    private EditText edtEmail;
    private Button btnEnviarCodigo;

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
        setContentView(R.layout.activity_esqueci_senha);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        tilEmail = findViewById(R.id.tilEmail);
        edtEmail = findViewById(R.id.edtEmail);
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo);

        btnEnviarCodigo.setOnClickListener(v -> enviarCodigo());
        edtEmail.setOnEditorActionListener((v, actionId, event) -> {
            enviarCodigo();
            return true;
        });
        findViewById(R.id.txtVoltarLogin).setOnClickListener(v -> finish());
    }

    private void enviarCodigo() {
        String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        tilEmail.setError(null);
        if (!Validacoes.email(email)) {
            tilEmail.setError(getString(R.string.conta_erro_email_invalido));
            return;
        }
        btnEnviarCodigo.setEnabled(false);
        contaRepository.esqueciSenha(email, new Tarefas.Callback<Void>() {
            @Override
            public void sucesso(Void dado) {
                btnEnviarCodigo.setEnabled(true);
                verificarEmail.launch(VerificarEmailActivity.intent(EsqueciSenhaActivity.this,
                        VerificarEmailActivity.MODO_REDEFINICAO, email));
            }

            @Override
            public void erro(ApiException e) {
                btnEnviarCodigo.setEnabled(true);
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(EsqueciSenhaActivity.this),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
