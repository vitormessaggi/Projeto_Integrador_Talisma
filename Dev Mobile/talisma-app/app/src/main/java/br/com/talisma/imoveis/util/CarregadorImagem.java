package br.com.talisma.imoveis.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Carregamento de imagens sem biblioteca (D8): decodifica em segundo plano com {@code inSampleSize}
 * pelo tamanho do {@link ImageView}, guarda em {@link LruCache} (1/8 da memória) e, para URLs,
 * em disco ({@code cacheDir/imagens}, até 50 MB — D36).
 * <p>
 * Origem da foto, conforme o texto:
 * <ul>
 *   <li>{@code http://} ou {@code https://} → baixa (com cache em disco);</li>
 *   <li>caminho absoluto ({@code /...}) → arquivo local (fotos enviadas no modo simulado);</li>
 *   <li>{@code content://} → Uri da câmera (FileProvider) ou da galeria (fotos novas do corretor);</li>
 *   <li>qualquer outro → nome de arquivo em {@code assets/fake/fotos/}.</li>
 * </ul>
 * Envio de fotos (2.5.2, 6.5): {@link #comprimirParaEnvio} reduz a foto escolhida/tirada para no
 * máximo {@value #LADO_ENVIO} px no maior lado e grava JPEG {@value #QUALIDADE_ENVIO}%.
 */
public final class CarregadorImagem {

    private static final String TAG = "CarregadorImagem";
    private static final String PASTA_ASSETS = "fake/fotos/";
    private static final String PASTA_DISCO = "imagens";
    private static final long LIMITE_DISCO = 50L * 1024 * 1024;
    private static final int LADO_PADRAO = 1080;
    private static final int TIMEOUT_MS = 15_000;
    /** Maior lado da foto enviada pelo corretor (6.5). */
    public static final int LADO_ENVIO = 1600;
    /** Qualidade JPEG da foto enviada (6.5). */
    public static final int QUALIDADE_ENVIO = 80;

    private static final LruCache<String, Bitmap> MEMORIA =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String chave, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };

    private CarregadorImagem() {
    }

    /**
     * Mostra {@code foto} em {@code imageView}. Enquanto carrega (ou se falhar) a imagem fica vazia,
     * deixando aparecer o fundo do layout ({@code placeholder_foto}). Seguro para itens reciclados
     * de RecyclerView: o resultado só é aplicado se a view ainda pedir a mesma foto.
     */
    public static void carregar(ImageView imageView, @Nullable String foto) {
        imageView.setTag(foto);
        if (foto == null || foto.isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }
        int largura = imageView.getWidth() > 0 ? imageView.getWidth() : LADO_PADRAO;
        int altura = imageView.getHeight() > 0 ? imageView.getHeight() : LADO_PADRAO;
        String chave = foto + "@" + largura + "x" + altura;
        Bitmap emMemoria = MEMORIA.get(chave);
        if (emMemoria != null) {
            imageView.setImageBitmap(emMemoria);
            return;
        }
        imageView.setImageDrawable(null);
        Context context = imageView.getContext().getApplicationContext();
        Tarefas.emSegundoPlano(() -> {
            Bitmap bitmap = decodificar(context, foto, largura, altura);
            if (bitmap != null) {
                MEMORIA.put(chave, bitmap);
            }
            Tarefas.naPrincipal(() -> {
                if (foto.equals(imageView.getTag()) && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    /** Decodifica a foto já reduzida para caber em {@code largura} × {@code altura}. Rodar fora da main thread. */
    @Nullable
    public static Bitmap decodificar(Context context, String foto, int largura, int altura) {
        try {
            if (ehUrl(foto)) {
                File arquivo = baixar(context, foto);
                return arquivo != null ? decodificarArquivo(arquivo, largura, altura) : null;
            }
            if (foto.startsWith("/")) {
                return decodificarArquivo(new File(foto), largura, altura);
            }
            if (foto.startsWith("content://")) {
                return decodificarUri(context, Uri.parse(foto), largura, altura);
            }
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            opcoes.inJustDecodeBounds = true;
            try (InputStream in = context.getAssets().open(PASTA_ASSETS + foto)) {
                BitmapFactory.decodeStream(in, null, opcoes);
            }
            opcoes.inSampleSize = calcularAmostra(opcoes, largura, altura);
            opcoes.inJustDecodeBounds = false;
            try (InputStream in = context.getAssets().open(PASTA_ASSETS + foto)) {
                return BitmapFactory.decodeStream(in, null, opcoes);
            }
        } catch (IOException e) {
            Log.w(TAG, "Falha ao carregar imagem: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Lê a foto de {@code uri} (câmera via FileProvider ou galeria), corrige a rotação do EXIF, reduz
     * para no máximo {@link #LADO_ENVIO} px no maior lado e grava em {@code destino} como JPEG
     * {@link #QUALIDADE_ENVIO}%. Rodar fora da main thread.
     *
     * @return {@code false} se a foto não pôde ser lida ou gravada
     */
    public static boolean comprimirParaEnvio(Context context, Uri uri, File destino) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            opcoes.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return false;
                }
                BitmapFactory.decodeStream(in, null, opcoes);
            }
            if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) {
                return false;
            }
            // inSampleSize mantém o maior lado ≥ LADO_ENVIO; o ajuste fino vem na Matrix abaixo.
            int amostra = 1;
            int maiorLado = Math.max(opcoes.outWidth, opcoes.outHeight);
            while (maiorLado / (amostra * 2) >= LADO_ENVIO) {
                amostra *= 2;
            }
            opcoes.inJustDecodeBounds = false;
            opcoes.inSampleSize = amostra;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                bitmap = in != null ? BitmapFactory.decodeStream(in, null, opcoes) : null;
            }
            if (bitmap == null) {
                return false;
            }
            Matrix matriz = new Matrix();
            float escala = Math.min(1f, (float) LADO_ENVIO / Math.max(bitmap.getWidth(), bitmap.getHeight()));
            matriz.postScale(escala, escala);
            matriz.postRotate(rotacaoExif(context, uri));
            Bitmap ajustado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true);
            if (ajustado != bitmap) {
                bitmap.recycle();
                bitmap = ajustado;
            }
            File pasta = destino.getParentFile();
            if (pasta != null && !pasta.exists() && !pasta.mkdirs()) {
                return false;
            }
            try (OutputStream out = new FileOutputStream(destino)) {
                return bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_ENVIO, out);
            }
        } catch (IOException | SecurityException | OutOfMemoryError e) {
            Log.w(TAG, "Falha ao comprimir foto: " + e.getClass().getSimpleName());
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    /**
     * Graus de rotação indicados no EXIF da foto (0 se não houver). Usa o ExifInterface da plataforma
     * (estável a partir da API 24 = minSdk), já que o plano não admite a biblioteca androidx (6.1).
     */
    @SuppressLint("ExifInterface")
    private static int rotacaoExif(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return 0;
            }
            int orientacao = new android.media.ExifInterface(in).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            switch (orientacao) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    /** Miniatura de uma Uri {@code content://} (fotos novas do corretor, antes do envio). */
    @Nullable
    private static Bitmap decodificarUri(Context context, Uri uri, int largura, int altura) throws IOException {
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            BitmapFactory.decodeStream(in, null, opcoes);
        }
        opcoes.inSampleSize = calcularAmostra(opcoes, largura, altura);
        opcoes.inJustDecodeBounds = false;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return in != null ? BitmapFactory.decodeStream(in, null, opcoes) : null;
        }
    }

    private static boolean ehUrl(String foto) {
        return foto.startsWith("http://") || foto.startsWith("https://");
    }

    @Nullable
    private static Bitmap decodificarArquivo(File arquivo, int largura, int altura) {
        if (!arquivo.exists()) {
            return null;
        }
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(arquivo.getAbsolutePath(), opcoes);
        opcoes.inSampleSize = calcularAmostra(opcoes, largura, altura);
        opcoes.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(arquivo.getAbsolutePath(), opcoes);
    }

    static int calcularAmostra(BitmapFactory.Options opcoes, int largura, int altura) {
        int amostra = 1;
        int alturaOriginal = opcoes.outHeight;
        int larguraOriginal = opcoes.outWidth;
        while (alturaOriginal / (amostra * 2) >= altura && larguraOriginal / (amostra * 2) >= largura) {
            amostra *= 2;
        }
        return amostra;
    }

    /** Baixa a URL para o cache em disco (ou devolve o arquivo já salvo). */
    @Nullable
    private static File baixar(Context context, String url) throws IOException {
        File pasta = new File(context.getCacheDir(), PASTA_DISCO);
        if (!pasta.exists() && !pasta.mkdirs()) {
            return null;
        }
        File arquivo = new File(pasta, Integer.toHexString(url.hashCode()) + "_" + url.length());
        if (arquivo.exists()) {
            // Marca como usado recentemente para a limpeza por idade.
            if (!arquivo.setLastModified(System.currentTimeMillis())) {
                Log.d(TAG, "Não foi possível atualizar a data do cache");
            }
            return arquivo;
        }
        HttpURLConnection conexao = (HttpURLConnection) new URL(url).openConnection();
        conexao.setConnectTimeout(TIMEOUT_MS);
        conexao.setReadTimeout(TIMEOUT_MS);
        File temporario = new File(pasta, arquivo.getName() + ".tmp");
        try {
            if (conexao.getResponseCode() >= 400) {
                return null;
            }
            try (InputStream in = conexao.getInputStream(); OutputStream out = new FileOutputStream(temporario)) {
                byte[] buffer = new byte[8192];
                int lidos;
                while ((lidos = in.read(buffer)) != -1) {
                    out.write(buffer, 0, lidos);
                }
            }
            if (!temporario.renameTo(arquivo)) {
                return null;
            }
        } finally {
            conexao.disconnect();
            if (temporario.exists() && !temporario.delete()) {
                Log.d(TAG, "Temporário não removido");
            }
        }
        limitarDisco(pasta);
        return arquivo;
    }

    /** Apaga os arquivos mais antigos até o cache em disco ficar abaixo de 50 MB. */
    private static synchronized void limitarDisco(File pasta) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null) {
            return;
        }
        long total = 0;
        for (File f : arquivos) {
            total += f.length();
        }
        if (total <= LIMITE_DISCO) {
            return;
        }
        Arrays.sort(arquivos, Comparator.comparingLong(File::lastModified));
        for (File f : arquivos) {
            if (total <= LIMITE_DISCO) {
                break;
            }
            long tamanho = f.length();
            if (f.delete()) {
                total -= tamanho;
            }
        }
    }
}
