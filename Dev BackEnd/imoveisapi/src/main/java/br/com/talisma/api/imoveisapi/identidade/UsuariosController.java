package br.com.talisma.api.imoveisapi.identidade;

import br.com.talisma.api.imoveisapi.catalogo.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class UsuariosController {
    private final IdentidadeService identidade;
    private final UsuariosService usuarios;

    UsuariosController(IdentidadeService identidade, UsuariosService usuarios) {
        this.identidade = identidade;
        this.usuarios = usuarios;
    }

    record Atualizacao(@NotBlank @Size(max = 160) String nome, @Pattern(regexp = "[0-9+() -]{8,20}") String telefone) {}
    record TrocaSenha(@NotBlank @Size(max = 72) String senhaAtual, @NotBlank @Size(min = 8, max = 72) String novaSenha) {}
    record Corretor(@NotBlank @Size(max = 160) String nome, @NotBlank @Email @Size(max = 254) String email,
                    @NotBlank @Size(min = 8, max = 72) String senha) {}

    @GetMapping("/usuarios/me")
    UsuariosService.Perfil perfil(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.perfil(identidade.usuarioAtual(jwt).id());
    }

    @PutMapping("/usuarios/me")
    UsuariosService.Perfil atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Atualizacao dados) {
        return usuarios.atualizar(identidade.usuarioAtual(jwt).id(), dados.nome(), dados.telefone());
    }

    @PutMapping("/usuarios/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void senha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TrocaSenha dados) {
        usuarios.trocarSenha(identidade.usuarioAtual(jwt).id(), dados.senhaAtual(), dados.novaSenha());
    }

    @GetMapping("/usuarios/me/favoritos")
    CatalogoService.Pagina favoritos(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return usuarios.favoritos(identidade.usuarioAtual(jwt), page, size);
    }

    @PutMapping("/usuarios/me/favoritos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void favoritar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        usuarios.favorito(identidade.usuarioAtual(jwt), id, true);
    }

    @DeleteMapping("/usuarios/me/favoritos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void desfavoritar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        usuarios.favorito(identidade.usuarioAtual(jwt), id, false);
    }

    @GetMapping("/admin/usuarios")
    UsuariosService.PaginaInterna internos(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return usuarios.internos(identidade.usuarioAtual(jwt), page, size);
    }

    @PostMapping("/admin/corretores")
    @ResponseStatus(HttpStatus.CREATED)
    UsuariosService.ContaInterna criarCorretor(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Corretor dados) {
        return usuarios.criarCorretor(identidade.usuarioAtual(jwt), dados.nome(), dados.email(), dados.senha());
    }

    @PatchMapping("/admin/usuarios/{id}/desbloqueio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void desbloquear(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        usuarios.desbloquear(identidade.usuarioAtual(jwt), id);
    }
}
