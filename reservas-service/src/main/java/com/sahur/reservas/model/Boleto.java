package com.sahur.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "BOLETO")
@Getter
@Setter
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleto")
    private Long idBoleto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    @Column(name = "id_vuelo", nullable = false)
    private Long idVuelo;

    @Column(name = "id_pasajero", nullable = false)
    private Long idPasajero;

    @Column(name = "id_asiento", nullable = false)
    private Long idAsiento;

    @Column(name = "codigo_boleto", nullable = false, unique = true)
    private String codigoBoleto;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}