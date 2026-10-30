package com.sahur.pagos.client;

import com.sahur.pagos.dto.ReservaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ReservasClient {

    private final RestClient restClient = RestClient.create();

    private final String reservasUrl;

    public ReservasClient(@Value("${servicios.reservas.url}") String reservasUrl) {
        this.reservasUrl = reservasUrl;
    }

    public ReservaDto obtenerReserva(Long idReserva) {
        try {
            return restClient.get().uri(reservasUrl + "/reservas/" + idReserva).retrieve().body(ReservaDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public String marcarPagada(Long idReserva) {
        try {
            restClient.post().uri(reservasUrl + "/reservas/" + idReserva + "/pagar").retrieve().toBodilessEntity();
            return null;
        } catch (HttpClientErrorException e) {
            return e.getResponseBodyAsString();
        }
    }
}