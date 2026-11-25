package br.com.talisma.api.imoveisapi.identidade;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/api/v1/legal")
public class DocumentosLegais {
    public static final String VERSAO = "2026-11-25";
    public static final String TERMOS = ler("termos.txt");
    public static final String PRIVACIDADE = ler("privacidade.txt");
    public record Documento(String versao, String hash, String texto) {}
    public record Documentos(Documento termos, Documento privacidade) {}
    @GetMapping
    public Documentos documentos() {
        return new Documentos(new Documento(VERSAO, hash(TERMOS), TERMOS),
                new Documento(VERSAO, hash(PRIVACIDADE), PRIVACIDADE));
    }
    public static void validar(String termos, String privacidade) {
        if (!VERSAO.equals(termos) || !VERSAO.equals(privacidade))
            throw new ResponseStatusException(CONFLICT, "Os documentos foram atualizados. Leia-os e confirme o aceite novamente.");
    }
    public static void registrar(JdbcClient banco, long usuario, String termos, String privacidade, String origem) {
        validar(termos, privacidade);
        banco.sql("INSERT INTO aceite_legal(usuario_id,versao_termos,versao_privacidade,hash_termos,hash_privacidade,origem) VALUES (?,?,?,?,?,?)")
                .params(usuario, termos, privacidade, hash(TERMOS), hash(PRIVACIDADE), origem).update();
    }
    private static String ler(String arquivo) {
        try { return new ClassPathResource("legal/" + arquivo).getContentAsString(StandardCharsets.UTF_8); }
        catch (java.io.IOException e) { throw new IllegalStateException("Documento legal indisponível", e); }
    }
    private static String hash(String texto) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
