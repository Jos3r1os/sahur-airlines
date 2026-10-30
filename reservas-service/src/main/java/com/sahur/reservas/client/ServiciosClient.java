package com.sahur.reservas.client;

import com.sahur.reservas.dto.AsientoDto;
import com.sahur.reservas.dto.EmpleadoDto;
import com.sahur.reservas.dto.VueloDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ServiciosClient {

    private final RestClient restClient = RestClient.create();

    private final String pasajerosUrl;
    private final String empleadosUrl;
    private final String vuelosUrl;

    public ServiciosClient(@Value("${servicios.pasajeros.url}") String pasajerosUrl,
                           @Value("${servicios.empleados.url}") String empleadosUrl,
                           @Value("${servicios.vuelos.url}") String vuelosUrl) {
        this.pasajerosUrl = pasajerosUrl;
        this.empleadosUrl = empleadosUrl;
        this.vuelosUrl = vuelosUrl;
    }

    public boolean pasajeroExiste(Long id) {
        try {
            restClient.get().uri(pasajerosUrl + "/pasajeros/" + id).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
    public EmpleadoDto obtenerEmpleado(Long id) {
        return obtener(empleadosUrl + "/empleados/" + id, EmpleadoDto.class);
    }

    public VueloDto obtenerVuelo(Long id) {
        return obtener(vuelosUrl + "/vuelos/" + id, VueloDto.class);
    }

    public AsientoDto obtenerAsiento(Long id) {
        return obtener(vuelosUrl + "/asientos/" + id, AsientoDto.class);
    }

    public List<AsientoDto> obtenerAsientosDeAvion(Long idAvion) {
        List<AsientoDto> asientos = restClient.get()
                .uri(vuelosUrl + "/asientos/avion/" + idAvion)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AsientoDto>>() {});
        return asientos == null ? List.of() : asientos;
    }

    private <T> T obtener(String url, Class<T> tipo) {
        try {
            return restClient.get().uri(url).retrieve().body(tipo);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}