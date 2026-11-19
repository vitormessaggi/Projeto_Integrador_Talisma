package br.com.talisma.api.imoveisapi.identidade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class PrivacidadeController {
    private final JdbcClient banco;
    private final IdentidadeService identidade;
    private final BCryptPasswordEncoder senhas;
    public PrivacidadeController(JdbcClient banco, IdentidadeService identidade, BCryptPasswordEncoder senhas) {
        this.banco = banco; this.identidade = identidade; this.senhas = senhas;
    }
    public record Pedido(@NotBlank @Pattern(regexp="ACESSO|EXCLUSAO") String tipo,
                         @NotBlank @Size(max=72) String senha) {}
    public record Senha(@NotBlank @Size(max=72) String senha) {}
    public record Aceite(@NotNull @AssertTrue Boolean aceiteTermos, @NotBlank String versaoTermos,
                         @NotBlank String versaoPrivacidade, @Pattern(regexp="WEB|ANDROID") @NotBlank String origem) {}
    public record Resposta(@NotBlank @Pattern(regexp="CONCLUIDA|INDEFERIDA") String status,
                           @NotBlank @Size(max=2000) String resposta) {}

    @GetMapping("/usuarios/me/privacidade")
    public Map<String,Object> listar(@AuthenticationPrincipal Jwt jwt) {
        long id = identidade.usuarioAtual(jwt).id();
        return Map.of("aceiteAtual", banco.sql("SELECT EXISTS(SELECT 1 FROM aceite_legal WHERE usuario_id=? AND versao_termos=? AND versao_privacidade=?)")
                .params(id,DocumentosLegais.VERSAO,DocumentosLegais.VERSAO).query(Boolean.class).single(),
                "solicitacoes", banco.sql("SELECT protocolo,tipo,status,resposta,criado_em,concluido_em FROM solicitacao_privacidade WHERE usuario_id=? ORDER BY criado_em DESC LIMIT 100")
                        .param(id).query().listOfRows());
    }
    @PostMapping("/usuarios/me/aceites")
    @ResponseStatus(NO_CONTENT)
    public void aceitar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Aceite dados) {
        DocumentosLegais.registrar(banco,identidade.usuarioAtual(jwt).id(),dados.versaoTermos(),dados.versaoPrivacidade(),dados.origem());
    }

    @PostMapping("/usuarios/me/privacidade/solicitacoes")
    @ResponseStatus(CREATED)
    public Map<String,Object> solicitar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Pedido dados) {
        var ator = identidade.usuarioAtual(jwt);
        confirmar(ator.id(),dados.senha());
        var existente = banco.sql("SELECT protocolo,status FROM solicitacao_privacidade WHERE usuario_id=? AND tipo=? AND status='PENDENTE'")
                .params(ator.id(),dados.tipo()).query().listOfRows();
        if (!existente.isEmpty()) return existente.getFirst();
        String protocolo = UUID.randomUUID().toString();
        banco.sql("INSERT INTO solicitacao_privacidade(protocolo,usuario_id,tipo) VALUES (?,?,?)").params(protocolo,ator.id(),dados.tipo()).update();
        if (dados.tipo().equals("EXCLUSAO") && ator.perfil().equals("CLIENTE")) {
            banco.sql("UPDATE solicitacao_privacidade SET usuario_excluido_id=? WHERE protocolo=?").params(ator.id(),protocolo).update();
            excluirCliente(ator.id());
            banco.sql("UPDATE solicitacao_privacidade SET status='CONCLUIDA',resposta='Conta e dados relacionados excluídos do ambiente ativo.',concluido_em=UTC_TIMESTAMP(6) WHERE protocolo=?")
                    .param(protocolo).update();
            return Map.of("protocolo",protocolo,"status","CONCLUIDA");
        }
        return Map.of("protocolo",protocolo,"status","PENDENTE");
    }

    @PostMapping("/usuarios/me/privacidade/exportacao")
    public ResponseEntity<Map<String,Object>> exportar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Senha dados) {
        long id = identidade.usuarioAtual(jwt).id();
        confirmar(id,dados.senha());
        var resultado = new LinkedHashMap<String,Object>();
        resultado.put("geradoEm",java.time.Instant.now().toString());
        resultado.put("perfil",banco.sql("SELECT id,nome,email,perfil,telefone,email_verificado,criado_em FROM usuario WHERE id=?").param(id).query().singleRow());
        resultado.put("documentosLegados",banco.sql("SELECT cpf,rg,orgao_emissor,uf FROM documento_cliente WHERE usuario_id=?").param(id).query().listOfRows());
        resultado.put("favoritos",banco.sql("SELECT imovel_id FROM favorito WHERE usuario_id=?").param(id).query().listOfRows());
        resultado.put("contatos",banco.sql("SELECT id,imovel_id,oferta_id,assunto,mensagem,telefone,status,criado_em FROM lead_contato WHERE cliente_id=?").param(id).query().listOfRows());
        resultado.put("visitas",banco.sql("SELECT v.id,v.data_preferida,v.periodo,v.status FROM visita v JOIN lead_contato l ON l.id=v.lead_id WHERE l.cliente_id=?").param(id).query().listOfRows());
        resultado.put("aceites",banco.sql("SELECT versao_termos,versao_privacidade,hash_termos,hash_privacidade,origem,criado_em FROM aceite_legal WHERE usuario_id=?").param(id).query().listOfRows());
        resultado.put("solicitacoes",banco.sql("SELECT protocolo,tipo,status,resposta,criado_em,concluido_em FROM solicitacao_privacidade WHERE usuario_id=?").param(id).query().listOfRows());
        banco.sql("UPDATE solicitacao_privacidade SET status='CONCLUIDA',resposta='Cópia disponibilizada ao titular autenticado.',concluido_em=UTC_TIMESTAMP(6) WHERE usuario_id=? AND tipo='ACESSO' AND status='PENDENTE'").param(id).update();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Content-Disposition","attachment; filename=dados-talisma.json").body(resultado);
    }

    @GetMapping("/admin/privacidade/solicitacoes")
    public List<Map<String,Object>> fila(@AuthenticationPrincipal Jwt jwt) {
        UsuariosService.exigir(identidade.usuarioAtual(jwt),"ADMIN");
        return banco.sql("SELECT protocolo,usuario_id,tipo,status,resposta,criado_em,concluido_em FROM solicitacao_privacidade ORDER BY criado_em DESC LIMIT 200").query().listOfRows();
    }
    @GetMapping("/admin/privacidade/exclusoes")
    public ResponseEntity<List<Map<String,Object>>> exclusoes(@AuthenticationPrincipal Jwt jwt) {
        UsuariosService.exigir(identidade.usuarioAtual(jwt),"ADMIN");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(banco.sql("SELECT protocolo,usuario_excluido_id,concluido_em FROM solicitacao_privacidade WHERE usuario_excluido_id IS NOT NULL AND status='CONCLUIDA'").query().listOfRows());
    }
    @PatchMapping("/admin/privacidade/solicitacoes/{protocolo}")
    @ResponseStatus(NO_CONTENT)
    public void responder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID protocolo, @Valid @RequestBody Resposta dados) {
        UsuariosService.exigir(identidade.usuarioAtual(jwt),"ADMIN");
        if (banco.sql("UPDATE solicitacao_privacidade SET status=?,resposta=?,concluido_em=UTC_TIMESTAMP(6) WHERE protocolo=? AND status='PENDENTE'")
                .params(dados.status(),dados.resposta(),protocolo.toString()).update()==0)
            throw new ResponseStatusException(CONFLICT,"Solicitação inexistente ou já concluída");
    }
    private void confirmar(long id, String senha) {
        String hash = banco.sql("SELECT senha_hash FROM usuario WHERE id=? FOR UPDATE").param(id).query(String.class).single();
        if (senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !senhas.matches(senha,hash))
            throw new ResponseStatusException(BAD_REQUEST,"Senha atual incorreta");
    }
    private void excluirCliente(long id) {
        // Remove notificações de todos os destinatários quando contêm referência ao atendimento excluído.
        banco.sql("DELETE n FROM notificacao n LEFT JOIN lead_contato l ON l.id=n.lead_id WHERE n.usuario_id=? OR l.cliente_id=?").params(id,id).update();
        banco.sql("DELETE v FROM visita v JOIN lead_contato l ON l.id=v.lead_id WHERE l.cliente_id=?").param(id).update();
        banco.sql("DELETE FROM lead_contato WHERE cliente_id=?").param(id).update();
        for (String tabela : List.of("favorito","documento_cliente","aceite_legal","codigo_verificacao","sessao"))
            banco.sql("DELETE FROM " + tabela + " WHERE usuario_id=?").param(id).update();
        banco.sql("DELETE FROM usuario WHERE id=? AND perfil='CLIENTE'").param(id).update();
    }
}
