package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.TipoClase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoClaseRepository extends JpaRepository<TipoClase, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    // Para modificar: revisa si OTRA clase (distinto id) ya tiene ese nombre
    boolean existsByNombreIgnoreCaseAndIdClaseNot(String nombre, Long idClase);
}