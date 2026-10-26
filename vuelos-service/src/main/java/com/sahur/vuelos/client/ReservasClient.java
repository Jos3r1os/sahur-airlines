package com.sahur.vuelos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ReservasClient {

    private final RestClient restClient = RestClient.create();

    private final String reservasUrl;

    public ReservasClient(@Value("${servicios.reservas.url}") String reservasUrl) {
        this.reservasUrl = reservasUrl;
    }

    // El vuelo ya tiene boletos vendidos? (GET /boletos/vuelo/{id} en reservas-service)
    public boolean vueloTieneBoletos(Long idVuelo) {
        List<?> boletos = restClient.get()
                .uri(reservasUrl + "/boletos/vuelo/" + idVuelo)
                .retrieve()
                .body(List.class);
        return boletos != null && !boletos.isEmpty();
    }

    // El asiento aparece en algun boleto? (revisa la lista de boletos de reservas-service)
    public boolean asientoTieneBoletos(Long idAsiento) {
        List<Map<String, Object>> boletos = restClient.get()
                .uri(reservasUrl + "/boletos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        if (boletos == null) {
            return false;
        }
        return boletos.stream()
                .anyMatch(b -> b.get("idAsiento") != null
                        && ((Number) b.get("idAsiento")).longValue() == idAsiento);
    }
}