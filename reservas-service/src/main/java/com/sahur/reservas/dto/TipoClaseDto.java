package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipoClaseDto {
    private Long idClase;
    private String nombre;
    private Double porcentajeRecargo;
}