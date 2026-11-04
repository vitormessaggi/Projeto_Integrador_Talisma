package br.com.talisma.api.imoveisapi.identidade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("provisionar-admin")
class ProvisionamentoAdmin implements ApplicationRunner {
    private final UsuariosService usuarios;
    private final String nome;
    private final String email;
    private final String senha;

    ProvisionamentoAdmin(UsuariosService usuarios, @Value("${ADMIN_NOME}") String nome,
                         @Value("${ADMIN_EMAIL}") String email, @Value("${ADMIN_SENHA}") String senha) {
        this.usuarios = usuarios;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (nome.isBlank() || nome.length() > 160 || email.length() > 254 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Informe nome e e-mail válidos para o administrador");
        }
        usuarios.provisionarAdmin(nome, email, senha);
    }
}
