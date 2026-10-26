package com.sahur.reservas.controller;

import com.sahur.reservas.client.ServiciosClient;
import com.sahur.reservas.dto.AsientoDto;
import com.sahur.reservas.dto.BoletoRequest;
import com.sahur.reservas.dto.EstadoRequest;
import com.sahur.reservas.dto.TipoClaseDto;
import com.sahur.reservas.dto.VueloDto;
import com.sahur.reservas.model.Boleto;
import com.sahur.reservas.model.Reserva;
import com.sahur.reservas.repository.BoletoRepository;
import com.sahur.reservas.repository.HistorialBoletoRepository;
import com.sahur.reservas.repository.ReservaRepository;
import com.sahur.reservas.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/boletos")
public class BoletoController {

    // Solo se venden boletos de vuelos en estos estados
    private static final Set<String> ESTADOS_VUELO_VENTA = Set.of("PROGRAMADO", "RETRASADO");

    // Cambios de estado que se pueden hacer a mano.
    // A PAGADO no se llega desde aqui: solo cuando se registra el pago en pagos-service.
    private static final Map<String, Set<String>> CAMBIOS_PERMITIDOS = Map.of(
            "EMITIDO", Set.of("CANCELADO"),
            "PAGADO", Set.of("USADO", "CANCELADO"),
            "CANCELADO", Set.of(),
            "USADO", Set.of()
    );

    private final BoletoRepository boletoRepository;
    private final ReservaRepository reservaRepository;
    private final HistorialBoletoRepository historialRepository;
    private final ReservaService reservaService;
    private final ServiciosClient servicios;

    public BoletoController(BoletoRepository boletoRepository,
                            ReservaRepository reservaRepository,
                            HistorialBoletoRepository historialRepository,
                            ReservaService reservaService,
                            ServiciosClient servicios) {
        this.boletoRepository = boletoRepository;
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.reservaService = reservaService;
        this.servicios = servicios;
    }

