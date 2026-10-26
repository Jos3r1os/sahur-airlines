package com.sahur.vuelos.repository;

import com.sahur.vuelos.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface VueloRepository extends JpaRepository<Vuelo, Long> {
    boolean existsByAvionIdAvion(Long idAvion);

    boolean existsByRutaIdRuta(Long idRuta);

    // El avion ya tiene otro vuelo (no cancelado) que se cruza con este horario?
    // Se cruzan si el otro sale antes de que este llegue, y llega despues de que este sale.
    boolean existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfter(
            Long idAvion, String estado, LocalDateTime llegada, LocalDateTime salida);

    // Lo mismo, pero sin contar el vuelo que se esta modificando
    boolean existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfterAndIdVueloNot(
            Long idAvion, String estado, LocalDateTime llegada, LocalDateTime salida, Long idVuelo);
}