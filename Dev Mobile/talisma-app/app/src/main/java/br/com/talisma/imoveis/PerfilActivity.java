package br.com.talisma.imoveis;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.FavoritoRepository;
import br.com.talisma.imoveis.dados.Preferencias;
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.BarraNavegacao;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Perfil (protótipo Perfil + D31, 6.5). Logado: cabeçalho com nome/e-mail, itens pessoais e Sair
 * (confirmação). Deslogado: convite "Entre para salvar visitas e favoritos" com Entrar/Cadastrar e só
 * Fale com a imobiliária e Termos. Fale com a imobiliária: sheet com Ligar/E-mail (D38). 5 toques na
 * versão: diálogo Modo demonstração + "Simular sem conexão" (D26, 6.7).
 */
public class PerfilActivity extends AppCompatActivity {

    private static final int TOQUES_MODO_DEMONSTRACAO = 5;

    private ContaRepository contaRepository;
    private int toquesVersao;

    private final ActivityResultLauncher<Intent> login = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> atualizarSessao());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BarraNavegacao.configurar(this, R.id.nav_perfil);
        contaRepository = new ContaRepository(this);

        findViewById(R.id.btnEntrar).setOnClickListener(v -> login.launch(LoginActivity.intent(this, Usuario.CLIENTE)));
        findViewById(R.id.btnCadastrar).setOnClickListener(v -> login.launch(new Intent(this, CadastroActivity.class)));
        findViewById(R.id.itemDadosPessoais).setOnClickListener(v -> startActivity(new Intent(this, DadosPessoaisActivity.class)));
        findViewById(R.id.itemMeusFavoritos).setOnClickListener(v -> {
            Intent intent = new Intent(this, FavoritosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.itemMeusContatos).setOnClickListener(v -> startActivity(new Intent(this, MeusContatosActivity.class)));
        findViewById(R.id.itemAlterarSenha).setOnClickListener(v -> startActivity(new Intent(this, AlterarSenhaActivity.class)));
        findViewById(R.id.itemFaleConosco).setOnClickListener(v -> abrirFaleConosco());
        findViewById(R.id.itemTermos).setOnClickListener(v -> startActivity(new Intent(this, TermosActivity.class)));
        findViewById(R.id.btnSair).setOnClickListener(v -> confirmarSaida());

        TextView txtVersao = findViewById(R.id.txtVersao);
        txtVersao.setText(getString(R.string.versao_app, BuildConfig.VERSION_NAME));
        txtVersao.setOnClickListener(v -> {
            toquesVersao++;
            if (toquesVersao >= TOQUES_MODO_DEMONSTRACAO) {
                toquesVersao = 0;
                abrirModoDemonstracao();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSessao();
    }

    /** Alterna entre o cabeçalho/itens de logado e o convite de deslogado (D31). */
    private void atualizarSessao() {
        Usuario usuario = Sessao.usuario(this);
        boolean logado = Sessao.logado(this) && usuario != null;
        TextView txtNome = findViewById(R.id.txtNome);
        TextView txtEmail = findViewById(R.id.txtEmail);
        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        if (logado) {
            txtNome.setText(usuario.nome);
            txtEmail.setText(usuario.email);
            imgAvatar.setContentDescription(getString(R.string.desc_avatar));
        } else {
            txtNome.setText(R.string.perfil_convidado);
            txtEmail.setText(R.string.perfil_convite);
            imgAvatar.setContentDescription(getString(R.string.perfil_convidado));
        }
        int seLogado = logado ? View.VISIBLE : View.GONE;
        int seDeslogado = logado ? View.GONE : View.VISIBLE;
        findViewById(R.id.btnEntrar).setVisibility(seDeslogado);
        findViewById(R.id.btnCadastrar).setVisibility(seDeslogado);
        findViewById(R.id.itemDadosPessoais).setVisibility(seLogado);
        findViewById(R.id.divDadosPessoais).setVisibility(seLogado);
        findViewById(R.id.itemMeusFavoritos).setVisibility(seLogado);
        findViewById(R.id.divMeusFavoritos).setVisibility(seLogado);
        findViewById(R.id.itemMeusContatos).setVisibility(seLogado);
        findViewById(R.id.divMeusContatos).setVisibility(seLogado);
        findViewById(R.id.itemAlterarSenha).setVisibility(seLogado);
        findViewById(R.id.divAlterarSenha).setVisibility(seLogado);
        findViewById(R.id.btnSair).setVisibility(seLogado);
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perfil_sair_titulo)
                .setMessage(R.string.perfil_sair_texto)
                .setNegativeButton(R.string.acao_cancelar, null)
                .setPositiveButton(R.string.perfil_sair, (d, w) -> {
                    contaRepository.sair();
                    atualizarSessao();
                })
                .show();
    }

    // ---------------------------------------------------------------- Fale com a imobiliária (D38)

    private void abrirFaleConosco() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(R.layout.sheet_fale_conosco);
        View btnLigar = sheet.findViewById(R.id.btnLigar);
        View btnEmail = sheet.findViewById(R.id.btnEnviarEmail);
        if (btnLigar != null) {
            btnLigar.setOnClickListener(v -> abrirIntent(new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + getString(R.string.fale_telefone).replaceAll("[^\\d+]", "")))));
        }
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> abrirIntent(new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + getString(R.string.fale_email)))
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fale_assunto_email))));
        }
        sheet.show();
    }

    private void abrirIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(findViewById(R.id.main), R.string.fale_sem_app, Snackbar.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- Modo demonstração (D26, 6.7)

    private void abrirModoDemonstracao() {
        View corpo = getLayoutInflater().inflate(R.layout.dialog_modo_demonstracao, null);
        MaterialSwitch swModo = corpo.findViewById(R.id.swModoDemonstracao);
        MaterialSwitch swOffline = corpo.findViewById(R.id.swSimularOffline);
        TextView txtUrl = corpo.findViewById(R.id.txtUrlApi);
        swModo.setChecked(Preferencias.modoDemonstracao(this));
        swOffline.setChecked(Preferencias.simularFalhaRede(this));
        txtUrl.setText(getString(R.string.demo_url_api, BuildConfig.API_URL));
        swOffline.setOnCheckedChangeListener((b, ligado) -> Preferencias.setSimularFalhaRede(this, ligado));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_titulo)
                .setView(corpo)
                .setNegativeButton(R.string.acao_fechar, null)
                .setPositiveButton(R.string.demo_alternar, (d, w) -> alternarModo(swModo.isChecked()))
                .show();
    }

    /** Antes de alternar, avisa quantos favoritos ainda não foram enviados à conta (6.7). */
    private void alternarModo(boolean ligado) {
        if (ligado == Preferencias.modoDemonstracao(this)) {
            return;
        }
        new FavoritoRepository(this).naoSincronizados(new Tarefas.Callback<Integer>() {
            @Override
            public void sucesso(Integer pendentes) {
                if (pendentes != null && pendentes > 0) {
                    new MaterialAlertDialogBuilder(PerfilActivity.this)
                            .setTitle(R.string.demo_titulo)
                            .setMessage(getString(R.string.demo_favoritos_pendentes, pendentes))
                            .setNegativeButton(R.string.acao_cancelar, null)
                            .setPositiveButton(R.string.demo_alternar, (d, w) -> aplicarModo(ligado))
                            .show();
                } else {
                    aplicarModo(ligado);
                }
            }

            @Override
            public void erro(ApiException e) {
                aplicarModo(ligado);
            }
        });
    }

    private void aplicarModo(boolean ligado) {
        contaRepository.alternarModoDemonstracao(ligado);
        atualizarSessao();
        Snackbar.make(findViewById(R.id.main), R.string.demo_aviso, Snackbar.LENGTH_SHORT).show();
    }
}