    @GetMapping
    public List<Boleto> listar() {
        return boletoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Boleto> buscar(@PathVariable Long id) {
        return boletoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Historial de cambios de un boleto: GET /boletos/1/historial
    @GetMapping("/{id}/historial")
    public ResponseEntity<?> historial(@PathVariable Long id) {
        if (!boletoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(historialRepository.findByBoletoIdBoletoOrderByIdHistorialAsc(id));
    }

    // Boletos vendidos de un vuelo: GET /boletos/vuelo/1
    @GetMapping("/vuelo/{idVuelo}")
    public List<Boleto> porVuelo(@PathVariable Long idVuelo) {
        return boletoRepository.findByIdVuelo(idVuelo);
    }

    // Boletos de un pasajero: GET /boletos/pasajero/1
    @GetMapping("/pasajero/{idPasajero}")
    public List<Boleto> porPasajero(@PathVariable Long idPasajero) {
        return boletoRepository.findByIdPasajero(idPasajero);
    }

    // Asientos libres de un vuelo: GET /boletos/vuelo/1/asientos-disponibles
    // Los asientos estan en vuelos-service y los boletos aqui, por eso se combinan en este servicio
    @GetMapping("/vuelo/{idVuelo}/asientos-disponibles")
    public ResponseEntity<?> asientosDisponibles(@PathVariable Long idVuelo) {
        VueloDto vuelo = servicios.obtenerVuelo(idVuelo);
        if (vuelo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El vuelo no existe");
        }
        if (vuelo.getEstado() == null || !ESTADOS_VUELO_VENTA.contains(vuelo.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El vuelo no esta a la venta (estado " + vuelo.getEstado() + ")");
        }

        Set<Long> ocupados = new HashSet<>(boletoRepository.asientosOcupados(idVuelo));

        List<AsientoDto> disponibles = servicios.obtenerAsientosDeAvion(vuelo.getAvion().getIdAvion())
                .stream()
                .filter(asiento -> !ocupados.contains(asiento.getIdAsiento()))
                .toList();

        return ResponseEntity.ok(disponibles);
    }

    // Emitir boleto: POST /boletos con {"idReserva":1, "idVuelo":1, "idAsiento":1, "usuario":"admin"}
    @PostMapping
    @Transactional
    public ResponseEntity<?> emitir(@RequestBody BoletoRequest request) {
        if (request.getIdReserva() == null || request.getIdVuelo() == null || request.getIdAsiento() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("idReserva, idVuelo e idAsiento son obligatorios");
        }

        // 1. La reserva debe existir y estar PENDIENTE
        Optional<Reserva> reserva = reservaRepository.findById(request.getIdReserva());
        if (reserva.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La reserva no existe");
        }
        if (!"PENDIENTE".equals(reserva.get().getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Solo se pueden agregar boletos a una reserva PENDIENTE (esta " + reserva.get().getEstado() + ")");
        }

        // 2. El vuelo debe existir, estar a la venta y no haber salido
        VueloDto vuelo = servicios.obtenerVuelo(request.getIdVuelo());
        if (vuelo == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El vuelo no existe");
        }
        if (vuelo.getEstado() == null || !ESTADOS_VUELO_VENTA.contains(vuelo.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El vuelo no esta a la venta (estado " + vuelo.getEstado() + ")");
        }
        if (vuelo.getFechaSalida() != null && !vuelo.getFechaSalida().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El vuelo ya salio");
        }
        if (vuelo.getPrecioBase() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El vuelo no tiene precio base");
        }

        // 3. El asiento debe existir y ser del avion de ese vuelo
        AsientoDto asiento = servicios.obtenerAsiento(request.getIdAsiento());
        if (asiento == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El asiento no existe");
        }
        if (!asiento.getAvion().getIdAvion().equals(vuelo.getAvion().getIdAvion())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El asiento no pertenece al avion de este vuelo");
        }

        // 4. El asiento no debe estar ocupado en ese vuelo
        if (boletoRepository.existsByIdVueloAndIdAsientoAndEstadoNot(
                request.getIdVuelo(), request.getIdAsiento(), "CANCELADO")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ese asiento ya esta ocupado en este vuelo");
        }

        // Todo bien: armamos el boleto
        Boleto boleto = new Boleto();
        boleto.setReserva(reserva.get());
        boleto.setIdVuelo(request.getIdVuelo());
        boleto.setIdPasajero(reserva.get().getIdPasajero());
        boleto.setIdAsiento(request.getIdAsiento());
        boleto.setCodigoBoleto("SAH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        boleto.setPrecio(calcularPrecio(vuelo.getPrecioBase(), asiento.getTipoClase()));
        boleto.setEstado("EMITIDO");
        Boleto guardado = boletoRepository.save(boleto);

        // Primer registro del historial y nuevo total de la reserva
        reservaService.registrarHistorial(guardado, null, "EMITIDO", request.getUsuario());
        reservaService.recalcularTotal(reserva.get());

        return ResponseEntity.ok(guardado);
    }

    // Cambiar el estado: POST /boletos/1/estado con {"estado":"CANCELADO", "usuario":"admin"}
    @PostMapping("/{id}/estado")
    @Transactional
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        Optional<Boleto> encontrado = boletoRepository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (request.getEstado() == null || request.getEstado().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El estado es obligatorio");
        }
        String nuevo = request.getEstado().trim().toUpperCase();
        if (!CAMBIOS_PERMITIDOS.containsKey(nuevo)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Estado invalido. Usa: EMITIDO, PAGADO, CANCELADO o USADO");
        }
        if ("PAGADO".equals(nuevo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Un boleto solo pasa a PAGADO al registrar el pago en pagos-service");
        }

        Boleto boleto = encontrado.get();
        String actual = boleto.getEstado();
        if (!CAMBIOS_PERMITIDOS.getOrDefault(actual, Set.of()).contains(nuevo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede pasar un boleto de " + actual + " a " + nuevo);
        }

        reservaService.cambiarEstado(boleto, nuevo, request.getUsuario());

        // Si la reserva aun no se paga, su total cambia (un boleto cancelado ya no se cobra)
        if ("PENDIENTE".equals(boleto.getReserva().getEstado())) {
            reservaService.recalcularTotal(boleto.getReserva());
        }
        return ResponseEntity.ok(boleto);
    }

    // Precio = precio base del vuelo + porcentaje de recargo de la clase del asiento
    private BigDecimal calcularPrecio(BigDecimal precioBase, TipoClaseDto clase) {
        double recargo = (clase == null || clase.getPorcentajeRecargo() == null) ? 0 : clase.getPorcentajeRecargo();
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(recargo).divide(BigDecimal.valueOf(100)));
        return precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}