package br.com.talisma.api.imoveisapi.identidade;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class Seguranca {
    static final String EMISSOR = "talisma-imoveis";

    @Bean
    BCryptPasswordEncoder senhas() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey chaveJwt(@Value("${APP_JWT_KEY}") String base64) {
        byte[] chave = Base64.getDecoder().decode(base64);
        if (chave.length < 32) {
            throw new IllegalArgumentException("APP_JWT_KEY precisa ter pelo menos 32 bytes após decodificação");
        }
        return new SecretKeySpec(chave, "HmacSHA256");
    }

    @Bean
    JwtEncoder encoder(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder decoder(SecretKey chave) {
        var decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain api(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.GET, "/api/v1/imoveis", "/api/v1/imoveis/*", "/api/v1/fotos/*", "/actuator/health", "/v3/api-docs").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastro", "/api/v1/auth/login",
                                "/api/v1/auth/verificar-email", "/api/v1/auth/reenviar-codigo",
                                "/api/v1/auth/esqueci-senha", "/api/v1/auth/redefinir-senha",
                                "/api/v1/auth/renovar", "/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(config -> config.jwt(jwt -> {}))
                .build();
    }
}
