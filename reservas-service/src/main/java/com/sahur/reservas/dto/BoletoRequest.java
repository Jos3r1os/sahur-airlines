package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoletoRequest {
    private Long idReserva;
    private Long idVuelo;
    private Long idAsiento;
    // Usuario del empleado que emite el boleto (queda en el historial)
    private String usuario;
}