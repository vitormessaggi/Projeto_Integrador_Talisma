package br.com.talisma.api.imoveisapi.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
    private final ImovelRepository imoveis;
    private final OfertaRepository ofertas;
    private final FotoRepository fotos;

    public CatalogoService(ImovelRepository imoveis, OfertaRepository ofertas, FotoRepository fotos) {
        this.imoveis = imoveis;
        this.ofertas = ofertas;
        this.fotos = fotos;
    }

    public record OfertaDto(Long id, String finalidade, BigDecimal preco) {}
    public record ImovelDto(Long id, String titulo, String cidade, String bairro, String uf,
                            String tipo, Integer quartos, Integer banheiros, Integer vagas,
                            BigDecimal areaTotal, List<OfertaDto> ofertas, List<String> fotos) {}
    public @io.swagger.v3.oas.annotations.media.Schema(name = "DetalheCatalogo") record Detalhe(ImovelDto imovel, String descricao, BigDecimal latitude, BigDecimal longitude) {}
    public record Filtros(List<String> tipos, List<String> cidades) {}
    public record Pagina(List<ImovelDto> itens, int pagina, int tamanho, long totalElementos, int totalPaginas) {}

    public Pagina buscar(String cidade, String tipo, Integer quartos, Boolean destaque,
                         String finalidade, BigDecimal min, BigDecimal max, int page, int size) {
        validarPagina(page, size);
        if ((min != null && min.signum() < 0)
                || (max != null && max.signum() < 0) || (min != null && max != null && min.compareTo(max) > 0)
                || (quartos != null && quartos < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros inválidos");
        }
        if (finalidade != null && !Set.of("VENDA", "LOCACAO").contains(finalidade)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalidade inválida");
        }
        var resultado = imoveis.buscar(cidade, tipo, quartos, destaque, finalidade, min, max,
                PageRequest.of(page, size, Sort.by("id")));
        var itens = montarResumos(resultado.getContent());
        return new Pagina(itens, page, size, resultado.getTotalElements(), resultado.getTotalPages());
    }

    public static void validarPagina(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida");
        }
    }

    public Pagina favoritos(long usuarioId, int pagina, int tamanho) {
        validarPagina(pagina, tamanho);
        var resultado = imoveis.favoritos(usuarioId, PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina(montarResumos(resultado.getContent()), pagina, tamanho,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    public Detalhe detalhar(long id) {
        var imovel = imoveis.findByIdAndStatus(id, "PUBLICADO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imóvel indisponível"));
        return new Detalhe(montarResumos(List.of(imovel)).getFirst(), imovel.descricao, imovel.latitude, imovel.longitude);
    }

    public Filtros filtros() {
        return new Filtros(imoveis.tiposPublicados(), imoveis.cidadesPublicadas());
    }

    private List<ImovelDto> montarResumos(List<Imovel> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        var ids = lista.stream().map(imovel -> imovel.id).toList();
        Map<Long, List<OfertaDto>> ofertasPorImovel = ofertas.findByImovelIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(oferta -> oferta.imovelId,
                        Collectors.mapping(oferta -> new OfertaDto(oferta.id, oferta.finalidade, oferta.preco), Collectors.toList())));
        Map<Long, List<String>> fotosPorImovel = fotos.findByImovelIdInOrderByOrdemAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(foto -> foto.imovelId,
                        Collectors.mapping(foto -> "/api/v1/fotos/" + foto.id, Collectors.toList())));
        return lista.stream().map(imovel -> new ImovelDto(imovel.id, imovel.titulo, imovel.cidade, imovel.bairro,
                imovel.uf, imovel.tipo, imovel.quartos, imovel.banheiros, imovel.vagas, imovel.areaTotal,
                ofertasPorImovel.getOrDefault(imovel.id, List.of()), fotosPorImovel.getOrDefault(imovel.id, List.of()))).toList();
    }
}
