package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Avion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvionRepository extends JpaRepository<Avion, Long> {
    boolean existsByMatricula(String matricula);

    boolean existsByMatriculaAndIdAvionNot(String matricula, Long idAvion);
}