package com.sahur.reservas.repository;

import com.sahur.reservas.model.HistorialBoleto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialBoletoRepository extends JpaRepository<HistorialBoleto, Long> {

    List<HistorialBoleto> findByBoletoIdBoletoOrderByIdHistorialAsc(Long idBoleto);
}