package br.com.talisma.api.imoveisapi.atendimento;

import br.com.talisma.api.imoveisapi.identidade.IdentidadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class AtendimentoController {
    private final IdentidadeService identidade;
    private final Atendimento atendimento;

    AtendimentoController(IdentidadeService identidade, Atendimento atendimento) {
        this.identidade = identidade;
        this.atendimento = atendimento;
    }

    @io.swagger.v3.oas.annotations.media.Schema(name = "StatusAtendimento") record Status(@NotBlank String status) {}
    record Atribuicao(@Positive long corretorId) {}

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    Atendimento.Lead contato(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Atendimento.Contato dados) {
        return atendimento.contato(identidade.usuarioAtual(jwt), dados);
    }

    @GetMapping("/leads")
    Atendimento.Pagina<Atendimento.Lead> leads(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return atendimento.leads(identidade.usuarioAtual(jwt), page, size);
    }

    @PatchMapping("/leads/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void statusLead(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Status dados) {
        atendimento.statusLead(identidade.usuarioAtual(jwt), id, dados.status());
    }

    @PatchMapping("/leads/{id}/corretor")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void atribuir(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Atribuicao dados) {
        atendimento.atribuir(identidade.usuarioAtual(jwt), id, dados.corretorId());
    }

    @PostMapping("/visitas")
    @ResponseStatus(HttpStatus.CREATED)
    Atendimento.Visita visitar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Atendimento.PedidoVisita dados) {
        return atendimento.solicitar(identidade.usuarioAtual(jwt), dados);
    }

    @GetMapping("/visitas")
    Atendimento.Pagina<Atendimento.Visita> visitas(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return atendimento.visitas(identidade.usuarioAtual(jwt), page, size);
    }

    @PatchMapping("/visitas/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void statusVisita(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Status dados) {
        atendimento.statusVisita(identidade.usuarioAtual(jwt), id, dados.status());
    }
}
