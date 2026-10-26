package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsientoDto {
    private Long idAsiento;
    private String numeroAsiento;
    private String ubicacion;
    private AvionDto avion;
    private TipoClaseDto tipoClase;
}