package br.com.talisma.api.imoveisapi.identidade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class IdentidadeController {
    private final IdentidadeService identidade;

    IdentidadeController(IdentidadeService identidade) {
        this.identidade = identidade;
    }

    record Cadastro(@NotBlank @Size(max = 160) String nome, @NotBlank @Email @Size(max = 254) String email,
                    @NotBlank @Size(max = 20) String cpf, @NotBlank @Size(max = 30) String rg,
                    @NotBlank @Size(max = 20) String orgaoEmissor,
                    @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf, @NotBlank @Size(min = 8, max = 72) String senha) {}
    record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String senha) {}
    record PedidoEmail(@NotBlank @Email @Size(max = 254) String email) {}
    record Codigo(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) {}
    record Redefinicao(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo,
                       @NotBlank @Size(min = 8, max = 72) String novaSenha) {}
    record Renovacao(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String refreshToken) {}

    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    void cadastro(@Valid @RequestBody Cadastro dados) { identidade.cadastrar(dados); }

    @PostMapping("/auth/login")
    IdentidadeService.Tokens login(@Valid @RequestBody Login dados) { return identidade.login(dados); }

    @PostMapping("/auth/verificar-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verificar(@Valid @RequestBody Codigo dados) { identidade.verificar(dados); }

    @PostMapping("/auth/reenviar-codigo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reenviar(@Valid @RequestBody PedidoEmail dados) { identidade.solicitarCodigo(dados.email(), "CADASTRO"); }

    @PostMapping("/auth/esqueci-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void recuperar(@Valid @RequestBody PedidoEmail dados) { identidade.solicitarCodigo(dados.email(), "RECUPERACAO"); }

    @PostMapping("/auth/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void redefinir(@Valid @RequestBody Redefinicao dados) { identidade.redefinir(dados); }

    @PostMapping("/auth/renovar")
    IdentidadeService.Tokens renovar(@Valid @RequestBody Renovacao dados) { return identidade.renovar(dados.refreshToken()); }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody Renovacao dados) { identidade.logout(dados.refreshToken()); }

}
