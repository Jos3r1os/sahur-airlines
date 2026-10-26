package com.sahur.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESERVA")
@Getter
@Setter
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    // Vienen de otros servicios, por eso son solo numeros (sin relacion JPA)
    @Column(name = "id_pasajero", nullable = false)
    private Long idPasajero;

    @Column(name = "id_empleado", nullable = false)
    private Long idEmpleado;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    // PENDIENTE, PAGADA o CANCELADA
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // Suma de los precios de los boletos no cancelados
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
}