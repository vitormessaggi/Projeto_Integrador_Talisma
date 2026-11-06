package br.com.talisma.imoveis.dados;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configurações do app (SharedPreferences "config"): {@code modo_demonstracao} (padrão ligado
 * enquanto não houver API — D26), {@code simular_falha_rede} (6.7) e {@code localizacao_pedida} (D15).
 */
public final class Preferencias {

    private static final String ARQUIVO = "config";
    private static final String CHAVE_MODO_DEMONSTRACAO = "modo_demonstracao";
    private static final String CHAVE_SIMULAR_FALHA_REDE = "simular_falha_rede";
    private static final String CHAVE_LOCALIZACAO_PEDIDA = "localizacao_pedida";

    private Preferencias() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    /** {@code true} = Repositories usam o {@link ServidorSimulado}; {@code false} = {@link Api} real. */
    public static boolean modoDemonstracao(Context context) {
        return prefs(context).getBoolean(CHAVE_MODO_DEMONSTRACAO, true);
    }

    public static void setModoDemonstracao(Context context, boolean ligado) {
        prefs(context).edit().putBoolean(CHAVE_MODO_DEMONSTRACAO, ligado).apply();
    }

    public static boolean simularFalhaRede(Context context) {
        return prefs(context).getBoolean(CHAVE_SIMULAR_FALHA_REDE, false);
    }

    public static void setSimularFalhaRede(Context context, boolean ligado) {
        prefs(context).edit().putBoolean(CHAVE_SIMULAR_FALHA_REDE, ligado).apply();
    }

    public static boolean localizacaoPedida(Context context) {
        return prefs(context).getBoolean(CHAVE_LOCALIZACAO_PEDIDA, false);
    }

    public static void setLocalizacaoPedida(Context context, boolean pedida) {
        prefs(context).edit().putBoolean(CHAVE_LOCALIZACAO_PEDIDA, pedida).apply();
    }
}
