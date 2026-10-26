package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    // Hay alguna ruta que sale de o llega a este aeropuerto (codigo IATA)?
    boolean existsByAeropuertoOrigenOrAeropuertoDestino(String origen, String destino);

    // Para no repetir la misma ruta (mismo origen y destino)
    boolean existsByAeropuertoOrigenAndAeropuertoDestino(String origen, String destino);

    // Para modificar: misma ruta pero con otro id
    boolean existsByAeropuertoOrigenAndAeropuertoDestinoAndIdRutaNot(String origen, String destino, Long idRuta);
}