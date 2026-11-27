package br.com.talisma.api.imoveisapi.gestao;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class Geocodificacao {
    record Ponto(BigDecimal lat, BigDecimal lon, Map<String,String> address) {}

    // Endpoint Nominatim da instalação autorizada a processar os endereços.
    static Ponto buscar(GestaoImoveis.ImovelInput dados) {
        String base = System.getenv("NOMINATIM_URL");
        if (base == null || base.isBlank()) return null;
        var transporte = new SimpleClientHttpRequestFactory();
        transporte.setConnectTimeout(Duration.ofSeconds(2));
        transporte.setReadTimeout(Duration.ofSeconds(3));
        try {
            var resultados = RestClient.builder().baseUrl(base).requestFactory(transporte).build()
                    .get().uri(uri -> uri.path("/search").queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1).queryParam("limit", 2)
                            .queryParam("countrycodes", "br").queryParam("street", "{rua}")
                            .queryParam("city", "{cidade}").queryParam("postalcode", "{cep}")
                            .build(dados.numero() + " " + dados.logradouro(), dados.cidade(),
                                   dados.cep().substring(0,5) + "-" + dados.cep().substring(5)))
                    .retrieve().body(new ParameterizedTypeReference<List<Ponto>>() {});
            if (resultados == null || resultados.size() != 1) return null;
            var ponto = resultados.getFirst();
            if (ponto.address() == null || !dados.numero().trim().equalsIgnoreCase(ponto.address().get("house_number"))
                    || ponto.lat() == null || ponto.lon() == null
                    || ponto.lat().abs().compareTo(BigDecimal.valueOf(90)) > 0
                    || ponto.lon().abs().compareTo(BigDecimal.valueOf(180)) > 0) return null;
            return ponto;
        } catch (RestClientException e) {
            return null;
        }
    }
}
