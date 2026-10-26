package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VueloDto {
    private Long idVuelo;
    private AvionDto avion;
    private LocalDateTime fechaSalida;
    private BigDecimal precioBase;
    private String estado;
}