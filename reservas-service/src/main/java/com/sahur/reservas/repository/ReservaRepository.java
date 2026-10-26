package com.sahur.reservas.repository;

import com.sahur.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByIdPasajero(Long idPasajero);

    List<Reserva> findByIdEmpleado(Long idEmpleado);
}