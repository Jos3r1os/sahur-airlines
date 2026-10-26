package com.sahur.pagos.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReservaDto {
    private Long idReserva;
    private String estado;
    private BigDecimal total;
}