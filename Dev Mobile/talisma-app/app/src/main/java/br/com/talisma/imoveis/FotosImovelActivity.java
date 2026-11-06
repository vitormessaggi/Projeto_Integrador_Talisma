package br.com.talisma.imoveis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.dados.FotoEnvio;
import br.com.talisma.imoveis.dados.FotoRepository;
import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.dados.ImovelRepository;
import br.com.talisma.imoveis.dados.Usuario;
import br.com.talisma.imoveis.util.EstadoTela;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Fotos do imóvel (nova, D3, D33, 6.5, EAP 2.5.1/2.5.2): "Fotos atuais" (grade, só leitura — toque abre a
 * Galeria) e "Novas fotos" (grade com lixeira e estado aguardando / enviando com progresso / enviada /
 * falhou — toque reenvia); Câmera ({@code TakePicture} + FileProvider, permissão pedida ao tocar) e Galeria
 * ({@code PickVisualMedia}); até {@value FotoRepository#MAX_FOTOS_POR_ENVIO} por envio; "Enviar N fotos"
 * envia uma por vez. As fotos pendentes sobrevivem à rotação (D20). Extras {@code imovel_id} e {@code titulo}.
 */
public class FotosImovelActivity extends AppCompatActivity implements FotoNovaAdapter.Ouvinte {

    private static final String TAG = "FotosImovelActivity";
    public static final String EXTRA_TITULO = "titulo";
    private static final String ESTADO_NOVAS = "novas";
    private static final String ESTADO_URI_CAMERA = "uriCamera";

    private FotoRepository fotoRepository;
    private ImovelRepository imovelRepository;
    private long imovelId;
    private Imovel imovel;
    private final List<FotoEnvio> novas = new ArrayList<>();
    private Uri uriCamera;
    private boolean enviando;

    private View estado;
    private FotoAdapter atuais;
    private FotoNovaAdapter adapterNovas;
    private TextView txtAtuaisVazio;
    private TextView txtNovasVazio;
    private Button btnCamera;
    private Button btnGaleria;
    private Button btnEnviarFotos;

    private final ActivityResultLauncher<Intent> login = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != RESULT_OK) {
                    finish();
                }
            });

    private final ActivityResultLauncher<String> permissaoCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    abrirCamera();
                } else {
                    cameraNegada();
                }
            });

    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), tirou -> {
                if (tirou && uriCamera != null) {
                    adicionar(Collections.singletonList(uriCamera));
                }
                uriCamera = null;
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galeria = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(FotoRepository.MAX_FOTOS_POR_ENVIO),
            this::adicionar);

    public static Intent intent(Context context, Imovel imovel) {
        return new Intent(context, FotosImovelActivity.class)
                .putExtra(DetalheImovelActivity.EXTRA_IMOVEL_ID, imovel.id)
                .putExtra(EXTRA_TITULO, imovel.titulo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fotos_imovel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fotoRepository = new FotoRepository(this);
        imovelRepository = new ImovelRepository(this);
        imovelId = getIntent().getLongExtra(DetalheImovelActivity.EXTRA_IMOVEL_ID, 0);
        restaurar(savedInstanceState);

        ((TextView) findViewById(R.id.txtTituloTela)).setText(getIntent().getStringExtra(EXTRA_TITULO));
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnSair).setVisibility(View.GONE);
        estado = findViewById(R.id.estadoTela);
        txtAtuaisVazio = findViewById(R.id.txtAtuaisVazio);
        txtNovasVazio = findViewById(R.id.txtNovasVazio);
        btnCamera = findViewById(R.id.btnCamera);
        btnGaleria = findViewById(R.id.btnGaleria);
        btnEnviarFotos = findViewById(R.id.btnEnviarFotos);

        int colunas = getResources().getInteger(R.integer.colunas_fotos);
        RecyclerView rvAtuais = findViewById(R.id.rvAtuais);
        rvAtuais.setLayoutManager(new GridLayoutManager(this, colunas));
        atuais = new FotoAdapter(FotoAdapter.Modo.GRADE, this::abrirGaleria);
        rvAtuais.setAdapter(atuais);
        RecyclerView rvNovas = findViewById(R.id.rvNovas);
        rvNovas.setLayoutManager(new GridLayoutManager(this, colunas));
        adapterNovas = new FotoNovaAdapter(novas, this);
        rvNovas.setAdapter(adapterNovas);

        btnCamera.setOnClickListener(v -> pedirCamera());
        btnGaleria.setOnClickListener(v -> abrirGaleriaDoAparelho());
        btnEnviarFotos.setOnClickListener(v -> enviarPendentes());
        EstadoTela.aoTentarNovamente(estado, this::carregar);

        atualizarNovas();
        if (!SelecionarImovelActivity.sessaoDeCorretor(this)) {
            login.launch(LoginActivity.intent(this, Usuario.CORRETOR)
                    .putExtra(LoginActivity.EXTRA_ABRIR_AREA_CORRETOR, false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (imovel == null && SelecionarImovelActivity.sessaoDeCorretor(this)) {
            carregar();
        }
    }

    // ---------------------------------------------------------------- rotação (D20)

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        JSONArray lista = new JSONArray();
        for (FotoEnvio f : novas) {
            lista.put(f.paraJson());
        }
        outState.putString(ESTADO_NOVAS, lista.toString());
        outState.putParcelable(ESTADO_URI_CAMERA, uriCamera);
    }

    private void restaurar(Bundle salvo) {
        if (salvo == null) {
            return;
        }
        uriCamera = BundleCompat.getParcelable(salvo, ESTADO_URI_CAMERA, Uri.class);
        String json = salvo.getString(ESTADO_NOVAS);
        if (json == null) {
            return;
        }
        try {
            JSONArray lista = new JSONArray(json);
            for (int k = 0; k < lista.length(); k++) {
                FotoEnvio f = FotoEnvio.deJson(lista.getJSONObject(k));
                // Um envio em andamento morreu com a tela antiga: fica como falhou para o corretor reenviar
                if (FotoEnvio.ENVIANDO.equals(f.estado)) {
                    f.estado = FotoEnvio.FALHOU;
                    f.progresso = 0;
                }
                novas.add(f);
            }
        } catch (JSONException e) {
            Log.w(TAG, "Fotos pendentes ilegíveis; descartadas");
            novas.clear();
        }
    }

    // ---------------------------------------------------------------- fotos atuais

    private void carregar() {
        if (imovel == null) {
            EstadoTela.mostrar(estado, EstadoTela.Tipo.CARREGANDO, null);
        }
        imovelRepository.detalhe(imovelId, new Tarefas.Callback<Imovel>() {
            @Override
            public void sucesso(Imovel dado) {
                imovel = dado;
                atuais.substituir(imovel.fotos);
                txtAtuaisVazio.setVisibility(imovel.fotos.isEmpty() ? View.VISIBLE : View.GONE);
                EstadoTela.esconder(estado);
            }

            @Override
            public void erro(ApiException e) {
                if (imovel != null) {
                    Snackbar.make(findViewById(R.id.main), e.mensagemParaUsuario(FotosImovelActivity.this),
                            Snackbar.LENGTH_LONG).setAnchorView(R.id.btnEnviarFotos).show();
                    return;
                }
                EstadoTela.mostrar(estado, e.isSemConexao() ? EstadoTela.Tipo.OFFLINE : EstadoTela.Tipo.ERRO,
                        e.mensagemParaUsuario(FotosImovelActivity.this));
            }
        });
    }

    private void abrirGaleria(int posicao) {
        if (imovel == null || imovel.fotos.isEmpty()) {
            return;
        }
        startActivity(new Intent(this, GaleriaActivity.class)
                .putExtra(DetalheImovelActivity.EXTRA_IMOVEL_ID, imovelId)
                .putExtra(GaleriaActivity.EXTRA_POSICAO, posicao)
                .putStringArrayListExtra(GaleriaActivity.EXTRA_FOTOS, new ArrayList<>(imovel.fotos)));
    }

    // ---------------------------------------------------------------- câmera e galeria

    /** Permissão de câmera pedida ao tocar btnCamera (6.6); negada → explicação + atalho para configurações. */
    private void pedirCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.fotos_camera)
                    .setMessage(R.string.fotos_camera_explicacao)
                    .setNegativeButton(R.string.acao_cancelar, null)
                    .setPositiveButton(R.string.localizacao_continuar, (d, w) ->
                            permissaoCamera.launch(Manifest.permission.CAMERA))
                    .show();
        } else {
            permissaoCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamera() {
        if (!temEspaco()) {
            return;
        }
        uriCamera = fotoRepository.novaFotoCamera();
        if (uriCamera == null) {
            aviso(getString(R.string.fotos_camera_indisponivel));
            return;
        }
        try {
            camera.launch(uriCamera);
        } catch (RuntimeException e) {
            Log.w(TAG, "Sem app de câmera: " + e.getClass().getSimpleName());
            uriCamera = null;
            aviso(getString(R.string.fotos_camera_indisponivel));
        }
    }

    private void cameraNegada() {
        Snackbar.make(findViewById(R.id.main), R.string.fotos_camera_negada, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.btnEnviarFotos)
                .setAction(R.string.localizacao_configuracoes, v -> startActivity(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null))))
                .show();
    }

    private void abrirGaleriaDoAparelho() {
        if (!temEspaco()) {
            return;
        }
        galeria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /** Ainda cabe foto nova neste envio (até 10 — 6.5)? Avisa se não. */
    private boolean temEspaco() {
        if (pendentes() + enviadas() >= FotoRepository.MAX_FOTOS_POR_ENVIO) {
            aviso(getString(R.string.fotos_limite, FotoRepository.MAX_FOTOS_POR_ENVIO));
            return false;
        }
        return true;
    }

    private void adicionar(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return;
        }
        boolean estourou = false;
        for (Uri uri : uris) {
            if (novas.size() >= FotoRepository.MAX_FOTOS_POR_ENVIO) {
                estourou = true;
                break;
            }
            novas.add(new FotoEnvio(uri));
            adapterNovas.notifyItemInserted(novas.size() - 1);
        }
        if (estourou) {
            aviso(getString(R.string.fotos_limite, FotoRepository.MAX_FOTOS_POR_ENVIO));
        }
        atualizarNovas();
    }

    @Override
    public void aoRemover(int posicao) {
        novas.remove(posicao);
        adapterNovas.notifyItemRemoved(posicao);
        atualizarNovas();
    }

    @Override
    public void aoReenviar(int posicao) {
        if (enviando) {
            return;
        }
        enviando = true;
        atualizarNovas();
        enviar(novas.get(posicao), this::terminarEnvio);
    }

    // ---------------------------------------------------------------- envio (2.5.2)

    /** "Enviar N fotos": uma por vez, na ordem, para as que estão AGUARDANDO ou FALHOU. */
    private void enviarPendentes() {
        if (enviando || pendentes() == 0) {
            return;
        }
        enviando = true;
        atualizarNovas();
        enviarProxima();
    }

    private void enviarProxima() {
        for (FotoEnvio f : novas) {
            if (FotoEnvio.AGUARDANDO.equals(f.estado) || FotoEnvio.FALHOU.equals(f.estado)) {
                enviar(f, this::enviarProxima);
                return;
            }
        }
        terminarEnvio();
    }

    private void enviar(FotoEnvio foto, Runnable depois) {
        fotoRepository.enviar(imovelId, foto, this::atualizarFoto, new Tarefas.Callback<String>() {
            @Override
            public void sucesso(String url) {
                depois.run();
            }

            @Override
            public void erro(ApiException e) {
                if (e.isSessaoExpirada()) {
                    enviando = false;
                    atualizarNovas();
                    login.launch(LoginActivity.intent(FotosImovelActivity.this, Usuario.CORRETOR)
                            .putExtra(LoginActivity.EXTRA_ABRIR_AREA_CORRETOR, false)
                            .putExtra(LoginActivity.EXTRA_MENSAGEM, e.mensagemParaUsuario(FotosImovelActivity.this)));
                    return;
                }
                if (e.isSemConexao()) {
                    // Sem rede não adianta seguir com as outras: para aqui e mostra o motivo
                    aviso(e.mensagemParaUsuario(FotosImovelActivity.this));
                    terminarEnvio();
                    return;
                }
                depois.run();
            }
        });
    }

    private void atualizarFoto(FotoEnvio foto) {
        int posicao = novas.indexOf(foto);
        if (posicao >= 0) {
            adapterNovas.notifyItemChanged(posicao);
        }
    }

    /** Fim da fila: tira da grade as enviadas, recarrega "Fotos atuais" e resume o resultado. */
    private void terminarEnvio() {
        enviando = false;
        int enviadas = enviadas();
        int falharam = 0;
        for (int k = novas.size() - 1; k >= 0; k--) {
            if (FotoEnvio.ENVIADA.equals(novas.get(k).estado)) {
                novas.remove(k);
                adapterNovas.notifyItemRemoved(k);
            } else if (FotoEnvio.FALHOU.equals(novas.get(k).estado)) {
                falharam++;
            }
        }
        atualizarNovas();
        if (enviadas > 0) {
            carregar();
            aviso(getResources().getQuantityString(R.plurals.fotos_enviadas, enviadas, enviadas));
        } else if (falharam > 0) {
            aviso(getResources().getQuantityString(R.plurals.fotos_falharam, falharam, falharam));
        }
    }

    private int pendentes() {
        int n = 0;
        for (FotoEnvio f : novas) {
            if (FotoEnvio.AGUARDANDO.equals(f.estado) || FotoEnvio.FALHOU.equals(f.estado)) {
                n++;
            }
        }
        return n;
    }

    private int enviadas() {
        int n = 0;
        for (FotoEnvio f : novas) {
            if (FotoEnvio.ENVIADA.equals(f.estado)) {
                n++;
            }
        }
        return n;
    }

    /** Texto do botão "Enviar N fotos", habilitação dos botões e legenda da seção vazia. */
    private void atualizarNovas() {
        int pendentes = pendentes();
        txtNovasVazio.setVisibility(novas.isEmpty() ? View.VISIBLE : View.GONE);
        txtNovasVazio.setText(getString(R.string.fotos_novas_vazio, FotoRepository.MAX_FOTOS_POR_ENVIO));
        btnEnviarFotos.setText(pendentes == 0 ? getString(R.string.fotos_enviar_vazio)
                : getResources().getQuantityString(R.plurals.fotos_enviar, pendentes, pendentes));
        btnEnviarFotos.setEnabled(pendentes > 0 && !enviando);
        btnCamera.setEnabled(!enviando);
        btnGaleria.setEnabled(!enviando);
    }

    private void aviso(String mensagem) {
        Snackbar.make(findViewById(R.id.main), mensagem, Snackbar.LENGTH_LONG).setAnchorView(R.id.btnEnviarFotos).show();
    }
}
