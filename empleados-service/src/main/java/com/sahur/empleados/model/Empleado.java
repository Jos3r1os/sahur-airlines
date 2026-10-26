package com.sahur.empleados.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EMPLEADO")
@Getter
@Setter
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Long idEmpleado;

    @Column(name = "usuario", nullable = false, unique = true)
    private String usuario;

    // WRITE_ONLY: se acepta al crear el empleado, pero nunca se devuelve en las respuestas
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false)
    private String password;

    // Campos nuevos: quedan opcionales en Oracle porque la tabla ya tiene datos.
    // Que sean obligatorios lo valida el controller.
    @Column(name = "nombres", length = 100)
    private String nombres;

    @Column(name = "apellidos", length = 100)
    private String apellidos;

    @Column(name = "puesto", length = 50)
    private String puesto;

    // ACTIVO o INACTIVO
    @Column(name = "estado", length = 20)
    private String estado;
}