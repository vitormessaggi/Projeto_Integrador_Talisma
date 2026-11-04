package br.com.talisma.imoveis.util;

import android.content.Intent;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.talisma.imoveis.FavoritosActivity;
import br.com.talisma.imoveis.HomeActivity;
import br.com.talisma.imoveis.ImoveisActivity;
import br.com.talisma.imoveis.PerfilActivity;
import br.com.talisma.imoveis.R;

/**
 * Bottom nav das 4 telas principais (6.3): {@code BarraNavegacao.configurar(this, R.id.nav_x)} marca a aba
 * atual e abre a outra Activity com {@code FLAG_ACTIVITY_REORDER_TO_FRONT} e sem animação.
 * Voltar na Home sai do app; nas outras abas volta para a Home.
 */
public final class BarraNavegacao {

    private BarraNavegacao() {
    }

    public static void configurar(AppCompatActivity activity, @IdRes int itemAtual) {
        BottomNavigationView barra = activity.findViewById(R.id.navInferior);
        // O inset da barra do sistema já vira padding de R.id.main (D7); sem isto a barra o aplicaria de novo
        // e os ícones sobreporiam os rótulos.
        ViewCompat.setOnApplyWindowInsetsListener(barra, (v, insets) -> insets);
        barra.setSelectedItemId(itemAtual);
        barra.setOnItemSelectedListener(item -> {
            if (item.getItemId() != itemAtual) {
                abrir(activity, item.getItemId());
                // false: esta tela continua marcando a própria aba (ao voltar a ela com
                // REORDER_TO_FRONT o onCreate não roda de novo e a seleção ficaria errada)
                return false;
            }
            return true;
        });
        // Voltar: a Home é a raiz do app (sai); as outras abas voltam para a Home.
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (itemAtual == R.id.nav_inicio) {
                    activity.finishAffinity();
                } else {
                    abrir(activity, R.id.nav_inicio);
                }
            }
        });
    }

    private static void abrir(AppCompatActivity activity, @IdRes int item) {
        Class<?> destino;
        if (item == R.id.nav_imoveis) {
            destino = ImoveisActivity.class;
        } else if (item == R.id.nav_favoritos) {
            destino = FavoritosActivity.class;
        } else if (item == R.id.nav_perfil) {
            destino = PerfilActivity.class;
        } else {
            destino = HomeActivity.class;
        }
        Intent intent = new Intent(activity, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }
}
