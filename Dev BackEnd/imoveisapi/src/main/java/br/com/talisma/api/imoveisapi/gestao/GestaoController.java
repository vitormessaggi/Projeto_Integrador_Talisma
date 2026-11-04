package br.com.talisma.api.imoveisapi.gestao;

import br.com.talisma.api.imoveisapi.identidade.IdentidadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gestao")
class GestaoController {
    private final IdentidadeService identidade;
    private final GestaoImoveis gestao;

    GestaoController(IdentidadeService identidade, GestaoImoveis gestao) {
        this.identidade = identidade;
        this.gestao = gestao;
    }

    @io.swagger.v3.oas.annotations.media.Schema(name = "StatusImovel") record Status(@NotBlank String status, @Size(max = 1000) String motivo) {}

    @GetMapping("/proprietarios")
    GestaoImoveis.Pagina<GestaoImoveis.Proprietario> proprietarios(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return gestao.proprietarios(identidade.usuarioAtual(jwt), page, size);
    }

    @PostMapping("/proprietarios")
    @ResponseStatus(HttpStatus.CREATED)
    GestaoImoveis.Proprietario proprietario(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GestaoImoveis.ProprietarioInput dados) {
        return gestao.salvarProprietario(identidade.usuarioAtual(jwt), null, dados);
    }

    @PutMapping("/proprietarios/{id}")
    GestaoImoveis.Proprietario atualizarProprietario(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody GestaoImoveis.ProprietarioInput dados) {
        return gestao.salvarProprietario(identidade.usuarioAtual(jwt), id, dados);
    }

    @GetMapping("/imoveis")
    GestaoImoveis.Pagina<GestaoImoveis.Resumo> imoveis(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return gestao.listar(identidade.usuarioAtual(jwt), status, page, size);
    }

    @GetMapping("/imoveis/{id}")
    GestaoImoveis.Detalhe detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return gestao.detalhe(identidade.usuarioAtual(jwt), id);
    }

    @PostMapping("/imoveis")
    @ResponseStatus(HttpStatus.CREATED)
    GestaoImoveis.Detalhe criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GestaoImoveis.ImovelInput dados) {
        return gestao.salvar(identidade.usuarioAtual(jwt), null, dados);
    }

    @PutMapping("/imoveis/{id}")
    GestaoImoveis.Detalhe atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody GestaoImoveis.ImovelInput dados) {
        return gestao.salvar(identidade.usuarioAtual(jwt), id, dados);
    }

    @PatchMapping("/imoveis/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void status(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Status dados) {
        gestao.transicao(identidade.usuarioAtual(jwt), id, dados.status(), dados.motivo());
    }
}
