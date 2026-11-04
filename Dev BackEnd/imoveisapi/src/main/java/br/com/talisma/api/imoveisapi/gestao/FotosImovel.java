package br.com.talisma.api.imoveisapi.gestao;

import static br.com.talisma.api.imoveisapi.gestao.GestaoImoveis.erro;
import static org.springframework.http.HttpStatus.*;

import br.com.talisma.api.imoveisapi.identidade.IdentidadeService;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Transactional
class FotosImovel {
    private final GestaoImoveis gestao;
    private final JdbcClient banco;
    private final IdentidadeService identidade;
    private final Path diretorio;

    FotosImovel(GestaoImoveis gestao, JdbcClient banco, IdentidadeService identidade,
                @Value("${APP_FOTOS_DIR:./fotos-local}") String diretorio) {
        this.gestao = gestao;
        this.banco = banco;
        this.identidade = identidade;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @PostMapping("/gestao/imoveis/{id}/fotos")
    @ResponseStatus(CREATED)
    List<Long> adicionar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                         @RequestParam("arquivos") List<MultipartFile> arquivos) {
        var ator = identidade.usuarioAtual(jwt);
        var imovel = editavel(ator, id);
        if (arquivos.isEmpty() || arquivos.size() > 10) {
            throw erro(BAD_REQUEST, "Envie de uma a dez imagens");
        }
        var ids = new ArrayList<Long>();
        var gravadas = new ArrayList<Path>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    gravadas.forEach(FotosImovel::apagar);
                }
            }
        });
        int ordem = banco.sql("SELECT COALESCE(MAX(ordem),0) FROM foto WHERE imovel_id=?")
                .param(id).query(Integer.class).single();
        try {
            Files.createDirectories(diretorio);
            for (MultipartFile arquivo : arquivos) {
                String formato = formato(arquivo);
                String nome = UUID.randomUUID() + "." + formato;
                Path destino = caminho(nome);
                gravadas.add(destino);
                try (var entrada = arquivo.getInputStream()) {
                    Files.copy(entrada, destino);
                }
                var chave = new GeneratedKeyHolder();
                banco.sql("INSERT INTO foto(imovel_id,arquivo,ordem) VALUES (?,?,?)")
                        .params(id, nome, ++ordem).update(chave, "id");
                ids.add(chave.getKey().longValue());
            }
        } catch (IOException excecao) {
            throw erro(INTERNAL_SERVER_ERROR, "Não foi possível armazenar as imagens");
        }
        gestao.fotoAlterada(ator, imovel);
        return ids;
    }

    @DeleteMapping("/gestao/imoveis/{id}/fotos/{foto}")
    @ResponseStatus(NO_CONTENT)
    void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @PathVariable long foto) {
        var ator = identidade.usuarioAtual(jwt);
        var imovel = editavel(ator, id);
        String nome = banco.sql("SELECT arquivo FROM foto WHERE id=? AND imovel_id=?")
                .params(foto, id).query(String.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Foto não encontrada"));
        banco.sql("DELETE FROM foto WHERE id=?").param(foto).update();
        gestao.fotoAlterada(ator, imovel);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { apagar(caminho(nome)); }
        });
    }

    @GetMapping("/fotos/{id}")
    @Transactional(readOnly = true)
    ResponseEntity<FileSystemResource> ler(@PathVariable long id) {
        String nome = banco.sql("""
                SELECT f.arquivo FROM foto f JOIN imovel i ON i.id=f.imovel_id WHERE f.id=? AND i.status='PUBLICADO'
                """).param(id).query(String.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Foto não encontrada"));
        Path local = caminho(nome);
        if (!Files.isRegularFile(local, LinkOption.NOFOLLOW_LINKS)) {
            throw erro(NOT_FOUND, "Foto não encontrada");
        }
        MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(tipo).body(new FileSystemResource(local));
    }

    private GestaoImoveis.Resumo editavel(IdentidadeService.UsuarioDto ator, long id) {
        var imovel = gestao.carregar(ator, id);
        if (imovel.status().equals("ARQUIVADO")) {
            throw erro(CONFLICT, "Imóvel arquivado");
        }
        return imovel;
    }

    private static String formato(MultipartFile arquivo) {
        if (arquivo.isEmpty() || arquivo.getSize() > 5L * 1024 * 1024) {
            throw erro(BAD_REQUEST, "Cada imagem deve ter até 5 MB");
        }
        try (var entrada = arquivo.getInputStream(); var imagem = ImageIO.createImageInputStream(entrada)) {
            var leitores = ImageIO.getImageReaders(imagem);
            if (!leitores.hasNext()) {
                throw erro(BAD_REQUEST, "Envie uma imagem JPEG ou PNG");
            }
            var leitor = leitores.next();
            try {
                leitor.setInput(imagem);
                String tipo = leitor.getFormatName().toLowerCase(Locale.ROOT);
                long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                if ((!tipo.equals("jpeg") && !tipo.equals("png")) || pixels > 25_000_000 || leitor.read(0) == null) {
                    throw erro(BAD_REQUEST, "Imagem inválida ou acima de 25 megapixels");
                }
                return tipo;
            } finally {
                leitor.dispose();
            }
        } catch (IOException | IllegalArgumentException excecao) {
            throw erro(BAD_REQUEST, "Imagem inválida");
        }
    }

    private Path caminho(String nome) {
        if (!nome.matches("[0-9a-f-]{36}\\.(jpeg|png)")) {
            throw erro(NOT_FOUND, "Foto não encontrada");
        }
        return diretorio.resolve(nome);
    }

    private static void apagar(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException excecao) {
            LoggerFactory.getLogger(FotosImovel.class).warn("Falha ao remover arquivo de foto");
        }
    }
}
