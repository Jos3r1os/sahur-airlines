package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoRequest {
    private String estado;
    // Usuario del empleado que hace el cambio (queda en el historial)
    private String usuario;
}