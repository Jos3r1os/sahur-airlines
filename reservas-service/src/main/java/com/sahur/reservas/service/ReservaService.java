package com.sahur.reservas.service;

import com.sahur.reservas.model.Boleto;
import com.sahur.reservas.model.HistorialBoleto;
import com.sahur.reservas.model.Reserva;
import com.sahur.reservas.repository.BoletoRepository;
import com.sahur.reservas.repository.HistorialBoletoRepository;
import com.sahur.reservas.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final BoletoRepository boletoRepository;
    private final HistorialBoletoRepository historialRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          BoletoRepository boletoRepository,
                          HistorialBoletoRepository historialRepository) {
        this.reservaRepository = reservaRepository;
        this.boletoRepository = boletoRepository;
        this.historialRepository = historialRepository;
    }

    // Cambia el estado del boleto y deja el registro en el historial
    public void cambiarEstado(Boleto boleto, String estadoNuevo, String usuario) {
        String anterior = boleto.getEstado();
        boleto.setEstado(estadoNuevo);
        boletoRepository.save(boleto);
        registrarHistorial(boleto, anterior, estadoNuevo, usuario);
    }

    public void registrarHistorial(Boleto boleto, String anterior, String nuevo, String usuario) {
        HistorialBoleto historial = new HistorialBoleto();
        historial.setBoleto(boleto);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setUsuarioResponsable(usuarioONulo(usuario));
        historialRepository.save(historial);
    }


    public Reserva recalcularTotal(Reserva reserva) {
        BigDecimal total = boletoRepository.findByReservaIdReserva(reserva.getIdReserva()).stream()
                .filter(b -> !"CANCELADO".equals(b.getEstado()))
                .map(Boleto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reserva.setTotal(total);
        return reservaRepository.save(reserva);
    }


    public String usuarioONulo(String usuario) {
        return (usuario == null || usuario.isBlank()) ? "SISTEMA" : usuario.trim();
    }
}