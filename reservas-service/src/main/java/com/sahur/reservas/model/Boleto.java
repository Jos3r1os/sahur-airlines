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

    // Esta si es relacion normal porque RESERVA esta en este mismo servicio
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    // Vienen de otros servicios (vuelos y pasajeros)
    @Column(name = "id_vuelo", nullable = false)
    private Long idVuelo;

    @Column(name = "id_pasajero", nullable = false)
    private Long idPasajero;

    @Column(name = "id_asiento", nullable = false)
    private Long idAsiento;

    @Column(name = "codigo_boleto", nullable = false, unique = true)
    private String codigoBoleto;

    // Precio base del vuelo + recargo de la clase del asiento
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    // EMITIDO, PAGADO, CANCELADO o USADO (estado actual; los cambios quedan en HISTORIAL_BOLETO)
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}