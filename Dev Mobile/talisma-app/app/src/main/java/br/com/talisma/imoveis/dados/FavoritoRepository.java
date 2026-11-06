package br.com.talisma.imoveis.dados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.talisma.imoveis.util.Tarefas;

/**
 * Favoritos (D10, 2.3.1): sempre gravados na tabela {@code favorito} do {@link BancoLocal}, com uma
 * cópia do imóvel em JSON — funcionam sem login e offline. Cada alteração fica com
 * {@code sincronizado = 0}; a remoção só marca {@code removido = 1} para poder ser enviada à API.
 * <p>
 * Sincronização com a conta (T05, 6.6): logado, ao entrar e a cada alteração, envia os pendentes
 * (PUT/DELETE) e baixa os remotos — resultado = pendentes locais ∪ remotos; um favorito já
 * sincronizado que sumiu da conta (removido em outro aparelho) sai do local. Sem rede, os pendentes
 * ficam para a próxima vez. Decide por {@link Preferencias#modoDemonstracao} entre o
 * {@link ServidorSimulado} e a {@link Api}.
 */
public class FavoritoRepository {

    private static final String TAG = "FavoritoRepository";
    /** Uma sincronização por vez no app (login e alterações seguidas disparam várias). */
    private static final Object TRAVA_SINCRONIZACAO = new Object();

    private final Context context;

    public FavoritoRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Favoritos ativos, do salvo mais recentemente para o mais antigo. */
    public void listar(Tarefas.Callback<List<Imovel>> callback) {
        Tarefas.executar(() -> {
            List<Imovel> favoritos = new ArrayList<>();
            try (Cursor c = banco().query(BancoLocal.FAVORITO, new String[]{"json"}, "removido = 0",
                    null, null, null, "atualizado_em DESC")) {
                while (c.moveToNext()) {
                    try {
                        favoritos.add(Imovel.deJson(new JSONObject(c.getString(0))));
                    } catch (JSONException e) {
                        Log.w(TAG, "Favorito ilegível", e);
                    }
                }
            }
            return favoritos;
        }, callback);
    }

    /** Ids dos favoritos ativos (para marcar o coração nas listas). */
    public void ids(Tarefas.Callback<Set<Long>> callback) {
        Tarefas.executar(() -> {
            Set<Long> ids = new HashSet<>();
            try (Cursor c = banco().query(BancoLocal.FAVORITO, new String[]{"imovel_id"}, "removido = 0",
                    null, null, null, null)) {
                while (c.moveToNext()) {
                    ids.add(c.getLong(0));
                }
            }
            return ids;
        }, callback);
    }

