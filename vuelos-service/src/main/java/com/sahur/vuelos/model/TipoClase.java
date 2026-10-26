package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TIPO_CLASE")
@Getter
@Setter
public class TipoClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clase")
    private Long idClase;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "porcentaje_recargo", nullable = false)
    private Double porcentajeRecargo;
}