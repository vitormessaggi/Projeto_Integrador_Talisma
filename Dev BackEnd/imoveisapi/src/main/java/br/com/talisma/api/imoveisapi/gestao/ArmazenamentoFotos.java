package br.com.talisma.api.imoveisapi.gestao;

import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** O bucket permanece privado; a autorização continua nas rotas da API. */
@Component
public class ArmazenamentoFotos implements AutoCloseable {
    private final S3Client s3;
    private final String bucket;
    private final Path diretorio;
    public ArmazenamentoFotos(@Value("${APP_STORAGE:local}") String provedor,
            @Value("${APP_S3_BUCKET:}") String bucket, @Value("${AWS_REGION:us-east-1}") String regiao,
            @Value("${APP_FOTOS_DIR:./fotos-local}") String diretorio) {
        if (!provedor.equals("local") && !provedor.equals("s3")) throw new IllegalArgumentException("APP_STORAGE inválido");
        if (provedor.equals("s3") && bucket.isBlank()) throw new IllegalArgumentException("APP_S3_BUCKET obrigatório");
        this.bucket = bucket;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.s3 = provedor.equals("s3") ? S3Client.builder().region(Region.of(regiao))
                .overrideConfiguration(c -> c.apiCallTimeout(java.time.Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(java.time.Duration.ofSeconds(10))).build() : null;
    }
    public void gravar(String chave, byte[] dados, String tipo) throws IOException {
        validar(chave);
        if (s3 != null) s3.putObject(r -> r.bucket(bucket).key(chave).contentType(tipo),RequestBody.fromBytes(dados));
        else { Files.createDirectories(diretorio); Files.write(diretorio.resolve(chave),dados,StandardOpenOption.CREATE_NEW); }
    }
    public Resource ler(String chave) throws IOException {
        validar(chave);
        if (s3 != null) {
            try { return new InputStreamResource(s3.getObject(r -> r.bucket(bucket).key(chave))); }
            catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) { throw new FileNotFoundException(); }
        }
        Path local = diretorio.resolve(chave);
        if (!Files.isRegularFile(local,LinkOption.NOFOLLOW_LINKS)) throw new FileNotFoundException();
        return new FileSystemResource(local);
    }
    public void apagar(String chave) throws IOException {
        validar(chave);
        if (s3 != null) s3.deleteObject(r -> r.bucket(bucket).key(chave));
        else Files.deleteIfExists(diretorio.resolve(chave));
    }
    private static void validar(String chave) throws IOException {
        if (!chave.matches("[0-9a-f-]{36}\\.(jpeg|png)")) throw new IOException("Chave de foto inválida");
    }
    @Override public void close() { if (s3!=null) s3.close(); }
}
