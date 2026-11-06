package br.com.talisma.imoveis.util;

import android.util.Patterns;

import androidx.annotation.Nullable;

/** Regras de validação de formulário (6.6). O servidor valida de novo. */
public final class Validacoes {

    /** Senha: mínimo de 6 caracteres (D34). */
    public static final int SENHA_MINIMA = 6;

    private Validacoes() {
    }

    public static boolean vazio(@Nullable String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** E-mail por {@link Patterns#EMAIL_ADDRESS}. */
    public static boolean email(@Nullable String email) {
        return !vazio(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /** Senha com pelo menos {@link #SENHA_MINIMA} caracteres. */
    public static boolean senha(@Nullable String senha) {
        return senha != null && senha.length() >= SENHA_MINIMA;
    }

    /** Confirmação igual à senha. */
    public static boolean senhasIguais(@Nullable String senha, @Nullable String confirmacao) {
        return senha != null && senha.equals(confirmacao);
    }

    /** CPF com 11 dígitos e dígitos verificadores válidos (aceita com ou sem máscara) (D12). */
    public static boolean cpf(@Nullable String cpf) {
        String d = Formatos.digitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return d.charAt(9) - '0' == digitoVerificador(d, 9) && d.charAt(10) - '0' == digitoVerificador(d, 10);
    }

    private static int digitoVerificador(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Telefone/WhatsApp com DDD: 10 ou 11 dígitos ("(00) 00000-0000"). */
    public static boolean telefone(@Nullable String telefone) {
        int tamanho = Formatos.digitos(telefone).length();
        return tamanho == 10 || tamanho == 11;
    }

    /** Faixa de preço válida: mínimo ≤ máximo quando os dois forem informados (RN-003). */
    public static boolean faixaPreco(@Nullable Double minimo, @Nullable Double maximo) {
        return minimo == null || maximo == null || minimo <= maximo;
    }

    /** Código de verificação de 4 dígitos. */
    public static boolean codigo(@Nullable String codigo) {
        return codigo != null && codigo.matches("\\d{4}");
    }
}
