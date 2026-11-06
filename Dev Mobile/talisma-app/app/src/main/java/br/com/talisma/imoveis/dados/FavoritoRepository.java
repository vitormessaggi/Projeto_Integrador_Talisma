package br.com.talisma.imoveis.dados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
 * A sincronização com a conta (logado) entra na T05.
 */
public class FavoritoRepository {

    private static final String TAG = "FavoritoRepository";

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
            return true;
        }, callback);
    }

    /** Remove o favorito (marcado para envio à API); entrega {@code false}. */
    public void remover(Imovel imovel, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> {
            gravar(imovel, true);
            return false;
        }, callback);
    }

    /** Inverte o coração; entrega o novo estado ({@code true} = favorito). */
    public void alternar(Imovel imovel, Tarefas.Callback<Boolean> callback) {
        Tarefas.executar(() -> {
            boolean novo = !favorito(imovel.id);
            gravar(imovel, !novo);
            return novo;
        }, callback);
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
