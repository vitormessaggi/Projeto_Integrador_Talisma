package br.com.talisma.imoveis.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.talisma.imoveis.dados.Imovel;

/**
 * Localização do aparelho e distância até os imóveis ("Perto de mim" — D15, 2.5.4).
 * {@link LocationManager} nativo ({@code getLastKnownLocation} + {@code requestSingleUpdate}),
 * distância com {@link Location#distanceBetween} e, quando o imóvel vem sem lat/long,
 * {@link Geocoder} sobre o endereço. O pedido de permissão fica na tela (Activity Result API).
 */
public final class Localizacao {

    private static final String TAG = "Localizacao";
    /** Última localização conhecida mais nova que isso é usada sem pedir outra. */
    private static final long VALIDADE_MS = 2 * 60 * 1000;
    /** Espera máxima por uma localização nova. */
    private static final long ESPERA_MS = 15_000;

    /** Coordenadas obtidas pelo Geocoder, por id do imóvel (evita repetir a consulta). */
    private static final Map<Long, double[]> GEOCODIFICADOS = new HashMap<>();

    private Localizacao() {
    }

    /** Permissão de localização (precisa ou aproximada) concedida. */
    public static boolean temPermissao(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Localização atual, entregue em {@code callback.sucesso} na main thread: a última conhecida se
     * for recente; senão uma leitura nova (até 15 s). Entrega {@code null} sem permissão, com a
     * localização desligada ou sem resposta (nesse caso, a última conhecida, se houver).
     * {@code callback.erro} nunca é chamado.
     */
    @SuppressLint("MissingPermission") // conferida em temPermissao(); declarada no manifesto (2.1.3)
    @SuppressWarnings("deprecation") // requestSingleUpdate: API do plano (6.1) e disponível desde a API 24
    public static void obter(Context context, Tarefas.Callback<Location> callback) {
        if (!temPermissao(context)) {
            Tarefas.naPrincipal(() -> callback.sucesso(null));
            return;
        }
        LocationManager gerenciador = (LocationManager) context.getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (gerenciador == null) {
            Tarefas.naPrincipal(() -> callback.sucesso(null));
            return;
        }
        Location ultima = ultimaConhecida(gerenciador);
        if (ultima != null && System.currentTimeMillis() - ultima.getTime() < VALIDADE_MS) {
            Tarefas.naPrincipal(() -> callback.sucesso(ultima));
            return;
        }
        String provedor = provedorAtivo(context, gerenciador);
        if (provedor == null) {
            Tarefas.naPrincipal(() -> callback.sucesso(ultima));
            return;
        }

        Handler principal = new Handler(Looper.getMainLooper());
        boolean[] entregue = {false};
        LocationListener ouvinte = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!entregue[0]) {
                    entregue[0] = true;
                    principal.removeCallbacksAndMessages(null);
                    callback.sucesso(location);
                }
            }

            // Implementados porque antes da API 30 não têm implementação padrão.
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        try {
            gerenciador.requestSingleUpdate(provedor, ouvinte, Looper.getMainLooper());
        } catch (RuntimeException e) {
            Log.w(TAG, "Falha ao pedir localização: " + e.getClass().getSimpleName());
            Tarefas.naPrincipal(() -> callback.sucesso(ultima));
            return;
        }
        principal.postDelayed(() -> {
            if (!entregue[0]) {
                entregue[0] = true;
                gerenciador.removeUpdates(ouvinte);
                callback.sucesso(ultima);
            }
        }, ESPERA_MS);
    }

    /** Distância em km entre dois pontos ({@link Location#distanceBetween}). */
    public static float distanciaKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        float[] metros = new float[1];
        Location.distanceBetween(latitude1, longitude1, latitude2, longitude2, metros);
        return metros[0] / 1000f;
    }

    /**
     * Latitude e longitude do imóvel; sem elas, tenta o {@link Geocoder} sobre o endereço (D15).
     * {@code null} se não houver como localizar. Bloqueia: chamar fora da main thread.
     */
    @Nullable
    @SuppressWarnings("deprecation") // getFromLocationName síncrono: a versão com callback só existe a partir da API 33
    public static double[] coordenadas(Context context, Imovel imovel) {
        if (imovel.latitude != null && imovel.longitude != null) {
            return new double[]{imovel.latitude, imovel.longitude};
        }
        synchronized (GEOCODIFICADOS) {
            double[] salvo = GEOCODIFICADOS.get(imovel.id);
            if (salvo != null) {
                return salvo;
            }
        }
        String endereco = imovel.endereco != null && !imovel.endereco.isEmpty() ? imovel.endereco : imovel.cidade;
        if (endereco == null || endereco.isEmpty() || !Geocoder.isPresent()) {
            return null;
        }
        try {
            List<Address> enderecos = new Geocoder(context, Formatos.PT_BR).getFromLocationName(endereco, 1);
            if (enderecos == null || enderecos.isEmpty()) {
                return null;
            }
            double[] coordenadas = {enderecos.get(0).getLatitude(), enderecos.get(0).getLongitude()};
            synchronized (GEOCODIFICADOS) {
                GEOCODIFICADOS.put(imovel.id, coordenadas);
            }
            return coordenadas;
        } catch (IOException | IllegalArgumentException e) {
            Log.w(TAG, "Geocoder indisponível: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Preenche {@link Imovel#distanciaKm} a partir de {@code origem} e ordena a lista da mais perto
     * para a mais longe; imóveis sem coordenadas ficam no fim. Bloqueia (Geocoder): chamar fora da
     * main thread.
     */
    public static void ordenarPorDistancia(Context context, Location origem, List<Imovel> imoveis) {
        for (Imovel i : imoveis) {
            double[] ponto = coordenadas(context, i);
            i.distanciaKm = ponto == null ? null
                    : distanciaKm(origem.getLatitude(), origem.getLongitude(), ponto[0], ponto[1]);
        }
        Collections.sort(imoveis, (a, b) -> {
            if (a.distanciaKm == null || b.distanciaKm == null) {
                return a.distanciaKm == null ? (b.distanciaKm == null ? 0 : 1) : -1;
            }
            return Float.compare(a.distanciaKm, b.distanciaKm);
        });
    }

    @Nullable
    @SuppressLint("MissingPermission") // só chamado depois de temPermissao()
    private static Location ultimaConhecida(LocationManager gerenciador) {
        Location melhor = null;
        for (String provedor : gerenciador.getProviders(true)) {
            try {
                Location l = gerenciador.getLastKnownLocation(provedor);
                if (l != null && (melhor == null || l.getTime() > melhor.getTime())) {
                    melhor = l;
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Provedor sem última localização: " + provedor);
            }
        }
        return melhor;
    }

    /** GPS se ligado e com permissão precisa; senão rede; {@code null} se nenhum estiver ligado. */
    @Nullable
    private static String provedorAtivo(Context context, LocationManager gerenciador) {
        boolean precisa = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (precisa && gerenciador.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (gerenciador.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }
}
