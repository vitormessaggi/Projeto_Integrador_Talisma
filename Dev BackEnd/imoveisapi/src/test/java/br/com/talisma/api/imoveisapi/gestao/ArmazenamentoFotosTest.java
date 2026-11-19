package br.com.talisma.api.imoveisapi.gestao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class ArmazenamentoFotosTest {
    @TempDir Path pasta;
    @Test void armazenaRecuperaERemoveSemPermitirCaminhosArbitrarios() throws Exception {
        try(var storage=new ArmazenamentoFotos("local","","us-east-1",pasta.toString())) {
            String chave="12345678-1234-1234-1234-123456789012.png";
            storage.gravar(chave,new byte[]{1,2,3},"image/png");
            assertThat(storage.ler(chave).getContentAsByteArray()).containsExactly(1,2,3);
            assertThatThrownBy(()->storage.ler("../segredo")).isInstanceOf(java.io.IOException.class);
            storage.apagar(chave); storage.apagar(chave);
            assertThatThrownBy(()->storage.ler(chave)).isInstanceOf(java.io.FileNotFoundException.class);
        }
    }
    @Test void recusaConfiguracaoS3Incompleta() {
        assertThatThrownBy(()->new ArmazenamentoFotos("s3","","us-east-1",pasta.toString())).isInstanceOf(IllegalArgumentException.class);
    }
}
