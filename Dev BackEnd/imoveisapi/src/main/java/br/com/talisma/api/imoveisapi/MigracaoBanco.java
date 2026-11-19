package br.com.talisma.api.imoveisapi;
import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;

@Configuration
@Profile("migrar")
class MigracaoBanco {
    @Bean CommandLineRunner encerrarAposMigracao(ConfigurableApplicationContext contexto) {
        return args -> SpringApplication.exit(contexto, () -> 0);
    }
}
