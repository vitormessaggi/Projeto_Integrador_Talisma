package br.com.talisma.imoveis;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.com.talisma.imoveis.util.BarraNavegacao;

/** Perfil (6.5, D31). T02: topo final e navegação; a T06 liga sessão, itens, Sair e Modo demonstração. */
public class PerfilActivity extends AppCompatActivity {

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

        // Conteúdo provisório: título da aba no lugar do nome e versão no rodapé.
        TextView txtNome = findViewById(R.id.txtNome);
        txtNome.setText(R.string.perfil_titulo);
        TextView txtVersao = findViewById(R.id.txtVersao);
        txtVersao.setText(getString(R.string.versao_app, BuildConfig.VERSION_NAME));
    }
}
