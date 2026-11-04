package br.com.talisma.api.imoveisapi.gestao;

import static org.springframework.http.HttpStatus.*;

import br.com.talisma.api.imoveisapi.catalogo.CatalogoService;
import br.com.talisma.api.imoveisapi.identidade.IdentidadeService.UsuarioDto;
import br.com.talisma.api.imoveisapi.identidade.UsuariosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GestaoImoveis {
    private final JdbcClient banco;

    public GestaoImoveis(JdbcClient banco) {
        this.banco = banco;
    }

    public record ProprietarioInput(@NotBlank @Size(max = 160) String nome,
                                    @NotBlank @Pattern(regexp = "[0-9+() -]{8,20}") String telefone) {}
    public record Proprietario(long id, String nome, String telefone) {}
    public record OfertaInput(@NotBlank @Pattern(regexp = "VENDA|LOCACAO") String finalidade,
                              @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal preco) {}
    public record ImovelInput(@Positive long proprietarioId, @NotBlank @Size(max = 180) String titulo,
                              @NotBlank @Size(max = 10000) String descricao,
                              @NotBlank @Pattern(regexp = "CASA|APARTAMENTO|TERRENO") String tipo,
                              @NotBlank @Size(max = 120) String cidade, @NotBlank @Size(max = 120) String bairro,
                              @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf,
                              @NotBlank @Pattern(regexp = "[0-9]{8}") String cep,
                              @NotBlank @Size(max = 200) String logradouro, @NotBlank @Size(max = 20) String numero,
                              @PositiveOrZero int quartos, @PositiveOrZero int banheiros, @PositiveOrZero int vagas,
                              @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal areaTotal,
                              @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal areaConstruida,
                              boolean mobiliado, boolean destaque,
                              @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                              @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                              @NotNull @Size(min = 1, max = 2) List<@NotNull @Valid OfertaInput> ofertas) {}
    public record Resumo(long id, long proprietarioId, long corretorId, String titulo, String status) {}
    public @io.swagger.v3.oas.annotations.media.Schema(name = "DetalheGestao") record Detalhe(Resumo imovel, ImovelInput dados, List<Long> fotos) {}
    public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalElementos, long totalPaginas) {}

    Pagina<Proprietario> proprietarios(UsuarioDto ator, int pagina, int tamanho) {
        interno(ator);
        CatalogoService.validarPagina(pagina, tamanho);
        long corretor = filtroCorretor(ator);
        long total = banco.sql("SELECT COUNT(*) FROM proprietario WHERE (?=0 OR corretor_id=?)")
                .params(corretor, corretor).query(Long.class).single();
        var itens = banco.sql("""
                SELECT id,nome,telefone FROM proprietario WHERE (?=0 OR corretor_id=?)
                ORDER BY id LIMIT ? OFFSET ?
                """).params(corretor, corretor, tamanho, (long) pagina * tamanho).query(Proprietario.class).list();
        return pagina(itens, pagina, tamanho, total);
    }

    Proprietario salvarProprietario(UsuarioDto ator, Long id, ProprietarioInput dados) {
        interno(ator);
        if (id == null) {
            var chave = new GeneratedKeyHolder();
            banco.sql("INSERT INTO proprietario(corretor_id,nome,telefone) VALUES (?,?,?)")
                    .params(ator.id(), dados.nome().strip(), dados.telefone()).update(chave, "id");
            id = chave.getKey().longValue();
        } else {
            long corretor = filtroCorretor(ator);
            int alterados = banco.sql("UPDATE proprietario SET nome=?,telefone=? WHERE id=? AND (?=0 OR corretor_id=?)")
                    .params(dados.nome().strip(), dados.telefone(), id, corretor, corretor).update();
            if (alterados == 0) {
                throw erro(NOT_FOUND, "Proprietário não encontrado");
            }
        }
        return new Proprietario(id, dados.nome().strip(), dados.telefone());
    }

    Pagina<Resumo> listar(UsuarioDto ator, String status, int pagina, int tamanho) {
        interno(ator);
        CatalogoService.validarPagina(pagina, tamanho);
        long corretor = filtroCorretor(ator);
        String filtro = status == null ? "" : status;
        long total = banco.sql("SELECT COUNT(*) FROM imovel WHERE (?=0 OR corretor_id=?) AND (?='' OR status=?)")
                .params(corretor, corretor, filtro, filtro).query(Long.class).single();
        var itens = banco.sql("""
                SELECT id,proprietario_id,corretor_id,titulo,status FROM imovel
                WHERE (?=0 OR corretor_id=?) AND (?='' OR status=?)
                ORDER BY id DESC LIMIT ? OFFSET ?
                """).params(corretor, corretor, filtro, filtro, tamanho, (long) pagina * tamanho).query(Resumo.class).list();
        return pagina(itens, pagina, tamanho, total);
    }

    Detalhe detalhe(UsuarioDto ator, long id) {
        Resumo resumo = carregar(ator, id);
        List<OfertaInput> ofertas = banco.sql("SELECT finalidade,preco FROM oferta WHERE imovel_id=? ORDER BY id")
                .param(id).query(OfertaInput.class).list();
        ImovelInput dados = banco.sql("SELECT * FROM imovel WHERE id=?").param(id)
                .query((rs, row) -> new ImovelInput(resumo.proprietarioId(), resumo.titulo(), rs.getString("descricao"),
                        rs.getString("tipo"), rs.getString("cidade"), rs.getString("bairro"), rs.getString("uf"),
                        rs.getString("cep"), rs.getString("logradouro"), rs.getString("numero"),
                        rs.getInt("quartos"), rs.getInt("banheiros"), rs.getInt("vagas"), rs.getBigDecimal("area_total"),
                        rs.getBigDecimal("area_construida"), rs.getBoolean("mobiliado"), rs.getBoolean("destaque"),
                        rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"), ofertas)).single();
        var fotos = banco.sql("SELECT id FROM foto WHERE imovel_id=? ORDER BY ordem,id").param(id).query(Long.class).list();
        return new Detalhe(resumo, dados, fotos);
    }

    Detalhe salvar(UsuarioDto ator, Long id, ImovelInput dados) {
        interno(ator);
        Resumo atual = id == null ? null : carregar(ator, id);
        if (atual != null && atual.status().equals("ARQUIVADO")) {
            throw erro(CONFLICT, "Imóvel arquivado");
        }
        long dono = banco.sql("SELECT corretor_id FROM proprietario WHERE id=?").param(dados.proprietarioId())
                .query(Long.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Proprietário não encontrado"));
        long corretor = atual == null ? dono : atual.corretorId();
        if (dono != corretor || (ator.perfil().equals("CORRETOR") && corretor != ator.id())) {
            throw erro(NOT_FOUND, "Proprietário não encontrado");
        }
        var finalidades = new HashSet<String>();
        for (OfertaInput oferta : dados.ofertas()) {
            if (!finalidades.add(oferta.finalidade())) {
                throw erro(BAD_REQUEST, "Informe apenas um preço por finalidade");
            }
        }

        String status = atual == null ? "RASCUNHO" : switch (atual.status()) {
            case "PUBLICADO", "PENDENTE" -> "PENDENTE";
            default -> "RASCUNHO";
        };
        String campos = """
                proprietario_id=?,corretor_id=?,titulo=?,descricao=?,tipo=?,status=?,cidade=?,bairro=?,uf=?,
                cep=?,logradouro=?,numero=?,quartos=?,banheiros=?,vagas=?,area_total=?,area_construida=?,
                mobiliado=?,destaque=?,latitude=?,longitude=?
                """;
        Object[] valores = {dados.proprietarioId(), corretor, dados.titulo().strip(), dados.descricao(), dados.tipo(), status,
                dados.cidade().strip(), dados.bairro().strip(), dados.uf(), dados.cep(), dados.logradouro().strip(),
                dados.numero(), dados.quartos(), dados.banheiros(), dados.vagas(), dados.areaTotal(), dados.areaConstruida(),
                dados.mobiliado(), dados.destaque(), dados.latitude(), dados.longitude()};
        if (atual == null) {
            var chave = new GeneratedKeyHolder();
            banco.sql("INSERT INTO imovel SET " + campos).params(valores).update(chave, "id");
            id = chave.getKey().longValue();
        } else {
            banco.sql("UPDATE imovel SET " + campos + ", versao=versao+1 WHERE id=?")
                    .params(valores).param(valores.length + 1, id).update();
        }
        for (OfertaInput oferta : dados.ofertas()) {
            banco.sql("""
                    INSERT INTO oferta(imovel_id,finalidade,preco) VALUES (?,?,?)
                    ON DUPLICATE KEY UPDATE preco=?
                    """).params(id, oferta.finalidade(), oferta.preco(), oferta.preco()).update();
        }
        for (String finalidade : List.of("VENDA", "LOCACAO")) {
            if (!finalidades.contains(finalidade)) {
                try {
                    banco.sql("DELETE FROM oferta WHERE imovel_id=? AND finalidade=?").params(id, finalidade).update();
                } catch (DataIntegrityViolationException erro) {
                    throw erro(CONFLICT, "A oferta já está vinculada a um atendimento");
                }
            }
        }
        historico(id, ator.id(), atual == null ? "CADASTRO" : "EDICAO", null);
        return detalhe(ator, id);
    }

    void transicao(UsuarioDto ator, long id, String proximo, String motivo) {
        Resumo atual = carregar(ator, id);
        boolean admin = ator.perfil().equals("ADMIN");
        boolean permitido = switch (proximo) {
            case "PENDENTE" -> List.of("RASCUNHO", "REJEITADO").contains(atual.status());
            case "PUBLICADO", "REJEITADO" -> admin && atual.status().equals("PENDENTE");
            case "ARQUIVADO" -> !atual.status().equals("ARQUIVADO");
            default -> false;
        };
        if (!permitido) {
            throw erro(CONFLICT, "Transição indisponível");
        }
        banco.sql("UPDATE imovel SET status=?,versao=versao+1 WHERE id=?").params(proximo, id).update();
        historico(id, ator.id(), proximo, motivo);
    }

    Resumo carregar(UsuarioDto ator, long id) {
        interno(ator);
        long corretor = filtroCorretor(ator);
        return banco.sql("""
                SELECT id,proprietario_id,corretor_id,titulo,status FROM imovel
                WHERE id=? AND (?=0 OR corretor_id=?) FOR UPDATE
                """).params(id, corretor, corretor).query(Resumo.class).optional()
                .orElseThrow(() -> erro(NOT_FOUND, "Imóvel não encontrado"));
    }

    void fotoAlterada(UsuarioDto ator, Resumo imovel) {
        if (imovel.status().equals("PUBLICADO")) {
            banco.sql("UPDATE imovel SET status='PENDENTE',versao=versao+1 WHERE id=?").param(imovel.id()).update();
        }
        historico(imovel.id(), ator.id(), "FOTOS_ALTERADAS", null);
    }

    private void historico(long imovel, long ator, String acao, String motivo) {
        banco.sql("INSERT INTO historico_imovel(imovel_id,autor_id,acao,motivo) VALUES (?,?,?,?)")
                .params(imovel, ator, acao, motivo).update();
    }

    private static void interno(UsuarioDto ator) {
        if (!ator.perfil().equals("ADMIN")) {
            UsuariosService.exigir(ator, "CORRETOR");
        }
    }

    private static long filtroCorretor(UsuarioDto ator) {
        return ator.perfil().equals("ADMIN") ? 0 : ator.id();
    }

    private static <T> Pagina<T> pagina(List<T> itens, int pagina, int tamanho, long total) {
        return new Pagina<>(itens, pagina, tamanho, total, (total + tamanho - 1) / tamanho);
    }

    static ResponseStatusException erro(org.springframework.http.HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
