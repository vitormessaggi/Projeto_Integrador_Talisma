package br.com.talisma.imoveis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Login (protótipo Login do cliente, 6.5, D9, D33). Extra {@link #EXTRA_MODO} = {@link Usuario#CLIENTE}
 * (padrão) | {@link Usuario#CORRETOR} ("Área do corretor", sem cadastro/esqueci). Quem exige login abre
 * esta tela com {@code registerForActivityResult} e continua a ação no {@code RESULT_OK}. Extra
 * {@link #EXTRA_MENSAGEM}: aviso mostrado ao abrir ("Sua sessão expirou").
 * <p>
 * Corretor (D3, D33, 6.3): login no modo CORRETOR abre SelecionarImovel (a não ser que
 * {@link #EXTRA_ABRIR_AREA_CORRETOR} seja {@code false} — quando é a própria SelecionarImovel que pediu o
 * login); se a sessão já é de corretor, tocar em "Acesse a área interna" abre SelecionarImovel direto.
 * <p>
 * Erros: credencial inválida no campo senha; conta bloqueada em diálogo (RN-002); e-mail não verificado →
 * VerificarEmail(CADASTRO); perfil diferente do modo → "Esta conta não é de corretor/cliente" no campo e-mail.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_MENSAGEM = "mensagem";
    /** {@code false} = não abrir SelecionarImovel após o login de corretor (padrão {@code true}). */
    public static final String EXTRA_ABRIR_AREA_CORRETOR = "abrir_area_corretor";

    private ContaRepository contaRepository;
    private String modo;
    private TextInputLayout tilEmail;
    private TextInputLayout tilSenha;
    private EditText edtEmail;
    private EditText edtSenha;
    private Button btnEntrar;

    private final ActivityResultLauncher<Intent> fluxoQueLoga = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    private final ActivityResultLauncher<Intent> esqueciSenha = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    Snackbar.make(findViewById(R.id.main), R.string.login_senha_alterada, Snackbar.LENGTH_LONG).show();
                }
            });

    /** Intent de login no modo dado (sem mensagem). */
    public static Intent intent(Context context, String modo) {
        return new Intent(context, LoginActivity.class).putExtra(EXTRA_MODO, modo);
    }

    /** Intent de login do cliente com o aviso "Sua sessão expirou" (6.6, D35). */
    public static Intent intentSessaoExpirada(Context context, ApiException e) {
        return intent(context, Usuario.CLIENTE).putExtra(EXTRA_MENSAGEM, e.mensagemParaUsuario(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contaRepository = new ContaRepository(this);
        modo = getIntent().getStringExtra(EXTRA_MODO);
        if (modo == null) {
            modo = Usuario.CLIENTE;
        }

        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);

        btnEntrar.setOnClickListener(v -> entrar());
        edtSenha.setOnEditorActionListener((v, actionId, event) -> {
            entrar();
            return true;
        });
        findViewById(R.id.txtEsqueciSenha).setOnClickListener(v ->
                esqueciSenha.launch(new Intent(this, EsqueciSenhaActivity.class)));
        findViewById(R.id.txtCadastrar).setOnClickListener(v ->
                fluxoQueLoga.launch(new Intent(this, CadastroActivity.class)));
        findViewById(R.id.txtAreaInterna).setOnClickListener(v -> abrirAreaInterna());
        findViewById(R.id.txtSouCliente).setOnClickListener(v -> trocarModo(Usuario.CLIENTE));

        // Aberta já no modo CORRETOR com sessão de corretor: vai direto para a área interna (6.3)
        if (savedInstanceState == null && Usuario.CORRETOR.equals(modo) && abrirAreaCorretor()
                && SelecionarImovelActivity.sessaoDeCorretor(this)) {
            startActivity(SelecionarImovelActivity.intent(this));
            setResult(RESULT_OK);
            finish();
            return;
        }

        aplicarModo();

        String mensagem = getIntent().getStringExtra(EXTRA_MENSAGEM);
        if (savedInstanceState == null && mensagem != null) {
            Snackbar.make(findViewById(R.id.main), mensagem, Snackbar.LENGTH_LONG).show();
        }
    }

    private void trocarModo(String novoModo) {
        modo = novoModo;
        getIntent().putExtra(EXTRA_MODO, modo);
        tilEmail.setError(null);
        tilSenha.setError(null);
        aplicarModo();
    }

    /** "É corretor? Acesse a área interna": sessão de corretor abre SelecionarImovel direto; senão troca o modo (6.3). */
    private void abrirAreaInterna() {
        if (SelecionarImovelActivity.sessaoDeCorretor(this)) {
            startActivity(SelecionarImovelActivity.intent(this));
            setResult(RESULT_OK);
            finish();
            return;
        }
        trocarModo(Usuario.CORRETOR);
    }

    private boolean abrirAreaCorretor() {
        return getIntent().getBooleanExtra(EXTRA_ABRIR_AREA_CORRETOR, true);
    }

    /** Modo CORRETOR: título "Área do corretor", esconde cadastro/esqueci/corretor e mostra "Sou cliente". */
    private void aplicarModo() {
        boolean corretor = Usuario.CORRETOR.equals(modo);
        TextView txtSubtitulo = findViewById(R.id.txtSubtitulo);
        txtSubtitulo.setText(corretor ? R.string.login_titulo_corretor : R.string.login_subtitulo);
        findViewById(R.id.txtEsqueciSenha).setVisibility(corretor ? View.GONE : View.VISIBLE);
        findViewById(R.id.linhaCadastro).setVisibility(corretor ? View.GONE : View.VISIBLE);
        findViewById(R.id.linhaCorretor).setVisibility(corretor ? View.GONE : View.VISIBLE);
        findViewById(R.id.txtSouCliente).setVisibility(corretor ? View.VISIBLE : View.GONE);
    }

    private void entrar() {
        String email = texto(edtEmail);
        String senha = texto(edtSenha);
        tilEmail.setError(null);
        tilSenha.setError(null);
        boolean valido = true;
        if (!Validacoes.email(email)) {
            tilEmail.setError(getString(R.string.conta_erro_email_invalido));
            valido = false;
        }
        if (Validacoes.vazio(senha)) {
            tilSenha.setError(getString(R.string.erro_campo_obrigatorio));
            valido = false;
        }
        if (!valido) {
            return;
        }
        btnEntrar.setEnabled(false);
        contaRepository.entrar(email, senha, modo, new Tarefas.Callback<Usuario>() {
            @Override
            public void sucesso(Usuario usuario) {
                if (usuario.isCorretor() && abrirAreaCorretor()) {
                    startActivity(SelecionarImovelActivity.intent(LoginActivity.this));
                }
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void erro(ApiException e) {
                btnEntrar.setEnabled(true);
                tratarErro(e, email);
            }
        });
    }

    private void tratarErro(ApiException e, String email) {
        if (e.isContaBloqueada()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.login_conta_bloqueada_titulo)
                    .setMessage(e.mensagemParaUsuario(this))
                    .setPositiveButton(R.string.acao_ok, null)
                    .show();
        } else if (e.isEmailNaoVerificado()) {
            fluxoQueLoga.launch(VerificarEmailActivity.intent(this, VerificarEmailActivity.MODO_CADASTRO, email));
        } else if (e.getStatus() == ApiException.NAO_AUTORIZADO) {
            tilSenha.setError(e.mensagemParaUsuario(this));
        } else if (e.getStatus() == ApiException.PERFIL_INCOMPATIVEL) {
            // "Esta conta não é de corretor/cliente" (6.5): a conta existe, mas não é do modo da tela
            tilEmail.setError(e.mensagemParaUsuario(this));
        } else {
            Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(this), Snackbar.LENGTH_LONG).show();
        }
    }

    private static String texto(@Nullable EditText campo) {
        return campo == null || campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
