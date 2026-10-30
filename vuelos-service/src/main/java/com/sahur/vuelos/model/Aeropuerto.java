package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "AEROPUERTO")
@Getter
@Setter
public class Aeropuerto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aeropuerto")
    private Long idAeropuerto;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "codigo_iata", nullable = false, unique = true, length = 3)
    private String codigoIata;
    
    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "pais", length = 100)
    private String pais;
}