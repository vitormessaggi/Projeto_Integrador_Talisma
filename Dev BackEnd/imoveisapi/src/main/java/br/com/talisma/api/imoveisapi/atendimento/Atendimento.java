package br.com.talisma.api.imoveisapi.atendimento;

import static org.springframework.http.HttpStatus.*;

import br.com.talisma.api.imoveisapi.catalogo.CatalogoService;
import br.com.talisma.api.imoveisapi.identidade.IdentidadeService.UsuarioDto;
import br.com.talisma.api.imoveisapi.identidade.UsuariosService;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
class Atendimento {
    private final JdbcClient banco;
    Atendimento(JdbcClient banco) { this.banco = banco; }

    record Contato(@Positive Long imovelId, @Positive Long ofertaId, @NotBlank @Size(max = 160) String assunto,
                   @NotBlank @Size(max = 2000) String mensagem, @NotBlank @Pattern(regexp = "[0-9+() -]{8,20}") String telefone) {}
    record PedidoVisita(@Positive long imovelId, @Positive Long ofertaId, @NotNull LocalDate data,
                        @NotBlank @Pattern(regexp = "MANHA|TARDE|NOITE") String periodo,
                        @NotBlank @Pattern(regexp = "[0-9+() -]{8,20}") String telefone, @Size(max = 2000) String mensagem) {}
    record Lead(long id, long clienteId, Long corretorId, Long imovelId, Long ofertaId,
                String assunto, String mensagem, String telefone, String status) {}
    record Visita(long id, long leadId, long imovelId, LocalDate dataPreferida, String periodo, String status) {}
    record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalElementos, long totalPaginas) {}

    Lead contato(UsuarioDto cliente, Contato dados) {
        UsuariosService.exigir(cliente, "CLIENTE");
        Long corretor = responsavel(dados.imovelId(), dados.ofertaId());
        var chave = new GeneratedKeyHolder();
        banco.sql("""
                INSERT INTO lead_contato(cliente_id,corretor_id,imovel_id,oferta_id,assunto,mensagem,telefone)
                VALUES (?,?,?,?,?,?,?)
                """).params(cliente.id(), corretor, dados.imovelId(), dados.ofertaId(),
                dados.assunto().strip(), dados.mensagem(), dados.telefone()).update(chave, "id");
        long id = chave.getKey().longValue();
        if (corretor != null) {
            notificar(corretor, id, "Novo atendimento");
        } else {
            var admins = banco.sql("SELECT id FROM usuario WHERE perfil='ADMIN'").query(Long.class).list();
            for (long admin : admins) {
                notificar(admin, id, "Contato aguardando atribuição");
            }
        }
        return lead(id, false);
    }

    Visita solicitar(UsuarioDto cliente, PedidoVisita dados) {
        if (!dados.data().isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) {
            throw erro(BAD_REQUEST, "Escolha uma data a partir de amanhã");
        }
        Lead lead = contato(cliente, new Contato(dados.imovelId(), dados.ofertaId(), "Solicitação de visita",
                dados.mensagem() == null || dados.mensagem().isBlank() ? "Solicitação de visita ao imóvel" : dados.mensagem(),
                dados.telefone()));
        var chave = new GeneratedKeyHolder();
        banco.sql("INSERT INTO visita(lead_id,data_preferida,periodo) VALUES (?,?,?)")
                .params(lead.id(), dados.data(), dados.periodo()).update(chave, "id");
        return new Visita(chave.getKey().longValue(), lead.id(), dados.imovelId(), dados.data(), dados.periodo(), "PENDENTE");
    }

    Pagina<Lead> leads(UsuarioDto ator, int pagina, int tamanho) {
        CatalogoService.validarPagina(pagina, tamanho);
        String condicao = acesso(ator);
        var total = banco.sql("SELECT COUNT(*) FROM lead_contato l WHERE " + condicao)
                .params(ator.id()).query(Long.class).single();
        var itens = banco.sql("""
                SELECT l.id,l.cliente_id,l.corretor_id,l.imovel_id,l.oferta_id,l.assunto,l.mensagem,l.telefone,l.status
                FROM lead_contato l WHERE
                """ + condicao + " ORDER BY l.id DESC LIMIT ? OFFSET ?").params(ator.id(), tamanho, (long) pagina * tamanho)
                .query(Lead.class).list();
        return pagina(itens, pagina, tamanho, total);
    }

    Pagina<Visita> visitas(UsuarioDto ator, int pagina, int tamanho) {
        CatalogoService.validarPagina(pagina, tamanho);
        String de = " FROM visita v JOIN lead_contato l ON l.id=v.lead_id WHERE " + acesso(ator);
        var total = banco.sql("SELECT COUNT(*)" + de).params(ator.id()).query(Long.class).single();
        var itens = banco.sql("SELECT v.id,v.lead_id,l.imovel_id,v.data_preferida,v.periodo,v.status" + de
                + " ORDER BY v.id DESC LIMIT ? OFFSET ?").params(ator.id(), tamanho, (long) pagina * tamanho)
                .query(Visita.class).list();
        return pagina(itens, pagina, tamanho, total);
    }

    void atribuir(UsuarioDto ator, long id, long corretorId) {
        UsuariosService.exigir(ator, "ADMIN");
        var lead = lead(id, true);
        if (lead.imovelId() != null) {
            throw erro(CONFLICT, "O corretor do contato é definido pelo imóvel");
        }
        boolean existe = banco.sql("SELECT EXISTS(SELECT 1 FROM usuario WHERE id=? AND perfil='CORRETOR' AND bloqueado=FALSE)")
                .param(corretorId).query(Boolean.class).single();
        if (!existe) {
            throw erro(NOT_FOUND, "Corretor não encontrado");
        }
        banco.sql("UPDATE lead_contato SET corretor_id=? WHERE id=?").params(corretorId, id).update();
        notificar(corretorId, id, "Atendimento atribuído");
    }

    void statusLead(UsuarioDto ator, long id, String status) {
        if (ator.perfil().equals("CLIENTE")) {
            throw erro(FORBIDDEN, "Operação não permitida");
        }
        var atual = lead(id, true);
        if (!ator.perfil().equals("ADMIN") && !Long.valueOf(ator.id()).equals(atual.corretorId())) {
            throw erro(NOT_FOUND, "Contato não encontrado");
        }
        if (!List.of("NOVO", "EM_ATENDIMENTO", "ENCERRADO").contains(status)) {
            throw erro(BAD_REQUEST, "Status inválido");
        }
        banco.sql("UPDATE lead_contato SET status=? WHERE id=?").params(status, id).update();
    }

    void statusVisita(UsuarioDto ator, long id, String proximo) {
        Visita atual = banco.sql("""
                SELECT v.id,v.lead_id,l.imovel_id,v.data_preferida,v.periodo,v.status
                FROM visita v JOIN lead_contato l ON l.id=v.lead_id WHERE v.id=? AND
                """ + acesso(ator) + " FOR UPDATE").params(id, ator.id()).query(Visita.class).optional()
                .orElseThrow(() -> erro(NOT_FOUND, "Visita não encontrada"));
        boolean interno = !ator.perfil().equals("CLIENTE");
        boolean valido = switch (proximo) {
            case "CONFIRMADA" -> interno && atual.status().equals("PENDENTE");
            case "CONCLUIDA" -> interno && atual.status().equals("CONFIRMADA");
            case "CANCELADA" -> List.of("PENDENTE", "CONFIRMADA").contains(atual.status());
            default -> false;
        };
        if (!valido) {
            throw erro(CONFLICT, "Transição indisponível");
        }
        banco.sql("UPDATE visita SET status=?,versao=versao+1 WHERE id=?").params(proximo, id).update();
        Lead lead = lead(atual.leadId(), false);
        Long destino = interno ? Long.valueOf(lead.clienteId()) : lead.corretorId();
        if (destino != null) {
            notificar(destino, lead.id(), "Visita " + proximo.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private Long responsavel(Long imovel, Long oferta) {
        if (imovel == null) {
            if (oferta != null) {
                throw erro(BAD_REQUEST, "Uma oferta precisa estar vinculada ao imóvel");
            }
            return null;
        }
        Long corretor = banco.sql("SELECT corretor_id FROM imovel WHERE id=? AND status='PUBLICADO' FOR SHARE")
                .param(imovel).query(Long.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Imóvel indisponível"));
        if (oferta != null && banco.sql("SELECT id FROM oferta WHERE id=? AND imovel_id=?")
                .params(oferta, imovel).query(Long.class).optional().isEmpty()) {
            throw erro(BAD_REQUEST, "Oferta incompatível com o imóvel");
        }
        return corretor;
    }

    private Lead lead(long id, boolean bloquear) {
        String sql = "SELECT id,cliente_id,corretor_id,imovel_id,oferta_id,assunto,mensagem,telefone,status FROM lead_contato WHERE id=?";
        return banco.sql(sql + (bloquear ? " FOR UPDATE" : "")).param(id).query(Lead.class).optional()
                .orElseThrow(() -> erro(NOT_FOUND, "Contato não encontrado"));
    }

    private void notificar(long usuario, long lead, String assunto) {
        banco.sql("INSERT INTO notificacao(usuario_id,lead_id,assunto,corpo) VALUES (?,?,?,?)")
                .params(usuario, lead, assunto, "Consulte o atendimento #" + lead + " no painel Talismã Imóveis.").update();
    }

    private static String acesso(UsuarioDto ator) {
        return switch (ator.perfil()) {
            case "ADMIN" -> "? > 0";
            case "CORRETOR" -> "l.corretor_id=?";
            case "CLIENTE" -> "l.cliente_id=?";
            default -> throw erro(FORBIDDEN, "Operação não permitida");
        };
    }

    private static <T> Pagina<T> pagina(List<T> itens, int pagina, int tamanho, long total) {
        return new Pagina<>(itens, pagina, tamanho, total, (total + tamanho - 1) / tamanho);
    }

    private static ResponseStatusException erro(org.springframework.http.HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
