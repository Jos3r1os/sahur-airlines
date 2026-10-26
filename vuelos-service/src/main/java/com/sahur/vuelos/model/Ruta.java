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

    // Guardan el codigo IATA del aeropuerto, como en el diagrama
    @Column(name = "aeropuerto_origen", nullable = false, length = 3)
    private String aeropuertoOrigen;

    @Column(name = "aeropuerto_destino", nullable = false, length = 3)
    private String aeropuertoDestino;

    // Campos nuevos: quedan opcionales en Oracle porque la tabla ya tiene datos.
    // Que sean obligatorios lo valida el controller.
    // Distancia en kilometros
    @Column(name = "distancia")
    private Double distancia;

    // Duracion aproximada en minutos
    @Column(name = "duracion_aproximada")
    private Integer duracionAproximada;
}