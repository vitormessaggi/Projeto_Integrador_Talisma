package br.com.talisma.imoveis;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.talisma.imoveis.dados.Api;
import br.com.talisma.imoveis.dados.Filtro;
import br.com.talisma.imoveis.dados.Sessao;

public class LiaActivity extends AppCompatActivity {
    private LiaConversa conversa;
    private EditText pergunta;
    private CheckBox optIn;
    private LinearLayout mensagens;
    private ScrollView rolagem;
    private TextView estado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lia);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            var margem = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(margem.left, margem.top, margem.right, margem.bottom);
            return insets;
        });

        MaterialToolbar barra = findViewById(R.id.barraLia);
        barra.setNavigationOnClickListener(v -> finish());
        conversa = new ViewModelProvider(this).get(LiaConversa.class);
        pergunta = findViewById(R.id.perguntaLia);
        pergunta.setText(conversa.rascunho);
        pergunta.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                conversa.rascunho = s.toString();
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        optIn = findViewById(R.id.optInLia);
        mensagens = findViewById(R.id.conversaLia);
        rolagem = findViewById(R.id.rolagemLia);
        estado = findViewById(R.id.estadoLia);
        View enviar = findViewById(R.id.enviarLia);
        enviar.setOnClickListener(v -> enviar());
        pergunta.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            enviar();
            return true;
        });
        atalho(R.id.atalhoLiaCompra, R.string.lia_compra);
        atalho(R.id.atalhoLiaLocacao, R.string.lia_locacao);
        atalho(R.id.atalhoLiaVisita, R.string.lia_visita);
        ChipGroup atalhos = findViewById(R.id.atalhosLia);
        conversa.atualizacao.observe(this, ignorado -> {
            enviar.setEnabled(!conversa.enviando);
            pergunta.setEnabled(!conversa.enviando);
            optIn.setEnabled(!conversa.enviando);
            for (int i = 0; i < atalhos.getChildCount(); i++) {
                atalhos.getChildAt(i).setEnabled(!conversa.enviando);
            }
            if (!pergunta.getText().toString().equals(conversa.rascunho)) pergunta.setText(conversa.rascunho);
            estado.setVisibility(conversa.estado == 0 ? View.GONE : View.VISIBLE);
            if (conversa.estado != 0) estado.setText(conversa.estado);
            mostrarMensagens();
        });
    }

    private void atalho(int id, @StringRes int sugestao) {
        findViewById(id).setOnClickListener(v -> {
            pergunta.setText(sugestao);
            enviar();
        });
    }

    private void enviar() {
        String texto = conversa.rascunho.trim();
        if (texto.isEmpty() || conversa.enviando) return;
        boolean usarIa = optIn.isChecked();
        optIn.setChecked(false);
        var controle = ViewCompat.getWindowInsetsController(pergunta);
        if (controle != null) controle.hide(WindowInsetsCompat.Type.ime());
        conversa.enviar(new Api(getApplicationContext()), texto, usarIa);
    }

    private void mostrarMensagens() {
        mensagens.removeAllViews();
        int espaco = getResources().getDimensionPixelSize(R.dimen.espaco_8);
        for (LiaConversa.Mensagem mensagem : conversa.mensagens) {
            TextView texto = new TextView(this);
            texto.setTextAppearance(R.style.TextAppearance_Talisma_BodyLarge);
            texto.setPadding(espaco, espaco, espaco, espaco);
            texto.setBackgroundColor(getColor(mensagem.propria ? R.color.superficie : R.color.branco));
            texto.setText(getString(R.string.lia_resposta,
                    getString(mensagem.propria ? R.string.lia_voce : R.string.lia_nome), mensagem.texto));
            mensagens.addView(texto);

            JSONArray links = mensagem.links;
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.optJSONObject(i);
                if (link != null) adicionarLink(link.optString("titulo"), Uri.parse(link.optString("href")));
            }
        }
        if (!conversa.mensagens.isEmpty()) rolagem.post(() -> rolagem.scrollTo(0, mensagens.getBottom()));
    }

    private void adicionarLink(String titulo, Uri destino) {
        String caminho = destino.getPath();
        if (destino.isAbsolute() || destino.getAuthority() != null || caminho == null) return;
        Intent intent;
        if (caminho.equals("/imoveis")) {
            Filtro filtro = new Filtro();
            filtro.finalidade = destino.getQueryParameter("finalidade");
            filtro.tipo = destino.getQueryParameter("tipo");
            filtro.cidade = destino.getQueryParameter("cidade");
            String quartos = destino.getQueryParameter("quartosMin");
            if (quartos != null && quartos.matches("[1-9][0-9]?")) filtro.quartosMin = Integer.valueOf(quartos);
            intent = new Intent(this, ImoveisActivity.class)
                    .putExtra(ImoveisActivity.EXTRA_FILTRO, filtro.paraJson().toString());
        } else if (caminho.matches("/imoveis/[1-9][0-9]{0,17}")) {
            intent = DetalheImovelActivity.intent(this, Long.parseLong(destino.getLastPathSegment()))
                    .putExtra("finalidade", destino.getQueryParameter("finalidade"));
        } else {
            switch (caminho) {
                case "/contato":
                    titulo = getString(R.string.lia_perfil_contato);
                    intent = new Intent(this, PerfilActivity.class);
                    break;
                case "/conta":
                    boolean visitas = "visitas".equals(destino.getQueryParameter("aba"));
                    if (visitas) titulo = getString(R.string.lia_perfil_visitas);
                    intent = new Intent(this, visitas && Sessao.usuario(this) != null
                            ? MeusContatosActivity.class : PerfilActivity.class);
                    break;
                case "/conta/privacidade":
                    intent = new Intent(this, PrivacidadeActivity.class);
                    break;
                case "/esqueci-senha":
                    intent = new Intent(this, EsqueciSenhaActivity.class);
                    break;
                case "/entrar":
                    intent = new Intent(this, PerfilActivity.class);
                    break;
                case "/favoritos":
                    intent = new Intent(this, FavoritosActivity.class);
                    break;
                default:
                    return;
            }
        }
        if (titulo.isEmpty()) return;
        MaterialButton botao = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        botao.setText(titulo);
        botao.setOnClickListener(v -> startActivity(intent));
        mensagens.addView(botao);
    }
}
