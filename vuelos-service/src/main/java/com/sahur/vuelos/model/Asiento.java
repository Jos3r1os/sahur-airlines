package com.sahur.vuelos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ASIENTO")
@Getter
@Setter
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asiento")
    private Long idAsiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_avion")
    private Avion avion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_clase")
    private TipoClase tipoClase;

    @Column(name = "numero_asiento", nullable = false)
    private String numeroAsiento;

    @Column(name = "ubicacion", length = 20)
    private String ubicacion;
}