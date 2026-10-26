package com.sahur.pasajeros.repository;

import com.sahur.pasajeros.model.Pasajero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {
    boolean existsByNumeroDocumento(String numeroDocumento);
    Optional<Pasajero> findByNumeroDocumento(String numeroDocumento);

    // Para modificar: revisa si OTRO pasajero (distinto id) ya tiene ese documento
    boolean existsByNumeroDocumentoAndIdPasajeroNot(String numeroDocumento, Long idPasajero);
}