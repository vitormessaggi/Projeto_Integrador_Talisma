package br.com.talisma.imoveis;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Solicitar visita — provisória da Fase 2.2 (só toolbar e título; recebe {@code imovel_id}).
 * A T06 completa com formulário, login com retorno (D9) e envio (D30).
 */
public class SolicitarVisitaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitar_visita);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnVoltar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }
}
