package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
   
    boolean existsByAeropuertoOrigenOrAeropuertoDestino(String origen, String destino);

    boolean existsByAeropuertoOrigenAndAeropuertoDestino(String origen, String destino);

    boolean existsByAeropuertoOrigenAndAeropuertoDestinoAndIdRutaNot(String origen, String destino, Long idRuta);
}