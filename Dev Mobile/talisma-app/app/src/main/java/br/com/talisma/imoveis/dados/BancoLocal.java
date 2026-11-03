package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Banco SQLite do app (6.8, versão 1): favoritos, cache da vitrine, buscas recentes e as tabelas
 * {@code sim_*} do {@link ServidorSimulado}. Uma instância única para o app ({@link #instancia}).
 */
public class BancoLocal extends SQLiteOpenHelper {

    private static final String NOME = "talisma.db";
    private static final int VERSAO = 1;

    public static final String FAVORITO = "favorito";
    public static final String CACHE_LISTA = "cache_lista";
    public static final String CACHE_IMOVEL = "cache_imovel";
    public static final String BUSCA_RECENTE = "busca_recente";
    public static final String SIM_USUARIO = "sim_usuario";
    public static final String SIM_FAVORITO = "sim_favorito";
    public static final String SIM_VISITA = "sim_visita";
    public static final String SIM_FOTO = "sim_foto";

    /** Limites de retenção (6.8, D36). */
    public static final int MAX_CACHE_IMOVEL = 30;
    public static final int MAX_BUSCA_RECENTE = 10;

    private static BancoLocal instancia;

    public static synchronized BancoLocal instancia(Context context) {
        if (instancia == null) {
            instancia = new BancoLocal(context.getApplicationContext());
        }
        return instancia;
    }

    private BancoLocal(Context context) {
        super(context, NOME, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + FAVORITO + " ("
                + "imovel_id INTEGER PRIMARY KEY, "
                + "json TEXT, "
                + "sincronizado INTEGER NOT NULL DEFAULT 0, "
                + "removido INTEGER NOT NULL DEFAULT 0, "
                + "atualizado_em INTEGER)");
        db.execSQL("CREATE TABLE " + CACHE_LISTA + " ("
                + "chave TEXT PRIMARY KEY, "
                + "json TEXT, "
                + "atualizado_em INTEGER)");
        db.execSQL("CREATE TABLE " + CACHE_IMOVEL + " ("
                + "id INTEGER PRIMARY KEY, "
                + "json TEXT, "
                + "visto_em INTEGER)");
        db.execSQL("CREATE TABLE " + BUSCA_RECENTE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "filtro_json TEXT UNIQUE, "
                + "criada_em INTEGER)");
        db.execSQL("CREATE TABLE " + SIM_USUARIO + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT, "
                + "email TEXT UNIQUE, "
                + "senha TEXT, "
                + "cpf TEXT, "
                + "telefone TEXT, "
                + "perfil TEXT, "
                + "bloqueado INTEGER NOT NULL DEFAULT 0, "
                + "email_verificado INTEGER NOT NULL DEFAULT 0, "
                + "tentativas INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE " + SIM_FAVORITO + " ("
                + "usuario_id INTEGER, "
                + "imovel_id INTEGER, "
                + "PRIMARY KEY (usuario_id, imovel_id))");
        db.execSQL("CREATE TABLE " + SIM_VISITA + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_id INTEGER, "
                + "imovel_id INTEGER, "
                + "data TEXT, "
                + "periodo TEXT, "
                + "telefone TEXT, "
                + "cpf TEXT, "
                + "mensagem TEXT, "
                + "status TEXT, "
                + "criada_em INTEGER)");
        db.execSQL("CREATE TABLE " + SIM_FOTO + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "imovel_id INTEGER, "
                + "caminho TEXT, "
                + "enviada_em INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versaoAntiga, int versaoNova) {
        // Versão 1: sem migrações ainda.
    }
}
