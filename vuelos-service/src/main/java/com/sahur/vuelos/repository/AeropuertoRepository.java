package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {
    boolean existsByCodigoIata(String codigoIata);

    // Para modificar: revisa si OTRO aeropuerto (distinto id) ya tiene ese codigo
    boolean existsByCodigoIataAndIdAeropuertoNot(String codigoIata, Long idAeropuerto);
}