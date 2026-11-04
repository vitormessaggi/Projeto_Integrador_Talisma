package br.com.talisma.api.imoveisapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ContratoApi {
    @Bean
    OpenAPI contrato() {
        return new OpenAPI().info(new Info().title("Talismã Imóveis").version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer documentarProtecao() {
        return api -> api.getPaths().forEach((caminho, operacoes) -> {
            if (!caminho.startsWith("/api/v1/auth/") && !caminho.startsWith("/api/v1/imoveis")
                    && !caminho.startsWith("/api/v1/fotos/")) {
                operacoes.readOperations().forEach(operacao -> operacao.addSecurityItem(new SecurityRequirement().addList("bearer")));
            }
        });
    }
}
