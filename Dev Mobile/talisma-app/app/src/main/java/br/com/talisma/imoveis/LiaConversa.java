package br.com.talisma.imoveis;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.dados.Api;
import br.com.talisma.imoveis.dados.ApiException;
import br.com.talisma.imoveis.util.Tarefas;

public class LiaConversa extends ViewModel {
    static final class Mensagem {
        final boolean propria;
        final String texto;
        final JSONArray links;

        Mensagem(boolean propria, String texto, JSONArray links) {
            this.propria = propria;
            this.texto = texto;
            this.links = links;
        }
    }

    final List<Mensagem> mensagens = new ArrayList<>();
    final MutableLiveData<Void> atualizacao = new MutableLiveData<>(null);
    String rascunho = "";
    boolean enviando;
    int estado;

    void enviar(Api api, String texto, boolean usarIa) {
        enviando = true;
        estado = R.string.lia_aguardando;
        atualizacao.setValue(null);
        Tarefas.executar(() -> {
            try {
                JSONObject pergunta = new JSONObject().put("pergunta", texto).put("usar_ia", usarIa);
                JSONObject resposta = api.perguntarLia(pergunta);
                String conteudo = resposta.getString("resposta").trim();
                if (conteudo.isEmpty()) throw new JSONException("Resposta vazia");
                return new Mensagem(false, conteudo, resposta.getJSONArray("links"));
            } catch (JSONException erro) {
                throw new ApiException(502, null, erro);
            }
        }, new Tarefas.Callback<Mensagem>() {
            @Override
            public void sucesso(Mensagem resposta) {
                mensagens.add(new Mensagem(true, texto, new JSONArray()));
                mensagens.add(resposta);
                rascunho = "";
                enviando = false;
                estado = 0;
                atualizacao.setValue(null);
            }

            @Override
            public void erro(ApiException erro) {
                enviando = false;
                estado = erro.getStatus() == 429 ? R.string.lia_limite : R.string.lia_falha;
                atualizacao.setValue(null);
            }
        });
    }
}
