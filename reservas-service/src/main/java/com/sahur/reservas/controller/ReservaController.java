package com.sahur.reservas.controller;

import com.sahur.reservas.client.ServiciosClient;
import com.sahur.reservas.dto.EmpleadoDto;
import com.sahur.reservas.dto.UsuarioRequest;
import com.sahur.reservas.model.Boleto;
import com.sahur.reservas.model.Reserva;
import com.sahur.reservas.repository.BoletoRepository;
import com.sahur.reservas.repository.ReservaRepository;
import com.sahur.reservas.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaRepository repository;
    private final BoletoRepository boletoRepository;
    private final ReservaService reservaService;
    private final ServiciosClient servicios;

    public ReservaController(ReservaRepository repository,
                             BoletoRepository boletoRepository,
                             ReservaService reservaService,
                             ServiciosClient servicios) {
        this.repository = repository;
        this.boletoRepository = boletoRepository;
        this.reservaService = reservaService;
        this.servicios = servicios;
    }

    @GetMapping
    public List<Reserva> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/boletos")
    public ResponseEntity<?> boletos(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(boletoRepository.findByReservaIdReserva(id));
    }

    @GetMapping("/pasajero/{idPasajero}")
    public List<Reserva> porPasajero(@PathVariable Long idPasajero) {
        return repository.findByIdPasajero(idPasajero);
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Reserva> porEmpleado(@PathVariable Long idEmpleado) {
        return repository.findByIdEmpleado(idEmpleado);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Reserva datos) {
        if (datos.getIdPasajero() == null || datos.getIdEmpleado() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("idPasajero e idEmpleado son obligatorios");
        }
        if (!servicios.pasajeroExiste(datos.getIdPasajero())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El pasajero no existe");
        }
        EmpleadoDto empleado = servicios.obtenerEmpleado(datos.getIdEmpleado());
        if (empleado == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El empleado no existe");
        }
        if ("INACTIVO".equals(empleado.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El empleado esta INACTIVO y no puede vender");
        }

        Reserva reserva = new Reserva();
        reserva.setIdPasajero(datos.getIdPasajero());
        reserva.setIdEmpleado(datos.getIdEmpleado());
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado("PENDIENTE");
        reserva.setTotal(BigDecimal.ZERO);
        return ResponseEntity.ok(repository.save(reserva));
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody(required = false) UsuarioRequest request) {
        Optional<Reserva> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Reserva reserva = encontrada.get();
        if ("CANCELADA".equals(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La reserva ya esta CANCELADA");
        }

        List<Boleto> boletos = boletoRepository.findByReservaIdReserva(id);
        if (boletos.stream().anyMatch(b -> "USADO".equals(b.getEstado()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede cancelar: la reserva tiene boletos USADOS");
        }

        String usuario = request == null ? null : request.getUsuario();
        for (Boleto boleto : boletos) {
            if (!"CANCELADO".equals(boleto.getEstado())) {
                reservaService.cambiarEstado(boleto, "CANCELADO", usuario);
            }
        }
        reserva.setEstado("CANCELADA");
        return ResponseEntity.ok(repository.save(reserva));
    }

    @PostMapping("/{id}/pagar")
    @Transactional
    public ResponseEntity<?> pagar(@PathVariable Long id) {
        Optional<Reserva> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Reserva reserva = encontrada.get();
        if (!"PENDIENTE".equals(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La reserva no esta PENDIENTE de pago (esta " + reserva.getEstado() + ")");
        }

        List<Boleto> emitidos = boletoRepository.findByReservaIdReserva(id).stream()
                .filter(b -> "EMITIDO".equals(b.getEstado()))
                .toList();
        if (emitidos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La reserva no tiene boletos pendientes de pago (EMITIDO)");
        }

        for (Boleto boleto : emitidos) {
            reservaService.cambiarEstado(boleto, "PAGADO", "PAGOS");
        }
        reserva.setEstado("PAGADA");
        return ResponseEntity.ok(repository.save(reserva));
    }
}