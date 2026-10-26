package com.sahur.reservas.repository;

import com.sahur.reservas.model.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    // Un asiento esta ocupado si ya tiene un boleto en ese vuelo que no esta CANCELADO
    boolean existsByIdVueloAndIdAsientoAndEstadoNot(Long idVuelo, Long idAsiento, String estado);

    // Ids de los asientos ocupados de un vuelo (boletos no cancelados)
    @Query("select b.idAsiento from Boleto b where b.idVuelo = :idVuelo and b.estado <> 'CANCELADO'")
    List<Long> asientosOcupados(@Param("idVuelo") Long idVuelo);

    List<Boleto> findByReservaIdReserva(Long idReserva);

    List<Boleto> findByIdVuelo(Long idVuelo);

    List<Boleto> findByIdPasajero(Long idPasajero);
}