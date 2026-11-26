package br.com.talisma.api.imoveisapi.identidade;

import static org.springframework.http.HttpStatus.*;

import br.com.talisma.api.imoveisapi.catalogo.CatalogoService;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UsuariosService {
    private final JdbcClient banco;
    private final IdentidadeService identidade;
    private final BCryptPasswordEncoder senhas;
    private final CatalogoService catalogo;

    UsuariosService(JdbcClient banco, IdentidadeService identidade, BCryptPasswordEncoder senhas,
                    CatalogoService catalogo) {
        this.banco = banco;
        this.identidade = identidade;
        this.senhas = senhas;
        this.catalogo = catalogo;
    }

    public record Perfil(long id, String nome, String email, String perfil, String telefone, String cpfMascarado, String rgMascarado) {}
    public record ContaInterna(long id, String nome, String email, String perfil, boolean bloqueado) {}
    public record PaginaInterna(List<ContaInterna> itens, int pagina, int tamanho, long totalElementos, long totalPaginas) {}

    Perfil perfil(long id) {
        return banco.sql("""
                SELECT u.id,u.nome,u.email,u.perfil,u.telefone,
                    CONCAT('***.***.***-',RIGHT(d.cpf,2)) AS cpf_mascarado,
                    CONCAT('***',RIGHT(d.rg,2)) AS rg_mascarado
                FROM usuario u LEFT JOIN documento_cliente d ON d.usuario_id=u.id WHERE u.id=?
                """).param(id).query(Perfil.class).single();
    }

    Perfil atualizar(long id, String nome, String telefone) {
        banco.sql("UPDATE usuario SET nome=?, telefone=? WHERE id=?").params(nome.strip(), telefone, id).update();
        return perfil(id);
    }

    void trocarSenha(long id, String atual, String nova) {
        String hash = banco.sql("SELECT senha_hash FROM usuario WHERE id=? FOR UPDATE").param(id).query(String.class).single();
        if (atual.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || !senhas.matches(atual, hash)) {
            throw new ResponseStatusException(BAD_REQUEST, "Senha atual incorreta");
        }
        banco.sql("UPDATE usuario SET senha_hash=? WHERE id=?").params(identidade.hashSenha(nova), id).update();
        identidade.revogarSessoes(id);
    }

    void favorito(IdentidadeService.UsuarioDto usuario, long imovelId, boolean salvar) {
        exigir(usuario, "CLIENTE");
        if (salvar) {
            int inseridos = banco.sql("""
                    INSERT INTO favorito(usuario_id,imovel_id)
                    SELECT ?,id FROM imovel WHERE id=? AND status='PUBLICADO'
                    ON DUPLICATE KEY UPDATE imovel_id=favorito.imovel_id
                    """).params(usuario.id(), imovelId).update();
            if (inseridos == 0) {
                catalogo.detalhar(imovelId);
            }
        } else {
            banco.sql("DELETE FROM favorito WHERE usuario_id=? AND imovel_id=?").params(usuario.id(), imovelId).update();
        }
    }

    CatalogoService.Pagina favoritos(IdentidadeService.UsuarioDto usuario, int pagina, int tamanho) {
        exigir(usuario, "CLIENTE");
        return catalogo.favoritos(usuario.id(), pagina, tamanho);
    }

    PaginaInterna internos(IdentidadeService.UsuarioDto admin, int pagina, int tamanho) {
        exigir(admin, "ADMIN");
        CatalogoService.validarPagina(pagina, tamanho);
        long total = banco.sql("SELECT COUNT(*) FROM usuario WHERE perfil IN ('CORRETOR','ADMIN','FUNCIONARIO')").query(Long.class).single();
        var itens = banco.sql("""
                SELECT id,nome,email,perfil,bloqueado FROM usuario WHERE perfil IN ('CORRETOR','ADMIN','FUNCIONARIO')
                ORDER BY id LIMIT ? OFFSET ?
                """).params(tamanho, (long) pagina * tamanho).query(ContaInterna.class).list();
        return new PaginaInterna(itens, pagina, tamanho, total, (total + tamanho - 1) / tamanho);
    }

    ContaInterna cadastrarEquipe(IdentidadeService.UsuarioDto admin, UsuariosController.CadastroEquipe dados, String perfil) {
        exigir(admin, "ADMIN");
        if (!"CORRETOR".equals(perfil) && !"FUNCIONARIO".equals(perfil)) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecione o perfil da conta");
        }
        String cpf = dados.cpf().replaceAll("[^0-9]", "");
        if (!cpfValido(cpf)) {
            throw new ResponseStatusException(BAD_REQUEST, "CPF inválido");
        }
        String telefone = dados.telefone().replaceAll("[^0-9]", "");
        if (!telefone.matches("[1-9][0-9]{9,10}")) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe o telefone com DDD");
        }
        String creci = dados.creci() == null ? "" : dados.creci().strip();
        if ("CORRETOR".equals(perfil) && !creci.matches(".*[0-9].*")) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe o CRECI do corretor");
        }
        ContaInterna conta = criarInterno(dados.nome(), dados.email(), dados.senha(), perfil);
        try {
            banco.sql("""
                    INSERT INTO documento_equipe(usuario_id,cpf,rg,orgao_emissor,uf_rg,cargo,creci)
                    VALUES (?,?,?,?,?,?,NULLIF(?, ''))
                    """).params(conta.id(), cpf, dados.rg().strip(), dados.orgaoEmissor().strip(), dados.ufRg(),
                    dados.cargo().strip(), "CORRETOR".equals(perfil) ? creci : "").update();
        } catch (DuplicateKeyException erro) {
            throw new ResponseStatusException(CONFLICT, "CPF já cadastrado na equipe");
        }
        banco.sql("UPDATE usuario SET telefone=? WHERE id=?").params(telefone, conta.id()).update();
        return conta;
    }

    private static boolean cpfValido(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        for (int posicao = 9; posicao < 11; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) {
                soma += (cpf.charAt(i) - '0') * (posicao + 1 - i);
            }
            int digito = (soma * 10 % 11) % 10;
            if (cpf.charAt(posicao) - '0' != digito) return false;
        }
        return true;
    }

    void desbloquear(IdentidadeService.UsuarioDto admin, long id) {
        exigir(admin, "ADMIN");
        int alterados = banco.sql("""
                UPDATE usuario SET bloqueado=FALSE,tentativas=0,limitado_ate=NULL
                WHERE id=? AND perfil IN ('ADMIN','CORRETOR','FUNCIONARIO')
                """).param(id).update();
        if (alterados == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Conta interna não encontrada");
        }
    }

    public void provisionarAdmin(String nome, String email, String senha) {
        banco.sql("SELECT codigo FROM perfil WHERE codigo='ADMIN' FOR UPDATE").query(String.class).single();
        if (banco.sql("SELECT COUNT(*) FROM usuario WHERE perfil='ADMIN'").query(Long.class).single() > 0) {
            throw new IllegalStateException("Já existe um administrador");
        }
        criarInterno(nome, email, senha, "ADMIN");
    }

    private ContaInterna criarInterno(String nome, String email, String senha, String perfil) {
        String normalizado = IdentidadeService.normalizarEmail(email);
        try {
            banco.sql("""
                    INSERT INTO usuario(nome,email,senha_hash,perfil,email_verificado)
                    VALUES (?,?,?,?,TRUE)
                    """).params(nome.strip(), normalizado, identidade.hashSenha(senha), perfil).update();
        } catch (DuplicateKeyException erro) {
            throw new ResponseStatusException(CONFLICT, "E-mail já cadastrado");
        }
        return banco.sql("SELECT id,nome,email,perfil,bloqueado FROM usuario WHERE email=?")
                .param(normalizado).query(ContaInterna.class).single();
    }

    public static void exigir(IdentidadeService.UsuarioDto usuario, String perfil) {
        if (!usuario.perfil().equals(perfil)) {
            throw new ResponseStatusException(FORBIDDEN, "Operação não permitida");
        }
    }
}
