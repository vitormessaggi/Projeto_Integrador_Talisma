package br.com.talisma.imoveis;

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
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.Sessao;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.dados.Visita;
import br.com.talisma.imoveis.dados.VisitaRepository;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Meus contatos com corretores (D37, 6.5): visitas do usuário com status e "Cancelar" (confirmação)
 * enquanto Pendente/Confirmada; toque abre o Detalhe; vazio "Você ainda não solicitou visitas".
 * Exige login (abre o Login com retorno); 401 idem.
 */
public class MeusContatosActivity extends AppCompatActivity implements ContatoAdapter.Ouvinte {

    private VisitaRepository visitaRepository;
    private ContatoAdapter adapter;
    private View estado;

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
        setContentView(R.layout.activity_meus_contatos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        visitaRepository = new VisitaRepository(this);
        ((TextView) findViewById(R.id.txtTituloTela)).setText(R.string.contatos_titulo);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        estado = findViewById(R.id.estadoTela);
        RecyclerView rvContatos = findViewById(R.id.rvContatos);
        rvContatos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContatoAdapter(this);
        rvContatos.setAdapter(adapter);
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        if (!Sessao.logado(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CLIENTE));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Sessao.logado(this)) {
            carregar();
        }
    }

    private void carregar() {
        if (adapter.getItemCount() == 0) {
            EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        }
        visitaRepository.minhas(new Tarefas.Callback<List<Visita>>() {
            @Override
            public void sucesso(List<Visita> visitas) {
                adapter.substituir(visitas);
                if (visitas.isEmpty()) {
                    EstadoTela.mostrar(estado, EstadoTela.Tipo.VAZIO, getString(R.string.contatos_vazio));
                    ((ImageView) estado.findViewById(R.id.imgEstado)).setImageResource(R.drawable.ic_calendario);
                } else {
                    EstadoTela.esconder(estado);
                }
            }

            @Override
            public void erro(ApiException e) {
                if (e.isSessaoExpirada()) {
                    login.launch(LoginActivity.intentSessaoExpirada(MeusContatosActivity.this, e));
                    return;
                }
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(MeusContatosActivity.this));
                ((ImageView) estado.findViewById(R.id.imgEstado)).setImageResource(R.drawable.ic_nuvem_off);
            }
        });
    }

    @Override
    public void aoTocar(Visita visita) {
        startActivity(DetalheImovelActivity.intent(this, visita.imovelId));
    }

    @Override
    public void aoCancelar(Visita visita, int posicao) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contatos_cancelar_titulo)
                .setMessage(getString(R.string.contatos_cancelar_texto, visita.imovelTitulo))
                .setNegativeButton(R.string.contatos_manter, null)
                .setPositiveButton(R.string.contatos_cancelar_confirmar, (d, w) -> cancelar(visita, posicao))
                .show();
    }

    private void cancelar(Visita visita, int posicao) {
        visitaRepository.cancelar(visita.id, new Tarefas.Callback<Visita>() {
            @Override
            public void sucesso(Visita atualizada) {
                if (posicao >= 0 && posicao < adapter.getItemCount()) {
                    adapter.atualizar(posicao, atualizada);
                } else {
                    carregar();
                }
                Snackbar.make(findViewById(R.id.main), R.string.contatos_cancelada, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void erro(ApiException e) {
                if (e.isSessaoExpirada()) {
                    login.launch(LoginActivity.intentSessaoExpirada(MeusContatosActivity.this, e));
                    return;
                }
                Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(MeusContatosActivity.this),
                        Snackbar.LENGTH_LONG).show();
                carregar();
            }
        });
    }
}
