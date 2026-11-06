package br.com.talisma.imoveis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Verificar e-mail (protótipo Autenticação por e-mail, 6.5, D29). Extras {@link #EXTRA_MODO}
 * ({@link #MODO_CADASTRO} | {@link #MODO_REDEFINICAO}) e {@link #EXTRA_EMAIL}. 4 campos de 1 dígito com
 * avanço automático e colar; "Reenviar código" bloqueado 30 s com contagem. CADASTRO: valida o código e já
 * entra (login automático) → {@code RESULT_OK}. REDEFINICAO: leva o código para NovaSenha → {@code RESULT_OK}
 * quando a senha for salva.
 */
public class VerificarEmailActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_EMAIL = "email";
    public static final String MODO_CADASTRO = "CADASTRO";
    public static final String MODO_REDEFINICAO = "REDEFINICAO";

    private static final int SEGUNDOS_REENVIO = 30;
    private static final String ESTADO_FIM_BLOQUEIO = "fimBloqueio";

    private ContaRepository contaRepository;
    private String modo;
    private String email;
    private EditText[] digitos;
    private TextView txtErroCodigo;
    private TextView txtReenviar;
    private Button btnEnviar;
    private long fimBloqueioReenvio;
    private CountDownTimer contagem;

    private final ActivityResultLauncher<Intent> novaSenha = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    public static Intent intent(Context context, String modo, String email) {
        return new Intent(context, VerificarEmailActivity.class)
                .putExtra(EXTRA_MODO, modo)
                .putExtra(EXTRA_EMAIL, email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verificar_email);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        modo = getIntent().getStringExtra(EXTRA_MODO);
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null) {
            email = "";
        }

        TextView txtInstrucao = findViewById(R.id.txtInstrucao);
        txtInstrucao.setText(Html.fromHtml(getString(R.string.verificar_texto) + "<br><b>" + Html.escapeHtml(email) + "</b>",
                Html.FROM_HTML_MODE_LEGACY));
        txtErroCodigo = findViewById(R.id.txtErroCodigo);
        txtReenviar = findViewById(R.id.txtReenviar);
        btnEnviar = findViewById(R.id.btnEnviar);
        digitos = new EditText[]{findViewById(R.id.edtDigito1), findViewById(R.id.edtDigito2),
                findViewById(R.id.edtDigito3), findViewById(R.id.edtDigito4)};
        for (int i = 0; i < digitos.length; i++) {
            configurarDigito(i);
        }

        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.txtCancelar).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        btnEnviar.setOnClickListener(v -> enviar());
        txtReenviar.setOnClickListener(v -> reenviar());

        // Primeiro acesso: o código acabou de ser enviado, então o reenvio já começa bloqueado (6.5)
        fimBloqueioReenvio = savedInstanceState != null
                ? savedInstanceState.getLong(ESTADO_FIM_BLOQUEIO, 0)
                : System.currentTimeMillis() + SEGUNDOS_REENVIO * 1000L;
        iniciarContagem();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ESTADO_FIM_BLOQUEIO, fimBloqueioReenvio);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contagem != null) {
            contagem.cancel();
        }
    }

    /** Avanço automático, colar o código inteiro e Backspace voltando ao campo anterior. */
    private void configurarDigito(int indice) {
        EditText campo = digitos[indice];
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                txtErroCodigo.setVisibility(View.GONE);
                String d = Formatos.digitos(s.toString());
                if (d.length() > 1) {
                    // Colou vários dígitos: distribui a partir deste campo
                    distribuir(indice, d);
                } else if (d.length() == 1 && indice < digitos.length - 1) {
                    digitos[indice + 1].requestFocus();
                }
            }
        });
        campo.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                    && campo.length() == 0 && indice > 0) {
                digitos[indice - 1].setText("");
                digitos[indice - 1].requestFocus();
                return true;
            }
            return false;
        });
        if (indice == digitos.length - 1) {
            campo.setOnEditorActionListener((v, actionId, event) -> {
                enviar();
                return true;
            });
        }
    }

    private void distribuir(int inicio, String valores) {
        int k = 0;
        for (int i = inicio; i < digitos.length && k < valores.length(); i++, k++) {
            digitos[i].setText(String.valueOf(valores.charAt(k)));
        }
        int ultimo = Math.min(inicio + valores.length(), digitos.length) - 1;
        digitos[ultimo].requestFocus();
        digitos[ultimo].setSelection(digitos[ultimo].length());
    }

    private String codigo() {
        StringBuilder sb = new StringBuilder();
        for (EditText d : digitos) {
            sb.append(d.getText());
        }
        return sb.toString();
    }

    private void enviar() {
        String codigo = codigo();
        if (!Validacoes.codigo(codigo)) {
            mostrarErro(getString(R.string.verificar_codigo_incompleto));
            return;
        }
        if (MODO_REDEFINICAO.equals(modo)) {
            novaSenha.launch(NovaSenhaActivity.intent(this, email, codigo));
            return;
        }
        btnEnviar.setEnabled(false);
        contaRepository.verificarEmail(email, codigo, new Tarefas.Callback<Usuario>() {
            @Override
            public void sucesso(Usuario usuario) {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void erro(ApiException e) {
                btnEnviar.setEnabled(true);
                mostrarErro(e.mensagemParaUsuario(VerificarEmailActivity.this));
            }
        });
    }

    private void mostrarErro(String mensagem) {
        txtErroCodigo.setText(mensagem);
        txtErroCodigo.setVisibility(View.VISIBLE);
    }

    private void reenviar() {
        if (System.currentTimeMillis() < fimBloqueioReenvio) {
            return;
        }
        txtReenviar.setEnabled(false);
        Tarefas.Callback<Void> callback = new Tarefas.Callback<Void>() {
            @Override
            public void sucesso(Void dado) {
                Snackbar.make(findViewById(R.id.main), R.string.verificar_reenviado, Snackbar.LENGTH_SHORT).show();
                fimBloqueioReenvio = System.currentTimeMillis() + SEGUNDOS_REENVIO * 1000L;
                iniciarContagem();
            }

            @Override
            public void erro(ApiException e) {
                txtReenviar.setEnabled(true);
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(VerificarEmailActivity.this),
                        Snackbar.LENGTH_LONG).show();
            }
        };
        if (MODO_REDEFINICAO.equals(modo)) {
            contaRepository.esqueciSenha(email, callback);
        } else {
            contaRepository.reenviarCodigo(email, callback);
        }
    }

    /** "Reenviar em 30 s" até liberar o link (6.5). */
    private void iniciarContagem() {
        if (contagem != null) {
            contagem.cancel();
        }
        long restante = fimBloqueioReenvio - System.currentTimeMillis();
        if (restante <= 0) {
            liberarReenvio();
            return;
        }
        txtReenviar.setEnabled(false);
        contagem = new CountDownTimer(restante, 1000) {
            @Override
            public void onTick(long faltam) {
                txtReenviar.setText(getString(R.string.verificar_reenviar_em, (int) Math.ceil(faltam / 1000.0)));
            }

            @Override
            public void onFinish() {
                liberarReenvio();
            }
        }.start();
    }

    private void liberarReenvio() {
        txtReenviar.setText(R.string.verificar_reenviar);
        txtReenviar.setEnabled(true);
    }
}
