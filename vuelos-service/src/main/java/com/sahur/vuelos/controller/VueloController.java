package com.sahur.vuelos.controller;

import com.sahur.vuelos.client.ReservasClient;
import com.sahur.vuelos.model.Avion;
import com.sahur.vuelos.model.Ruta;
import com.sahur.vuelos.model.Vuelo;
import com.sahur.vuelos.repository.AvionRepository;
import com.sahur.vuelos.repository.RutaRepository;
import com.sahur.vuelos.repository.VueloRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/vuelos")
public class VueloController {

    private static final Set<String> ESTADOS_VALIDOS =
            Set.of("PROGRAMADO", "RETRASADO", "EN_VUELO", "FINALIZADO", "CANCELADO");
    // Solo en estos estados se pueden vender boletos
    private static final Set<String> ESTADOS_VENTA = Set.of("PROGRAMADO", "RETRASADO");

    private final VueloRepository repository;
    private final RutaRepository rutaRepository;
    private final AvionRepository avionRepository;
    private final ReservasClient reservas;

    public VueloController(VueloRepository repository,
                           RutaRepository rutaRepository,
                           AvionRepository avionRepository,
                           ReservasClient reservas) {
        this.repository = repository;
        this.rutaRepository = rutaRepository;
        this.avionRepository = avionRepository;
        this.reservas = reservas;
    }

    @GetMapping
    public List<Vuelo> listar() {
        return repository.findAll();
    }

    // Vuelos disponibles para vender: GET /vuelos/disponibles?origen=GUA&destino=MIA&fecha=2026-11-15
    // (origen, destino y fecha son opcionales)
    @GetMapping("/disponibles")
    public List<Vuelo> disponibles(@RequestParam(required = false) String origen,
                                   @RequestParam(required = false) String destino,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now();
        return repository.findAll().stream()
                .filter(v -> v.getEstado() != null && ESTADOS_VENTA.contains(v.getEstado()))
                .filter(v -> v.getFechaSalida().isAfter(ahora))
                .filter(v -> origen == null || v.getRuta().getAeropuertoOrigen().equalsIgnoreCase(origen))
                .filter(v -> destino == null || v.getRuta().getAeropuertoDestino().equalsIgnoreCase(destino))
                .filter(v -> fecha == null || v.getFechaSalida().toLocalDate().equals(fecha))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vuelo> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Vuelo vuelo) {
        // Si no mandan estado, el vuelo nuevo queda PROGRAMADO
        if (vacio(vuelo.getEstado())) {
            vuelo.setEstado("PROGRAMADO");
        }
        String error = validar(vuelo);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (!vuelo.getFechaSalida().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La fecha de salida debe ser en el futuro");
        }
        if (repository.existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfter(
                vuelo.getAvion().getIdAvion(), "CANCELADO", vuelo.getFechaLlegada(), vuelo.getFechaSalida())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El avion ya tiene otro vuelo en ese horario");
        }
        vuelo.setIdVuelo(null);
        return ResponseEntity.ok(repository.save(vuelo));
    }

    // Modificar: PUT /vuelos/1
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Vuelo datos) {
        Optional<Vuelo> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Vuelo vuelo = encontrado.get();

        // Si ya se vendieron boletos, no se puede cambiar ni el avion (los asientos son de ese avion) ni la ruta
        boolean cambiaAvion = !datos.getAvion().getIdAvion().equals(vuelo.getAvion().getIdAvion());
        boolean cambiaRuta = !datos.getRuta().getIdRuta().equals(vuelo.getRuta().getIdRuta());
        if ((cambiaAvion || cambiaRuta) && reservas.vueloTieneBoletos(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede cambiar avion ni ruta: el vuelo ya tiene boletos vendidos");
        }
        if (repository.existsByAvionIdAvionAndEstadoNotAndFechaSalidaBeforeAndFechaLlegadaAfterAndIdVueloNot(
                datos.getAvion().getIdAvion(), "CANCELADO", datos.getFechaLlegada(), datos.getFechaSalida(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El avion ya tiene otro vuelo en ese horario");
        }

        vuelo.setRuta(datos.getRuta());
        vuelo.setAvion(datos.getAvion());
        vuelo.setFechaSalida(datos.getFechaSalida());
        vuelo.setFechaLlegada(datos.getFechaLlegada());
        vuelo.setPrecioBase(datos.getPrecioBase());
        vuelo.setEstado(datos.getEstado());
        return ResponseEntity.ok(repository.save(vuelo));
    }

    // Eliminar: DELETE /vuelos/1
    // Si ya tiene boletos no se elimina; en ese caso se pone CANCELADO con PUT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (reservas.vueloTieneBoletos(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el vuelo tiene boletos. Cambia su estado a CANCELADO");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Vuelo eliminado");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    // Tambien cambia la ruta y el avion que vienen en el JSON (solo con id) por los completos de la base,
    // para que la respuesta traiga todos sus datos.
    private String validar(Vuelo v) {
        if (v.getRuta() == null || v.getRuta().getIdRuta() == null) {
            return "La ruta es obligatoria";
        }
        if (v.getAvion() == null || v.getAvion().getIdAvion() == null) {
            return "El avion es obligatorio";
        }
        Optional<Ruta> ruta = rutaRepository.findById(v.getRuta().getIdRuta());
        if (ruta.isEmpty()) {
            return "La ruta no existe";
        }
        Optional<Avion> avion = avionRepository.findById(v.getAvion().getIdAvion());
        if (avion.isEmpty()) {
            return "El avion no existe";
        }
        if (!"ACTIVO".equals(avion.get().getEstado())) {
            return "El avion no esta ACTIVO";
        }
        if (v.getFechaSalida() == null || v.getFechaLlegada() == null) {
            return "La fecha de salida y la de llegada son obligatorias";
        }
        if (!v.getFechaLlegada().isAfter(v.getFechaSalida())) {
            return "La fecha de llegada debe ser despues de la salida";
        }
        if (v.getPrecioBase() == null || v.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
            return "El precio base debe ser mayor a cero";
        }
        if (vacio(v.getEstado()) || !ESTADOS_VALIDOS.contains(v.getEstado().toUpperCase())) {
            return "Estado invalido. Usa: PROGRAMADO, RETRASADO, EN_VUELO, FINALIZADO o CANCELADO";
        }
        v.setRuta(ruta.get());
        v.setAvion(avion.get());
        v.setEstado(v.getEstado().toUpperCase());
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}