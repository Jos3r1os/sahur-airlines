package com.sahur.reservas.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {
    // Usuario del empleado que hace la accion (queda en el historial)
    private String usuario;
}