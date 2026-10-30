package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "AVION")
@Getter
@Setter
public class Avion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avion")
    private Long idAvion;

    @Column(name = "matricula", nullable = false, unique = true)
    private String matricula;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "estado", length = 20)
    private String estado;
}