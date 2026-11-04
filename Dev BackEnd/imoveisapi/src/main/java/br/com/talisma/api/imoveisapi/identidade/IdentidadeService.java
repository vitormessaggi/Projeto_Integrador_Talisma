package br.com.talisma.api.imoveisapi.identidade;

import static org.springframework.http.HttpStatus.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IdentidadeService {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final JdbcClient banco;
    private final BCryptPasswordEncoder senhas;
    private final JwtEncoder encoder;
    private final ApplicationEventPublisher eventos;
    private final String hashAnonimo;

    IdentidadeService(JdbcClient banco, BCryptPasswordEncoder senhas, JwtEncoder encoder,
                      ApplicationEventPublisher eventos) {
        this.banco = banco;
        this.senhas = senhas;
        this.encoder = encoder;
        this.eventos = eventos;
        this.hashAnonimo = senhas.encode(gerarToken());
    }

    public record UsuarioDto(long id, String nome, String email, String perfil) {}
    public record Tokens(String accessToken, String refreshToken, long expiresIn, UsuarioDto usuario) {}
    record CodigoEmail(String email, String finalidade, String codigo) {}
    private record Usuario(long id, String nome, String email, String perfil, String senhaHash,
                           boolean verificado, boolean bloqueado, int tentativas, Instant limitadoAte) {
        UsuarioDto dto() { return new UsuarioDto(id, nome, email, perfil); }
    }
    private record Sessao(long id, long usuarioId, Instant expiraEm) {}
    private record Desafio(long id, String hash, int tentativas, Instant expiracao, Instant criadoEm) {}

    void cadastrar(IdentidadeController.Cadastro dados) {
        String cpf = Cpf.normalizar(dados.cpf());
        String email = normalizarEmail(dados.email());
        try {
            banco.sql("INSERT INTO usuario(nome,email,senha_hash,perfil) VALUES (?,?,?,'CLIENTE')")
                    .params(dados.nome().strip(), email, hashSenha(dados.senha())).update();
            Usuario usuario = buscar(email);
            banco.sql("INSERT INTO documento_cliente(usuario_id,cpf,rg,orgao_emissor,uf) VALUES (?,?,?,?,?)")
                    .params(usuario.id(), cpf, dados.rg().strip(), dados.orgaoEmissor().strip(), dados.uf()).update();
            emitirCodigo(usuario, "CADASTRO");
        } catch (DuplicateKeyException erro) {
            throw falha(CONFLICT, "Já existe cadastro com os dados informados");
        }
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    Tokens login(IdentidadeController.Login dados) {
        Usuario usuario = buscar(normalizarEmail(dados.email()));
        if (dados.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw falha(UNAUTHORIZED, "Credenciais inválidas");
        }
        boolean senhaCorreta = senhas.matches(dados.senha(), usuario == null ? hashAnonimo : usuario.senhaHash());
        if (usuario == null) {
            throw falha(UNAUTHORIZED, "Credenciais inválidas");
        }
        if (usuario.bloqueado() || (usuario.limitadoAte() != null && usuario.limitadoAte().isAfter(Instant.now()))) {
            throw falha(LOCKED, "Acesso temporariamente indisponível");
        }
        if (!senhaCorreta) {
            registrarFalha(usuario);
            throw falha(UNAUTHORIZED, "Credenciais inválidas");
        }
        if (!usuario.verificado()) {
            throw falha(FORBIDDEN, "Verifique seu e-mail");
        }
        banco.sql("UPDATE usuario SET tentativas=0, limitado_ate=NULL WHERE id=?").param(usuario.id()).update();
        String refresh = gerarToken();
        String hash = sha256(refresh);
        banco.sql("INSERT INTO sessao(usuario_id,refresh_hash,expira_em) VALUES (?,?,?)")
                .params(usuario.id(), hash, Timestamp.from(Instant.now().plus(Duration.ofDays(7)))).update();
        Sessao sessao = buscarSessao(hash);
        return tokens(usuario.dto(), sessao, refresh);
    }

    Tokens renovar(String refresh) {
        Sessao sessao = buscarSessao(sha256(refresh));
        if (sessao == null || !sessao.expiraEm().isAfter(Instant.now())) {
            throw falha(UNAUTHORIZED, "Sessão expirada");
        }
        UsuarioDto usuario = usuarioAtivo(sessao.usuarioId(), sessao.id());
        String novo = gerarToken();
        banco.sql("UPDATE sessao SET refresh_hash=? WHERE id=?").params(sha256(novo), sessao.id()).update();
        return tokens(usuario, sessao, novo);
    }

    void logout(String refresh) {
        banco.sql("UPDATE sessao SET revogada_em=UTC_TIMESTAMP(6) WHERE refresh_hash=? AND revogada_em IS NULL")
                .param(sha256(refresh)).update();
    }

    public UsuarioDto usuarioAtual(Jwt jwt) {
        return usuarioAtivo(Long.parseLong(jwt.getSubject()), Long.parseLong(jwt.getClaimAsString("sid")));
    }

    void solicitarCodigo(String email, String finalidade) {
        Usuario usuario = buscar(normalizarEmail(email));
        if (usuario == null || usuario.bloqueado() || (finalidade.equals("CADASTRO") && usuario.verificado())) {
            return;
        }
        emitirCodigo(usuario, finalidade);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    void verificar(IdentidadeController.Codigo dados) {
        Usuario usuario = validarCodigo(dados.email(), dados.codigo(), "CADASTRO");
        banco.sql("UPDATE usuario SET email_verificado=TRUE WHERE id=?").param(usuario.id()).update();
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    void redefinir(IdentidadeController.Redefinicao dados) {
        String senhaHash = hashSenha(dados.novaSenha());
        Usuario usuario = validarCodigo(dados.email(), dados.codigo(), "RECUPERACAO");
        banco.sql("UPDATE usuario SET senha_hash=? WHERE id=?").params(senhaHash, usuario.id()).update();
        revogarSessoes(usuario.id());
    }

    public void revogarSessoes(long usuarioId) {
        banco.sql("UPDATE sessao SET revogada_em=UTC_TIMESTAMP(6) WHERE usuario_id=? AND revogada_em IS NULL")
                .param(usuarioId).update();
    }

    String hashSenha(String senha) {
        if (senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw falha(BAD_REQUEST, "Senha deve ter pelo menos 8 caracteres e no máximo 72 bytes");
        }
        return senhas.encode(senha);
    }

    private UsuarioDto usuarioAtivo(long usuarioId, long sessaoId) {
        return banco.sql("""
                SELECT u.id,u.nome,u.email,u.perfil FROM usuario u JOIN sessao s ON s.usuario_id=u.id
                WHERE u.id=? AND s.id=? AND s.revogada_em IS NULL AND s.expira_em>UTC_TIMESTAMP(6)
                AND u.bloqueado=FALSE AND u.email_verificado=TRUE
                """).params(usuarioId, sessaoId).query(UsuarioDto.class).optional()
                .orElseThrow(() -> falha(UNAUTHORIZED, "Sessão expirada"));
    }

    private void registrarFalha(Usuario usuario) {
        int falhas = usuario.limitadoAte() == null ? usuario.tentativas() + 1 : 1;
        if (usuario.perfil().equals("CLIENTE") && falhas >= 5) {
            banco.sql("UPDATE usuario SET tentativas=0, limitado_ate=UTC_TIMESTAMP(6) + INTERVAL 15 MINUTE WHERE id=?")
                    .param(usuario.id()).update();
            return;
        }
        boolean bloquear = !usuario.perfil().equals("CLIENTE") && falhas >= 3;
        banco.sql("UPDATE usuario SET tentativas=?, bloqueado=?, limitado_ate=NULL WHERE id=?")
                .params(falhas, bloquear, usuario.id()).update();
        if (bloquear) {
            revogarSessoes(usuario.id());
        }
    }

    private void emitirCodigo(Usuario usuario, String finalidade) {
        Desafio ultimo = buscarDesafio(usuario.id(), finalidade);
        if (ultimo != null && ultimo.criadoEm().isAfter(Instant.now().minusSeconds(60))) {
            return;
        }
        banco.sql("UPDATE codigo_verificacao SET consumido_em=UTC_TIMESTAMP(6) WHERE usuario_id=? AND finalidade=? AND consumido_em IS NULL")
                .params(usuario.id(), finalidade).update();
        String codigo = String.format(Locale.ROOT, "%06d", ALEATORIO.nextInt(1_000_000));
        banco.sql("""
                INSERT INTO codigo_verificacao(usuario_id,finalidade,codigo_hash,expira_em)
                VALUES (?,?,?,UTC_TIMESTAMP(6) + INTERVAL 10 MINUTE)
                """).params(usuario.id(), finalidade, senhas.encode(codigo)).update();
        eventos.publishEvent(new CodigoEmail(usuario.email(), finalidade, codigo));
    }

    private Usuario validarCodigo(String email, String codigo, String finalidade) {
        Usuario usuario = buscar(normalizarEmail(email));
        Desafio desafio = usuario == null ? null : buscarDesafio(usuario.id(), finalidade);
        if (desafio == null || desafio.tentativas() >= 5 || !desafio.expiracao().isAfter(Instant.now())) {
            throw falha(BAD_REQUEST, "Código inválido ou expirado");
        }
        if (!senhas.matches(codigo, desafio.hash())) {
            banco.sql("UPDATE codigo_verificacao SET tentativas=tentativas+1 WHERE id=?").param(desafio.id()).update();
            throw falha(BAD_REQUEST, "Código inválido ou expirado");
        }
        banco.sql("UPDATE codigo_verificacao SET consumido_em=UTC_TIMESTAMP(6) WHERE id=?").param(desafio.id()).update();
        return usuario;
    }

    private Usuario buscar(String email) {
        return banco.sql("SELECT * FROM usuario WHERE email=? FOR UPDATE").param(email)
                .query((rs, row) -> new Usuario(rs.getLong("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("perfil"), rs.getString("senha_hash"), rs.getBoolean("email_verificado"),
                        rs.getBoolean("bloqueado"), rs.getInt("tentativas"),
                        rs.getTimestamp("limitado_ate") == null ? null : rs.getTimestamp("limitado_ate").toInstant()))
                .optional().orElse(null);
    }

    private Desafio buscarDesafio(long usuario, String finalidade) {
        return banco.sql("""
                SELECT id,codigo_hash,tentativas,expira_em,criado_em FROM codigo_verificacao
                WHERE usuario_id=? AND finalidade=? AND consumido_em IS NULL ORDER BY id DESC LIMIT 1
                """).params(usuario, finalidade).query((rs, row) -> new Desafio(rs.getLong("id"), rs.getString("codigo_hash"),
                        rs.getInt("tentativas"), rs.getTimestamp("expira_em").toInstant(), rs.getTimestamp("criado_em").toInstant()))
                .optional().orElse(null);
    }

    private Sessao buscarSessao(String hash) {
        return banco.sql("SELECT id,usuario_id,expira_em FROM sessao WHERE refresh_hash=? AND revogada_em IS NULL FOR UPDATE")
                .param(hash).query((rs, row) -> new Sessao(rs.getLong("id"), rs.getLong("usuario_id"),
                        rs.getTimestamp("expira_em").toInstant())).optional().orElse(null);
    }

    private Tokens tokens(UsuarioDto usuario, Sessao sessao, String refresh) {
        Instant agora = Instant.now();
        Instant expira = agora.plusSeconds(900);
        if (sessao.expiraEm().isBefore(expira)) {
            expira = sessao.expiraEm();
        }
        var claims = JwtClaimsSet.builder().issuer(Seguranca.EMISSOR).subject(Long.toString(usuario.id()))
                .issuedAt(agora).expiresAt(expira).claim("sid", Long.toString(sessao.id())).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String access = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Tokens(access, refresh, Duration.between(agora, expira).toSeconds(), usuario);
    }

    static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }

    private static ResponseStatusException falha(org.springframework.http.HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
