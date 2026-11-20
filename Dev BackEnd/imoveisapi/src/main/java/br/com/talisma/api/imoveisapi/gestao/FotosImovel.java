package br.com.talisma.api.imoveisapi.gestao;

import static br.com.talisma.api.imoveisapi.gestao.GestaoImoveis.erro;
import static org.springframework.http.HttpStatus.*;

import br.com.talisma.api.imoveisapi.identidade.IdentidadeService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Transactional
class FotosImovel {
    private final GestaoImoveis gestao;
    private final JdbcClient banco;
    private final IdentidadeService identidade;
    private final ArmazenamentoFotos arquivos;
    private final LimpezaFotos limpeza;

    FotosImovel(GestaoImoveis gestao, JdbcClient banco, IdentidadeService identidade,
                ArmazenamentoFotos arquivos, LimpezaFotos limpeza) {
        this.gestao=gestao; this.banco=banco; this.identidade=identidade;
        this.arquivos=arquivos; this.limpeza=limpeza;
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
        int ordem = banco.sql("SELECT COALESCE(MAX(ordem),0) FROM foto WHERE imovel_id=?")
                .param(id).query(Integer.class).single();
        try {
            for (MultipartFile arquivo : arquivos) {
                String formato = formato(arquivo);
                String nome = this.arquivos.novaChave(id, formato);
                limpeza.prepararUpload(nome);
                // Recodificar remove EXIF, GPS e outros metadados do arquivo original.
                java.awt.image.BufferedImage imagem;
                try (var entrada = arquivo.getInputStream()) { imagem = ImageIO.read(entrada); }
                var saida = new java.io.ByteArrayOutputStream();
                if (!ImageIO.write(imagem, formato, saida)) throw new IOException("Imagem inválida");
                this.arquivos.gravar(nome, saida.toByteArray(), formato.equals("png") ? "image/png" : "image/jpeg");
                var chave = new GeneratedKeyHolder();
                banco.sql("INSERT INTO foto(imovel_id,arquivo,ordem) VALUES (?,?,?)")
                        .params(id, nome, ++ordem).update(chave, "id");
                ids.add(chave.getKey().longValue());
                banco.sql("DELETE FROM exclusao_arquivo WHERE arquivo=?").param(nome).update();
            }
        } catch (IOException | software.amazon.awssdk.core.exception.SdkException excecao) {
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
        banco.sql("INSERT INTO exclusao_arquivo(arquivo,criado_em) VALUES (?,UTC_TIMESTAMP(6) - INTERVAL 2 HOUR)").param(nome).update();
    }

    @GetMapping("/fotos/{id}")
    @Transactional(readOnly = true)
    ResponseEntity<Resource> ler(@PathVariable long id) {
        String nome = banco.sql("""
                SELECT f.arquivo FROM foto f JOIN imovel i ON i.id=f.imovel_id WHERE f.id=? AND i.status='PUBLICADO'
                """).param(id).query(String.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Foto não encontrada"));
        MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(tipo).body(recurso(nome));
    }

    @GetMapping("/gestao/imoveis/{id}/fotos/{foto}")
    ResponseEntity<Resource> lerInterna(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @PathVariable long foto) {
        gestao.carregar(identidade.usuarioAtual(jwt), id);
        String nome = banco.sql("SELECT arquivo FROM foto WHERE id=? AND imovel_id=?").params(foto, id)
                .query(String.class).optional().orElseThrow(() -> erro(NOT_FOUND, "Foto não encontrada"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(recurso(nome));
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

    private Resource recurso(String nome) {
        try { return arquivos.ler(nome); }
        catch (java.io.FileNotFoundException e) { throw erro(NOT_FOUND,"Foto não encontrada"); }
        catch (IOException | software.amazon.awssdk.core.exception.SdkException e) { throw erro(SERVICE_UNAVAILABLE,"Armazenamento temporariamente indisponível"); }
    }
}
