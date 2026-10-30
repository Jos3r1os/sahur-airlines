package com.sahur.empleados.client;

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

    public boolean tieneReservas(Long idEmpleado) {
        List<?> reservas = restClient.get()
                .uri(reservasUrl + "/reservas/empleado/" + idEmpleado)
                .retrieve()
                .body(List.class);
        return reservas != null && !reservas.isEmpty();
    }
}