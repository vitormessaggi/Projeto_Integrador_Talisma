package br.com.talisma.api.imoveisapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ImoveisapiApplication {

	public static void main(String[] args) {
		var contexto = SpringApplication.run(ImoveisapiApplication.class, args);
        if (contexto.getEnvironment().matchesProfiles("provisionar-admin")) {
            contexto.close();
        }
	}

}
