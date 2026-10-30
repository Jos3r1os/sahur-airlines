package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoletoRequest {
    private Long idReserva;
    private Long idVuelo;
    private Long idAsiento;
    private String usuario;
}