    public void isFavorito(long imovelId, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> favorito(imovelId), callback);
    }

    /** Salva (ou restaura, no "Desfazer") o favorito; entrega {@code true}. */
    public void adicionar(Imovel imovel, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> {
            gravar(imovel, false);
            sincronizarEmSegundoPlano();
            return true;
        }, callback);
    }

    /** Remove o favorito (marcado para envio à API); entrega {@code false}. */
    public void remover(Imovel imovel, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> {
            gravar(imovel, true);
            sincronizarEmSegundoPlano();
            return false;
        }, callback);
    }

    /** Inverte o coração; entrega o novo estado ({@code true} = favorito). */
    public void alternar(Imovel imovel, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> {
            boolean novo = !favorito(imovel.id);
            gravar(imovel, !novo);
            sincronizarEmSegundoPlano();
            return novo;
        }, callback);
    }

    // ---------------------------------------------------------------- sincronização (D10)

    /**
     * Sincroniza agora (ex.: ao abrir Favoritos logado) e avisa ao terminar. Deslogado não faz nada.
     * Erros chegam ao callback (sem rede → os pendentes ficam; 401 → sessão expirada).
     */
    public void sincronizar(Tarefas.Callback<Void> callback) {
        Tarefas.executar(() -> {
            sincronizarAgora();
            return null;
        }, callback);
    }

    /** Sincroniza em segundo plano, sem avisar (ao entrar e a cada alteração). Erros só vão ao log. */
    public void sincronizarEmSegundoPlano() {
        if (!Sessao.logado(context)) {
            return;
        }
        Tarefas.emSegundoPlano(() -> {
            try {
                sincronizarAgora();
            } catch (ApiException e) {
                Log.w(TAG, "Sincronização de favoritos adiada (status " + e.getStatus() + ")");
            } catch (RuntimeException e) {
                Log.e(TAG, "Falha na sincronização de favoritos", e);
            }
        });
    }

    /** Quantos favoritos ainda não foram enviados à conta (aviso antes de alternar o Modo demonstração). */
    public void naoSincronizados(Tarefas.Callback<Integer> callback) {
        Tarefas.executar(() -> {
            try (Cursor c = banco().rawQuery("SELECT COUNT(*) FROM " + BancoLocal.FAVORITO
                    + " WHERE sincronizado = 0", null)) {
                return c.moveToFirst() ? c.getInt(0) : 0;
            }
        }, callback);
    }

    /**
     * Ao trocar de servidor (Modo demonstração — D26): os favoritos ativos voltam a ser só locais
     * (pendentes de envio para a próxima conta) e as remoções pendentes do servidor anterior somem.
     */
    void desvincularDoServidor() {
        Tarefas.emSegundoPlano(() -> {
            synchronized (TRAVA_SINCRONIZACAO) {
                SQLiteDatabase db = banco();
                db.delete(BancoLocal.FAVORITO, "removido = 1", null);
                ContentValues valores = new ContentValues();
                valores.put("sincronizado", 0);
                db.update(BancoLocal.FAVORITO, valores, null, null);
            }
        });
    }

    private void sincronizarAgora() throws ApiException {
        synchronized (TRAVA_SINCRONIZACAO) {
            if (!Sessao.logado(context)) {
                return;
            }
            enviarPendentes();
            baixarRemotos();
        }
    }

    /** Envia cada alteração pendente; só marca/apaga a linha se ela não mudou durante o envio. */
    private void enviarPendentes() throws ApiException {
        List<long[]> pendentes = new ArrayList<>(); // {imovel_id, removido, atualizado_em}
        try (Cursor c = banco().query(BancoLocal.FAVORITO, new String[]{"imovel_id", "removido", "atualizado_em"},
                "sincronizado = 0", null, null, null, "atualizado_em")) {
            while (c.moveToNext()) {
                pendentes.add(new long[]{c.getLong(0), c.getLong(1), c.getLong(2)});
            }
        }
        for (long[] p : pendentes) {
            long imovelId = p[0];
            boolean removido = p[1] == 1;
            String[] mesmaVersao = {String.valueOf(imovelId), String.valueOf(p[2])};
            String onde = "imovel_id = ? AND atualizado_em = ? AND sincronizado = 0";
            if (removido) {
                removerRemoto(imovelId);
                banco().delete(BancoLocal.FAVORITO, onde, mesmaVersao);
                continue;
            }
            try {
                adicionarRemoto(imovelId);
            } catch (ApiException e) {
                if (e.getStatus() != 404) {
                    throw e;
                }
                // Imóvel não existe mais no servidor: sai dos favoritos.
                banco().delete(BancoLocal.FAVORITO, onde, mesmaVersao);
                continue;
            }
            ContentValues valores = new ContentValues();
            valores.put("sincronizado", 1);
            banco().update(BancoLocal.FAVORITO, valores, onde, mesmaVersao);
        }
    }

    /** Baixa os favoritos da conta e mescla com o local (pendentes locais têm prioridade). */
    private void baixarRemotos() throws ApiException {
        List<Imovel> remotos = listarRemotos();
        SQLiteDatabase db = banco();
        Set<Long> idsRemotos = new HashSet<>();
        db.beginTransaction();
        try {
            for (Imovel imovel : remotos) {
                idsRemotos.add(imovel.id);
                Boolean pendente = null;
                try (Cursor c = db.query(BancoLocal.FAVORITO, new String[]{"sincronizado"}, "imovel_id = ?",
                        new String[]{String.valueOf(imovel.id)}, null, null, null)) {
                    if (c.moveToFirst()) {
                        pendente = c.getInt(0) == 0;
                    }
                }
                if (Boolean.TRUE.equals(pendente)) {
                    continue; // mudou no aparelho depois do envio: vai na próxima sincronização
                }
                ContentValues valores = new ContentValues();
                valores.put("json", imovel.paraJson().toString());
                valores.put("sincronizado", 1);
                valores.put("removido", 0);
                if (pendente == null) {
                    valores.put("imovel_id", imovel.id);
                    valores.put("atualizado_em", System.currentTimeMillis());
                    db.insert(BancoLocal.FAVORITO, null, valores);
                } else {
                    db.update(BancoLocal.FAVORITO, valores, "imovel_id = ?", new String[]{String.valueOf(imovel.id)});
                }
            }
            // Já sincronizados que não estão mais na conta: removidos em outro aparelho.
            List<String> remover = new ArrayList<>();
            try (Cursor c = db.query(BancoLocal.FAVORITO, new String[]{"imovel_id"}, "sincronizado = 1",
                    null, null, null, null)) {
                while (c.moveToNext()) {
                    if (!idsRemotos.contains(c.getLong(0))) {
                        remover.add(String.valueOf(c.getLong(0)));
                    }
                }
            }
            for (String id : remover) {
                db.delete(BancoLocal.FAVORITO, "imovel_id = ? AND sincronizado = 1", new String[]{id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private boolean simulado() {
        return Preferencias.modoDemonstracao(context);
    }

    private List<Imovel> listarRemotos() throws ApiException {
        if (simulado()) {
            return new ServidorSimulado(context).favoritos(Sessao.token(context));
        }
        JSONArray lista = Api.lista(new Api(context).get(ApiRotas.FAVORITOS));
        List<Imovel> imoveis = new ArrayList<>();
        try {
            for (int k = 0; k < lista.length(); k++) {
                imoveis.add(Imovel.deJson(lista.getJSONObject(k)));
            }
        } catch (JSONException e) {
            throw new ApiException(500, null, e);
        }
        return imoveis;
    }

    private void adicionarRemoto(long imovelId) throws ApiException {
        if (simulado()) {
            new ServidorSimulado(context).adicionarFavorito(Sessao.token(context), imovelId);
        } else {
            new Api(context).put(ApiRotas.favorito(imovelId), null);
        }
    }

    private void removerRemoto(long imovelId) throws ApiException {
        try {
            if (simulado()) {
                new ServidorSimulado(context).removerFavorito(Sessao.token(context), imovelId);
            } else {
                new Api(context).delete(ApiRotas.favorito(imovelId));
            }
        } catch (ApiException e) {
            if (e.getStatus() != 404) {
                throw e; // 404 = já não estava na conta
            }
        }
    }

    private SQLiteDatabase banco() {
        return BancoLocal.instancia(context).getWritableDatabase();
    }

    private boolean favorito(long imovelId) {
        try (Cursor c = banco().query(BancoLocal.FAVORITO, new String[]{"imovel_id"},
                "imovel_id = ? AND removido = 0", new String[]{String.valueOf(imovelId)},
                null, null, null)) {
            return c.moveToFirst();
        }
    }

    private void gravar(Imovel imovel, boolean removido) {
        ContentValues valores = new ContentValues();
        valores.put("imovel_id", imovel.id);
        valores.put("json", imovel.paraJson().toString());
        valores.put("sincronizado", 0);
        valores.put("removido", removido ? 1 : 0);
        valores.put("atualizado_em", System.currentTimeMillis());
        banco().insertWithOnConflict(BancoLocal.FAVORITO, null, valores, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
