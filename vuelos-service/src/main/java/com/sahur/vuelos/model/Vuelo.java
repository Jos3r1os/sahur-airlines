package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "VUELO")
@Getter
@Setter
public class Vuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vuelo")
    private Long idVuelo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_ruta")
    private Ruta ruta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_avion")
    private Avion avion;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDateTime fechaSalida;

    @Column(name = "fecha_llegada")
    private LocalDateTime fechaLlegada;

    @Column(name = "precio_base", precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "estado", length = 20)
    private String estado;
}