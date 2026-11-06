package br.com.talisma.imoveis.util;

import android.content.Context;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.talisma.imoveis.R;
import br.com.talisma.imoveis.dados.Imovel;

/**
 * Formatação para exibição (6.6, D21): moeda pt-BR sem centavos, "/mês" na locação, "65 m²",
 * "a 1,2 km", datas dd/MM/yyyy e máscaras de CPF/telefone. Textos fixos vêm de strings.xml.
 */
public final class Formatos {

    /** pt-BR (equivale a {@code new Locale("pt", "BR")}, sem o construtor obsoleto). */
    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public static final String MASCARA_CPF = "###.###.###-##";
    public static final String MASCARA_TELEFONE = "(##) #####-####";
    public static final String MASCARA_TELEFONE_FIXO = "(##) ####-####";

    private Formatos() {
    }

    /** "R$ 450.000" (sem centavos). */
    public static String moeda(double valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        // Android 7 (ICU antigo) formata "R$450.000" sem espaço; os mais novos usam espaço não separável (T09).
        String texto = formato.format(valor);
        String simbolo = formato.getCurrency() != null ? formato.getCurrency().getSymbol(PT_BR) : "";
        if (!simbolo.isEmpty() && texto.startsWith(simbolo) && texto.length() > simbolo.length()
                && !Character.isSpaceChar(texto.charAt(simbolo.length()))) {
            texto = simbolo + ' ' + texto.substring(simbolo.length());
        }
        return texto;
    }

    /** Preço do imóvel: "R$ 450.000" na venda, "R$ 2.500/mês" na locação. */
    public static String preco(Context context, Imovel imovel) {
        String valor = moeda(imovel.preco);
        return imovel.isLocacao() ? context.getString(R.string.formato_preco_mes, valor) : valor;
    }

    /** "65 m²" (até 1 casa decimal); vazio se a área não for informada. */
    public static String area(Context context, @Nullable Double area) {
        if (area == null) {
            return "";
        }
        return context.getString(R.string.formato_area, decimal(area, 1));
    }

    /** "a 1,2 km". */
    public static String distancia(Context context, float km) {
        return context.getString(R.string.formato_distancia, decimal(km, 1));
    }

    /** Número no formato pt-BR com até {@code casas} decimais ("1.234,5"). */
    public static String decimal(double valor, int casas) {
        NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
        formato.setMaximumFractionDigits(casas);
        formato.setMinimumFractionDigits(0);
        return formato.format(valor);
    }

    /** "2026-11-12" → "12/11/2026"; devolve o texto original se não estiver em yyyy-MM-dd. */
    public static String data(@Nullable String isoData) {
        if (isoData == null) {
            return "";
        }
        try {
            Date data = new SimpleDateFormat("yyyy-MM-dd", PT_BR).parse(isoData);
            return data != null ? new SimpleDateFormat("dd/MM/yyyy", PT_BR).format(data) : isoData;
        } catch (ParseException e) {
            return isoData;
        }
    }

    /** Data em yyyy-MM-dd (formato enviado à API, ex.: data da visita). */
    public static String dataIso(Date data) {
        return new SimpleDateFormat("yyyy-MM-dd", PT_BR).format(data);
    }

    /** Só os dígitos do texto. */
    public static String digitos(@Nullable String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    /**
     * Aplica uma máscara com {@code #} no lugar de cada dígito ("###.###.###-##"), parando quando
     * os dígitos acabam. Serve para formatar enquanto o usuário digita.
     */
    public static String mascarar(@Nullable String texto, String mascara) {
        String d = digitos(texto);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int k = 0; k < mascara.length() && i < d.length(); k++) {
            char c = mascara.charAt(k);
            if (c == '#') {
                sb.append(d.charAt(i++));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** "000.000.000-00". */
    public static String cpf(@Nullable String cpf) {
        return mascarar(cpf, MASCARA_CPF);
    }

    /** CPF mascarado para exibição: "***.***.***-12" (D12). */
    public static String cpfOculto(@Nullable String cpf) {
        String d = digitos(cpf);
        if (d.length() != 11) {
            return "";
        }
        return "***.***.***-" + d.substring(9);
    }

    /** "(00) 00000-0000" (celular) ou "(00) 0000-0000" (fixo). */
    public static String telefone(@Nullable String telefone) {
        String d = digitos(telefone);
        return mascarar(d, d.length() == 10 ? MASCARA_TELEFONE_FIXO : MASCARA_TELEFONE);
    }
}
