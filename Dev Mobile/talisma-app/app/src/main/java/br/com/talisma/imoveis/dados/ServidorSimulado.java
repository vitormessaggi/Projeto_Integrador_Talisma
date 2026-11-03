package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "API falsa" do modo demonstração (D24, 6.7): lê {@code assets/fake} e grava nas tabelas
 * {@code sim_*} do {@link BancoLocal}, com atraso de ~600 ms e falha de rede simulável
 * ({@link Preferencias#simularFalhaRede}). Os métodos bloqueiam: chamar sempre via {@code Tarefas}.
 * <p>
 * T01: só leitura de imóveis. Lista com filtros/paginação, destaques, opções de filtro (T03),
 * contas e visitas (T05) e fotos (T07) são acrescentados nas tasks seguintes.
 */
public class ServidorSimulado {

    private static final String TAG = "ServidorSimulado";
    private static final String ARQUIVO_IMOVEIS = "fake/imoveis.json";
    private static final long ATRASO_MS = 600;

    private static List<Imovel> imoveisCarregados;

    private final Context context;

    public ServidorSimulado(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Todos os imóveis do seed (já aprovados — RN-001), na ordem do arquivo. */
    public List<Imovel> imoveis() throws ApiException {
        simularRede();
        return new ArrayList<>(carregarImoveis(context));
    }

    /** Imóvel pelo id; 404 se não existir. */
    public Imovel imovel(long id) throws ApiException {
        simularRede();
        for (Imovel i : carregarImoveis(context)) {
            if (i.id == id) {
                return i;
            }
        }
        throw new ApiException(404, null);
    }

    /** Atraso de rede e, se ligado no Modo demonstração, falha de conexão. */
    private void simularRede() throws ApiException {
        try {
            Thread.sleep(ATRASO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Preferencias.simularFalhaRede(context)) {
            throw ApiException.semConexao(null);
        }
    }

    private static synchronized List<Imovel> carregarImoveis(Context context) throws ApiException {
        if (imoveisCarregados == null) {
            try {
                JSONArray lista = new JSONArray(lerAsset(context, ARQUIVO_IMOVEIS));
                List<Imovel> imoveis = new ArrayList<>();
                for (int k = 0; k < lista.length(); k++) {
                    imoveis.add(Imovel.deJson(lista.getJSONObject(k)));
                }
                imoveisCarregados = Collections.unmodifiableList(imoveis);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Falha ao ler " + ARQUIVO_IMOVEIS, e);
                throw new ApiException(500, null, e);
            }
        }
        return imoveisCarregados;
    }

    private static String lerAsset(Context context, String caminho) throws IOException {
        try (InputStream in = context.getAssets().open(caminho);
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                saida.write(buffer, 0, lidos);
            }
            return saida.toString(StandardCharsets.UTF_8.name());
        }
    }
}
