package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface VueloRepository extends JpaRepository<Vuelo, Long> {
    boolean existsByAvionIdAvion(Long idAvion);

    boolean existsByRutaIdRuta(Long idRuta);

    boolean existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfter(
            Long idAvion, String estado, LocalDateTime llegada, LocalDateTime salida);

    boolean existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfterAndIdVueloNot(
            Long idAvion, String estado, LocalDateTime llegada, LocalDateTime salida, Long idVuelo);
}