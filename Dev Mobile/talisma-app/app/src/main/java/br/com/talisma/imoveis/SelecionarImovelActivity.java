package br.com.talisma.imoveis;

import android.content.Context;
import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.ContaRepository;
import br.com.talisma.imoveis.dados.FotoRepository;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Selecionar imóvel (nova, D3, D33, 6.5, EAP 2.5.1): toolbar borgonha "Área do corretor" com Sair,
 * saudação "Olá, {nome}" e lista dos imóveis do corretor com a contagem de fotos; toque → FotosImovel.
 * Sem sessão de corretor abre o Login no modo CORRETOR (com retorno); 401 idem com "Sua sessão expirou".
 */
public class SelecionarImovelActivity extends AppCompatActivity {

    private FotoRepository fotoRepository;
    private ImovelCorretorAdapter adapter;
    private View estado;

    private final ActivityResultLauncher<Intent> login = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != RESULT_OK) {
                    finish();
                }
            });

    public static Intent intent(Context context) {
        return new Intent(context, SelecionarImovelActivity.class);
    }

    /** Sessão atual é de corretor (quem abre a área interna direto confere isso — 6.3). */
    public static boolean sessaoDeCorretor(Context context) {
        Usuario usuario = Sessao.usuario(context);
        return Sessao.logado(context) && usuario != null && usuario.isCorretor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selecionar_imovel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fotoRepository = new FotoRepository(this);
        ((TextView) findViewById(R.id.txtTituloTela)).setText(R.string.corretor_titulo);
        findViewById(R.id.btnVoltar).setVisibility(View.GONE);
        findViewById(R.id.btnSair).setOnClickListener(v -> confirmarSaida());
        estado = findViewById(R.id.estadoTela);
        RecyclerView rvImoveis = findViewById(R.id.rvImoveis);
        rvImoveis.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImovelCorretorAdapter(imovel -> startActivity(FotosImovelActivity.intent(this, imovel)));
        rvImoveis.setAdapter(adapter);
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        if (!sessaoDeCorretor(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CORRETOR)
                    .putExtra(LoginActivity.EXTRA_ABRIR_AREA_CORRETOR, false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessaoDeCorretor(this)) {
            Usuario usuario = Sessao.usuario(this);
            ((TextView) findViewById(R.id.txtSaudacao)).setText(getString(R.string.corretor_ola,
                    usuario != null && usuario.nome != null ? usuario.nome : ""));
            carregar();
        }
    }

    private void carregar() {
        if (adapter.getItemCount() == 0) {
            EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        }
        fotoRepository.imoveisDoCorretor(new Tarefas.Callback<List<Imovel>>() {
            @Override
            public void sucesso(List<Imovel> imoveis) {
                adapter.substituir(imoveis);
                if (imoveis.isEmpty()) {
                    EstadoTela.mostrar(estado, EstadoTela.Tipo.VAZIO, getString(R.string.corretor_sem_imoveis));
                    ((ImageView) estado.findViewById(R.id.imgEstado)).setImageResource(R.drawable.ic_marcador_casa);
                } else {
                    EstadoTela.esconder(estado);
                }
            }

            @Override
            public void erro(ApiException e) {
                if (e.isSessaoExpirada()) {
                    login.launch(LoginActivity.intent(SelecionarImovelActivity.this, Usuario.CORRETOR)
                            .putExtra(LoginActivity.EXTRA_ABRIR_AREA_CORRETOR, false)
                            .putExtra(LoginActivity.EXTRA_MENSAGEM, e.mensagemParaUsuario(SelecionarImovelActivity.this)));
                    return;
                }
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(SelecionarImovelActivity.this));
                ((ImageView) estado.findViewById(R.id.imgEstado)).setImageResource(R.drawable.ic_nuvem_off);
            }
        });
    }

    /** Sair no topo (D33): confirmação, limpa a sessão (D35) e fecha a área do corretor. */
    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.corretor_sair_titulo)
                .setNegativeButton(R.string.acao_cancelar, null)
                .setPositiveButton(R.string.corretor_sair, (d, w) -> {
                    new ContaRepository(this).sair();
                    finish();
                })
                .show();
    }
}
