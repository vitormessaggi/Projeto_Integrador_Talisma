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
    private final org.springframework.jdbc.core.simple.JdbcClient banco;

    public CatalogoService(ImovelRepository imoveis, OfertaRepository ofertas, FotoRepository fotos, org.springframework.jdbc.core.simple.JdbcClient banco) {
        this.imoveis = imoveis;
        this.ofertas = ofertas;
        this.fotos = fotos;
        this.banco = banco;
    }

    public record OfertaDto(Long id, String finalidade, BigDecimal preco) {}
    public record ImovelDto(Long id, String titulo, String cidade, String bairro, String uf,
                            String tipo, Integer quartos, Integer banheiros, Integer vagas,
                            BigDecimal areaTotal, List<OfertaDto> ofertas, List<String> fotos, Double distanciaKm) {}
    public @io.swagger.v3.oas.annotations.media.Schema(name = "DetalheCatalogo") record Detalhe(ImovelDto imovel, String descricao, BigDecimal latitude, BigDecimal longitude) {}
    public record Filtros(List<String> tipos, List<String> cidades) {}
    public record Pagina(List<ImovelDto> itens, int pagina, int tamanho, long totalElementos, int totalPaginas) {}

    public Pagina buscar(String cidade, String tipo, Integer quartos, Boolean destaque,
            String finalidade, BigDecimal min, BigDecimal max, int page, int size) {
        return buscar(cidade, tipo, quartos, destaque, finalidade, min, max, page, size, null, null, null);
    }

    public Pagina buscar(String cidade, String tipo, Integer quartos, Boolean destaque,
                         String finalidade, BigDecimal min, BigDecimal max, int page, int size, Double latitude, Double longitude, String ordenacao) {
        validarPagina(page, size);
        if ((min != null && min.signum() < 0)
                || (max != null && max.signum() < 0) || (min != null && max != null && min.compareTo(max) > 0)
                || (quartos != null && quartos < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros inválidos");
        }
        if (finalidade != null && !Set.of("VENDA", "LOCACAO").contains(finalidade)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalidade inválida");
        }
        if (ordenacao != null && !ordenacao.equals("distancia")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordenação inválida");
        if (latitude != null || longitude != null || ordenacao != null) {
            if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180 || !"distancia".equals(ordenacao))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe latitude, longitude e ordenacao=distancia");
            return proximos(cidade, tipo, quartos, destaque, finalidade, min, max, page, size, latitude, longitude);
        }
        var resultado = imoveis.buscar(cidade, tipo, quartos, destaque, finalidade, min, max,
                PageRequest.of(page, size, Sort.by("id")));
        var itens = montarResumos(resultado.getContent());
        return new Pagina(itens, page, size, resultado.getTotalElements(), resultado.getTotalPages());
    }


    private Pagina proximos(String cidade, String tipo, Integer quartos, Boolean destaque, String finalidade,
            BigDecimal min, BigDecimal max, int page, int size, double latitude, double longitude) {
        String filtro = """
            FROM imovel i WHERE i.status='PUBLICADO' AND i.latitude IS NOT NULL AND i.longitude IS NOT NULL
            AND (:cidade IS NULL OR i.cidade=:cidade) AND (:tipo IS NULL OR i.tipo=:tipo)
            AND (:quartos IS NULL OR i.quartos>=:quartos) AND (:destaque IS NULL OR i.destaque=:destaque)
            AND EXISTS (SELECT 1 FROM oferta o WHERE o.imovel_id=i.id
                AND (:finalidade IS NULL OR o.finalidade=:finalidade)
                AND (:min IS NULL OR o.preco>=:min) AND (:max IS NULL OR o.preco<=:max))
            """;
        var parametros = new java.util.HashMap<String, Object>();
        parametros.put("cidade", cidade); parametros.put("tipo", tipo); parametros.put("quartos", quartos);
        parametros.put("destaque", destaque); parametros.put("finalidade", finalidade);
        parametros.put("min", min); parametros.put("max", max);
        long total = banco.sql("SELECT COUNT(*) " + filtro).params(parametros).query(Long.class).single();
        parametros.put("lat", latitude); parametros.put("lon", longitude);
        parametros.put("limite", size); parametros.put("inicio", (long) page * size);
        record Distancia(long id, double km) {}
        var linhas = banco.sql("""
            SELECT i.id, 6371.0088 * 2 * ASIN(SQRT(LEAST(1.0,
                POW(SIN(RADIANS(i.latitude-:lat)/2),2) + COS(RADIANS(:lat))*COS(RADIANS(i.latitude))
                * POW(SIN(RADIANS(i.longitude-:lon)/2),2)))) AS km
            """ + filtro + " ORDER BY km,i.id LIMIT :limite OFFSET :inicio")
                .params(parametros).query(Distancia.class).list();
        var entidades = imoveis.findAllById(linhas.stream().map(Distancia::id).toList());
        var resumos = montarResumos(entidades).stream().collect(Collectors.toMap(ImovelDto::id, i -> i));
        var itens = linhas.stream().map(linha -> {
            var i = resumos.get(linha.id());
            return new ImovelDto(i.id(), i.titulo(), i.cidade(), i.bairro(), i.uf(), i.tipo(), i.quartos(),
                i.banheiros(), i.vagas(), i.areaTotal(), i.ofertas(), i.fotos(), linha.km());
        }).toList();
        return new Pagina(itens, page, size, total, (int) ((total + size - 1) / size));
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
                ofertasPorImovel.getOrDefault(imovel.id, List.of()), fotosPorImovel.getOrDefault(imovel.id, List.of()), null)).toList();
    }
}
