package com.sahur.empleados.repository;

import com.sahur.empleados.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByUsuario(String usuario);
    Optional<Empleado> findByUsuario(String usuario);

    boolean existsByUsuarioAndIdEmpleadoNot(String usuario, Long idEmpleado);
}