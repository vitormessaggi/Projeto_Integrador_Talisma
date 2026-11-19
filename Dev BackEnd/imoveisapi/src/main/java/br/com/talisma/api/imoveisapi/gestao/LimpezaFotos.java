package br.com.talisma.api.imoveisapi.gestao;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@EnableScheduling
public class LimpezaFotos {
    private final JdbcClient banco;
    private final ArmazenamentoFotos arquivos;
    public LimpezaFotos(JdbcClient banco, ArmazenamentoFotos arquivos) { this.banco=banco; this.arquivos=arquivos; }
    // Persistido antes do upload: rollback ou interrupção do processo deixam trabalho recuperável.
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void prepararUpload(String chave) {
        banco.sql("INSERT INTO exclusao_arquivo(arquivo) VALUES (?)").param(chave).update();
    }
    @Scheduled(fixedDelay=60000)
    public void executar() {
        var pendentes=banco.sql("SELECT arquivo FROM exclusao_arquivo WHERE criado_em < UTC_TIMESTAMP(6) - INTERVAL 1 HOUR LIMIT 100")
                .query(String.class).list();
        for (String chave:pendentes) {
            try {
                // A janela protege uploads ainda em andamento; fotos referenciadas nunca são removidas.
                boolean existe=banco.sql("SELECT EXISTS(SELECT 1 FROM foto WHERE arquivo=?)").param(chave).query(Boolean.class).single();
                if (!existe) arquivos.apagar(chave);
                banco.sql("DELETE FROM exclusao_arquivo WHERE arquivo=?").param(chave).update();
            } catch (Exception e) {
                banco.sql("UPDATE exclusao_arquivo SET tentativas=tentativas+1 WHERE arquivo=?").param(chave).update();
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Limpeza de foto pendente; será repetida");
            }
        }
    }
}
