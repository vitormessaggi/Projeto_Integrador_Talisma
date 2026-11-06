package br.com.talisma.imoveis.dados;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.util.Formatos;
import br.com.talisma.imoveis.util.Tarefas;
import br.com.talisma.imoveis.util.Validacoes;

/**
 * Visitas (EAP 2.4.2, D4, D30, D37): solicitar (gera o lead — RN-004), listar "Meus contatos" e
 * cancelar. Exige login; 401 chega como {@link ApiException#isSessaoExpirada()} (sessão já limpa).
 * Decide por {@link Preferencias#modoDemonstracao} entre o {@link ServidorSimulado} e a {@link Api}.
 * <p>
 * Para saber se a tela Solicitar visita precisa pedir o CPF e pré-preencher o telefone, use
 * {@link ContaRepository#perfil} (a {@link Sessao} não guarda CPF — D12).
 */
public class VisitaRepository {

    private final Context context;

    public VisitaRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Solicita a visita: {@code data} yyyy-MM-dd (a partir de amanhã), {@code periodo}
     * {@link Visita#MANHA}|{@link Visita#TARDE}|{@link Visita#NOITE}, telefone obrigatório, CPF só se
     * a conta não tiver, mensagem opcional. O telefone fica salvo no perfil (D30). Entrega a visita
     * criada (PENDENTE).
     */
    public void solicitar(long imovelId, String data, String periodo, String telefone, @Nullable String cpf,
                          @Nullable String mensagem, Tarefas.Callback<Visita> callback) {
        String cpfDigitos = Validacoes.vazio(cpf) ? null : Formatos.digitos(cpf);
        String texto = Validacoes.vazio(mensagem) ? null : mensagem.trim();
        Tarefas.executar(() -> {
            Visita visita;
            if (simulado()) {
                visita = new ServidorSimulado(context).solicitarVisita(Sessao.token(context), imovelId, data,
                        periodo, telefone, cpfDigitos, texto);
            } else {
                JSONObject corpo = new JSONObject();
                try {
                    corpo.put("imovelId", imovelId);
                    corpo.put("data", data);
                    corpo.put("periodo", periodo);
                    corpo.put("telefone", telefone);
                    corpo.putOpt("cpf", cpfDigitos);
                    corpo.putOpt("mensagem", texto);
                } catch (JSONException e) {
                    throw new IllegalStateException(e);
                }
                visita = visitaDeJson(Api.objeto(new Api(context).post(ApiRotas.VISITAS, corpo)));
            }
            salvarTelefoneNaSessao(telefone);
            return visita;
        }, callback);
    }

    /** "Meus contatos com corretores": visitas do usuário, da mais nova para a mais antiga. */
    public void minhas(Tarefas.Callback<List<Visita>> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                return new ServidorSimulado(context).minhasVisitas(Sessao.token(context));
            }
            JSONArray lista = Api.lista(new Api(context).get(ApiRotas.MINHAS_VISITAS));
            List<Visita> visitas = new ArrayList<>();
            try {
                for (int k = 0; k < lista.length(); k++) {
                    visitas.add(Visita.deJson(lista.getJSONObject(k)));
                }
            } catch (JSONException e) {
                throw new ApiException(500, null, e);
            }
            return visitas;
        }, callback);
    }

    /** Cancela a visita (só Pendente ou Confirmada — D37); entrega a visita atualizada. */
    public void cancelar(long visitaId, Tarefas.Callback<Visita> callback) {
        Tarefas.executar(() -> {
            if (simulado()) {
                return new ServidorSimulado(context).cancelarVisita(Sessao.token(context), visitaId);
            }
            return visitaDeJson(Api.objeto(new Api(context).patch(ApiRotas.cancelarVisita(visitaId), null)));
        }, callback);
    }

    private boolean simulado() {
        return Preferencias.modoDemonstracao(context);
    }

    /** D30: o telefone informado na visita passa a ser o do perfil. */
    private void salvarTelefoneNaSessao(String telefone) {
        Usuario usuario = Sessao.usuario(context);
        if (usuario != null) {
            usuario.telefone = Formatos.telefone(telefone);
            Sessao.atualizarUsuario(context, usuario);
        }
    }

    private static Visita visitaDeJson(JSONObject o) throws ApiException {
        try {
            return Visita.deJson(o);
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
    }
}
