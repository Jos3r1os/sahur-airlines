package com.sahur.pagos.controller;

import com.sahur.pagos.client.ReservasClient;
import com.sahur.pagos.dto.ReservaDto;
import com.sahur.pagos.model.Pago;
import com.sahur.pagos.repository.PagoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private static final Set<String> METODOS_VALIDOS = Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA");

    private final PagoRepository repository;
    private final ReservasClient reservas;

    public PagoController(PagoRepository repository, ReservasClient reservas) {
        this.repository = repository;
        this.reservas = reservas;
    }

    @GetMapping
    public List<Pago> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reserva/{idReserva}")
    public List<Pago> porReserva(@PathVariable Long idReserva) {
        return repository.findByIdReserva(idReserva);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Pago datos) {
        if (datos.getIdReserva() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El id de la reserva es obligatorio");
        }
        if (datos.getMonto() == null || datos.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El monto debe ser mayor a cero");
        }
        if (datos.getMetodoPago() == null || !METODOS_VALIDOS.contains(datos.getMetodoPago().trim().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Metodo de pago invalido. Usa: EFECTIVO, TARJETA o TRANSFERENCIA");
        }

        ReservaDto reserva = reservas.obtenerReserva(datos.getIdReserva());
        if (reserva == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La reserva no existe");
        }
        if (!"PENDIENTE".equals(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La reserva no esta PENDIENTE de pago (esta " + reserva.getEstado() + ")");
        }
        if (reserva.getTotal() == null || reserva.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La reserva no tiene boletos por pagar");
        }

        if (datos.getMonto().compareTo(reserva.getTotal()) != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El monto debe ser igual al total de la reserva: Q" + reserva.getTotal());
        }

        String error = reservas.marcarPagada(datos.getIdReserva());
        if (error != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        Pago pago = new Pago();
        pago.setIdReserva(datos.getIdReserva());
        pago.setMonto(datos.getMonto());
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodoPago(datos.getMetodoPago().trim().toUpperCase());
        pago.setEstado("COMPLETADO");
        return ResponseEntity.ok(repository.save(pago));
    }

    @PostMapping("/{id}/reembolsar")
    public ResponseEntity<?> reembolsar(@PathVariable Long id) {
        Optional<Pago> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Pago pago = encontrado.get();
        if (!"COMPLETADO".equals(pago.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El pago ya esta " + pago.getEstado());
        }
        ReservaDto reserva = reservas.obtenerReserva(pago.getIdReserva());
        if (reserva == null || !"CANCELADA".equals(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Solo se reembolsa el pago de una reserva CANCELADA");
        }
        pago.setEstado("REEMBOLSADO");
        return ResponseEntity.ok(repository.save(pago));
    }
}