package com.sahur.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIAL_BOLETO")
@Getter
@Setter
public class HistorialBoleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_boleto")
    private Boleto boleto;

    // Vacio en el primer registro (cuando se emite el boleto)
    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    // Quien hizo el cambio (usuario del empleado, PAGOS o SISTEMA)
    @Column(name = "usuario_responsable", nullable = false, length = 50)
    private String usuarioResponsable;
}