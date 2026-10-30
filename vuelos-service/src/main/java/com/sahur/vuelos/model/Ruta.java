package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "RUTA")
@Getter
@Setter
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @Column(name = "aeropuerto_origen", nullable = false, length = 3)
    private String aeropuertoOrigen;

    @Column(name = "aeropuerto_destino", nullable = false, length = 3)
    private String aeropuertoDestino;

    @Column(name = "distancia")
    private Double distancia;

    @Column(name = "duracion_aproximada")
    private Integer duracionAproximada;
}