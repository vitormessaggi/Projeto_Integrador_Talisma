package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.talisma.imoveis.BuildConfig;
import br.com.talisma.imoveis.R;
import br.com.talisma.imoveis.util.CarregadorImagem;
import br.com.talisma.imoveis.util.Tarefas;

/**
 * Fotos do corretor (EAP 2.5.2, D3, D33): imóveis do corretor logado e envio de fotos com
 * progresso e reenvio. Exige sessão de CORRETOR; 401 chega como {@link ApiException#isSessaoExpirada()}.
 * Decide por {@link Preferencias#modoDemonstracao} entre o {@link ServidorSimulado} e a {@link Api}.
 * <p>
 * Antes de enviar, cada foto é reduzida para no máximo 1600 px e JPEG 80%
 * ({@link CarregadorImagem#comprimirParaEnvio}) num arquivo temporário em {@code cacheDir/fotos}.
 * A contagem "4 fotos" da lista é {@code imovel.fotos.size()}; as fotos já enviadas voltam em
 * {@link Imovel#fotos} (aqui e no {@link ImovelRepository#detalhe}).
 */
public class FotoRepository {

    private static final String TAG = "FotoRepository";
    /** Limite de fotos novas por envio (6.5). */
    public static final int MAX_FOTOS_POR_ENVIO = 10;
    /** Pasta em {@code cacheDir} das fotos da câmera e dos JPEG comprimidos (declarada no FileProvider). */
    private static final String PASTA_CACHE = "fotos";
    private static final String AUTORIDADE = BuildConfig.APPLICATION_ID + ".fileprovider";

    /** Mudança de estado/progresso de uma {@link FotoEnvio}; chamado na main thread. */
    public interface Andamento {
        void atualizado(FotoEnvio foto);
    }

    private final Context context;

    public FotoRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Imóveis do corretor logado (Selecionar imóvel); conta que não é de corretor → 403. */
    public void imoveisDoCorretor(Tarefas.Callback<List<Imovel>> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                return new ServidorSimulado(context).imoveisDoCorretor(Sessao.token(context));
            }
            JSONArray lista = Api.lista(new Api(context).get(ApiRotas.IMOVEIS_CORRETOR));
            List<Imovel> imoveis = new ArrayList<>();
            try {
                for (int k = 0; k < lista.length(); k++) {
                    imoveis.add(Imovel.deJson(lista.getJSONObject(k)));
                }
            } catch (JSONException e) {
                throw new ApiException(500, null, e);
            }
            return imoveis;
        }, callback);
    }

    /**
     * Uri ({@code content://} do FileProvider) de um arquivo novo em {@code cacheDir/fotos} para a
     * câmera gravar ({@code ActivityResultContracts.TakePicture}). {@code null} se não der para criar.
     */
    @Nullable
    public Uri novaFotoCamera() {
        File pasta = new File(context.getCacheDir(), PASTA_CACHE);
        if (!pasta.exists() && !pasta.mkdirs()) {
            Log.w(TAG, "Não foi possível criar a pasta da câmera");
            return null;
        }
        File arquivo = new File(pasta, "camera_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(context, AUTORIDADE, arquivo);
    }

    /**
     * Envia {@code foto} ao imóvel (também usado para reenviar uma foto {@link FotoEnvio#FALHOU}).
     * Atualiza {@code foto.estado}/{@code foto.progresso} e avisa {@code andamento} a cada mudança:
     * ENVIANDO 0–100 → ENVIADA, ou FALHOU (o erro também chega em {@code callback.erro}).
     * {@code callback.sucesso} recebe a URL da foto no servidor (caminho local no modo simulado).
     * Chamar na main thread.
     */
    public void enviar(long imovelId, FotoEnvio foto, Andamento andamento, Tarefas.Callback<String> callback) {
        foto.estado = FotoEnvio.ENVIANDO;
        foto.progresso = 0;
        andamento.atualizado(foto);
        Uri uri = foto.uri;
        Api.Progresso progresso = porcentagem -> Tarefas.naPrincipal(() -> {
            if (FotoEnvio.ENVIANDO.equals(foto.estado) && porcentagem > foto.progresso) {
                foto.progresso = porcentagem;
                andamento.atualizado(foto);
            }
        });
        Tarefas.executar(() -> {
            File jpeg = new File(new File(context.getCacheDir(), PASTA_CACHE), "envio_" + UUID.randomUUID() + ".jpg");
            try {
                if (!CarregadorImagem.comprimirParaEnvio(context, uri, jpeg)) {
                    throw new ApiException(400, context.getString(R.string.foto_erro_arquivo));
                }
                if (simulado()) {
                    return new ServidorSimulado(context).enviarFoto(Sessao.token(context), imovelId, jpeg, progresso);
                }
                String resposta = new Api(context).postMultipart(ApiRotas.fotosImovel(imovelId),
                        ApiRotas.CAMPO_FOTO, jpeg, ApiRotas.TIPO_FOTO, progresso);
                return Api.objeto(resposta).optString("url", null);
            } finally {
                if (jpeg.exists() && !jpeg.delete()) {
                    Log.d(TAG, "JPEG temporário não removido");
                }
            }
        }, new Tarefas.Callback<String>() {
            @Override
            public void sucesso(String url) {
                foto.estado = FotoEnvio.ENVIADA;
                foto.progresso = 100;
                andamento.atualizado(foto);
                callback.sucesso(url);
            }

            @Override
            public void erro(ApiException e) {
                foto.estado = FotoEnvio.FALHOU;
                andamento.atualizado(foto);
                callback.erro(e);
            }
        });
    }

    private boolean simulado() {
        return Preferencias.modoDemonstracao(context);
    }
}
