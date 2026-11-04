package br.com.talisma.api.imoveisapi.catalogo;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/imoveis")
public class CatalogoController {
    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    public CatalogoService.Pagina listar(@RequestParam(required = false) String cidade,
                                         @RequestParam(required = false) String tipo,
                                         @RequestParam(required = false) Integer quartosMin,
                                         @RequestParam(required = false) Boolean destaque,
                                         @RequestParam(required = false) String finalidade,
                                         @RequestParam(required = false) BigDecimal precoMin,
                                         @RequestParam(required = false) BigDecimal precoMax,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return service.buscar(cidade, tipo, quartosMin, destaque, finalidade, precoMin, precoMax, page, size);
    }

    @GetMapping("/{id}")
    public CatalogoService.Detalhe detalhe(@PathVariable long id) {
        return service.detalhar(id);
    }

    @GetMapping("/filtros")
    public CatalogoService.Filtros filtros() {
        return service.filtros();
    }
}
