package com.sahur.pasajeros.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ReservasClient {

    private final RestClient restClient = RestClient.create();

    private final String reservasUrl;

    public ReservasClient(@Value("${servicios.reservas.url}") String reservasUrl) {
        this.reservasUrl = reservasUrl;
    }
    public boolean tieneReservas(Long idPasajero) {
        List<?> reservas = restClient.get()
                .uri(reservasUrl + "/reservas/pasajero/" + idPasajero)
                .retrieve()
                .body(List.class);
        return reservas != null && !reservas.isEmpty();
    }
}