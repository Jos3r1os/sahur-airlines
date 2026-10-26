package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {
    List<Asiento> findByAvionIdAvion(Long idAvion);

    boolean existsByAvionIdAvion(Long idAvion);

    long countByAvionIdAvion(Long idAvion);

    boolean existsByTipoClaseIdClase(Long idClase);

    // Para no repetir el numero de asiento dentro del mismo avion
    boolean existsByAvionIdAvionAndNumeroAsientoIgnoreCase(Long idAvion, String numeroAsiento);

    // Para modificar: mismo numero en el mismo avion pero con otro id
    boolean existsByAvionIdAvionAndNumeroAsientoIgnoreCaseAndIdAsientoNot(Long idAvion, String numeroAsiento, Long idAsiento);
}