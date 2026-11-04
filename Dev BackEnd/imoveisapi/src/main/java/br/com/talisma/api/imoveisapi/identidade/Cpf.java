package br.com.talisma.api.imoveisapi.identidade;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class Cpf {
    private Cpf() {}

    static String normalizar(String informado) {
        String cpf = informado.replaceAll("[.\\- ]", "");
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1
                || !digitoValido(cpf, 9) || !digitoValido(cpf, 10)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");
        }
        return cpf;
    }

    private static boolean digitoValido(String cpf, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (cpf.charAt(i) - '0') * (posicao + 1 - i);
        }
        int digito = 11 - soma % 11;
        return (digito >= 10 ? 0 : digito) == cpf.charAt(posicao) - '0';
    }
}
