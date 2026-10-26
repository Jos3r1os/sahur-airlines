package com.sahur.vuelos.controller;

import com.sahur.vuelos.client.ReservasClient;
import com.sahur.vuelos.model.Asiento;
import com.sahur.vuelos.model.Avion;
import com.sahur.vuelos.model.TipoClase;
import com.sahur.vuelos.repository.AsientoRepository;
import com.sahur.vuelos.repository.AvionRepository;
import com.sahur.vuelos.repository.TipoClaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/asientos")
public class AsientoController {

    private static final Set<String> UBICACIONES_VALIDAS = Set.of("VENTANA", "PASILLO", "CENTRO");

    private final AsientoRepository repository;
    private final AvionRepository avionRepository;
    private final TipoClaseRepository tipoClaseRepository;
    private final ReservasClient reservas;

    public AsientoController(AsientoRepository repository,
                             AvionRepository avionRepository,
                             TipoClaseRepository tipoClaseRepository,
                             ReservasClient reservas) {
        this.repository = repository;
        this.avionRepository = avionRepository;
        this.tipoClaseRepository = tipoClaseRepository;
        this.reservas = reservas;
    }

    @GetMapping
    public List<Asiento> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asiento> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Asientos de un avion: GET /asientos/avion/1
    @GetMapping("/avion/{idAvion}")
    public List<Asiento> porAvion(@PathVariable Long idAvion) {
        return repository.findByAvionIdAvion(idAvion);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Asiento asiento) {
        String error = validar(asiento);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Long idAvion = asiento.getAvion().getIdAvion();
        if (repository.existsByAvionIdAvionAndNumeroAsientoIgnoreCase(idAvion, asiento.getNumeroAsiento())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ese numero de asiento ya existe en el avion");
        }
        // No se pueden registrar mas asientos que la capacidad del avion
        if (repository.countByAvionIdAvion(idAvion) >= asiento.getAvion().getCapacidad()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El avion ya tiene todos sus asientos (capacidad " + asiento.getAvion().getCapacidad() + ")");
        }
        asiento.setIdAsiento(null);
        return ResponseEntity.ok(repository.save(asiento));
    }

    // Modificar: PUT /asientos/1
    // No se puede mover un asiento a otro avion: solo cambia numero, clase y ubicacion
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Asiento datos) {
        Optional<Asiento> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Asiento asiento = encontrado.get();
        // El avion siempre es el que ya tenia
        datos.setAvion(asiento.getAvion());
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByAvionIdAvionAndNumeroAsientoIgnoreCaseAndIdAsientoNot(
                asiento.getAvion().getIdAvion(), datos.getNumeroAsiento(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ese numero de asiento ya existe en el avion");
        }
        asiento.setNumeroAsiento(datos.getNumeroAsiento());
        asiento.setTipoClase(datos.getTipoClase());
        asiento.setUbicacion(datos.getUbicacion());
        return ResponseEntity.ok(repository.save(asiento));
    }

    // Eliminar: DELETE /asientos/1
    // Si el asiento aparece en algun boleto no se elimina (se perderia el historial de ventas)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (reservas.asientoTieneBoletos(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el asiento tiene boletos registrados");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Asiento eliminado");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    // Tambien cambia el avion y la clase que vienen en el JSON (solo con id) por los completos de la base.
    private String validar(Asiento a) {
        if (a.getAvion() == null || a.getAvion().getIdAvion() == null) {
            return "El avion es obligatorio";
        }
        if (a.getTipoClase() == null || a.getTipoClase().getIdClase() == null) {
            return "La clase es obligatoria";
        }
        Optional<Avion> avion = avionRepository.findById(a.getAvion().getIdAvion());
        if (avion.isEmpty()) {
            return "El avion no existe";
        }
        Optional<TipoClase> clase = tipoClaseRepository.findById(a.getTipoClase().getIdClase());
        if (clase.isEmpty()) {
            return "La clase no existe";
        }
        if (a.getNumeroAsiento() == null || a.getNumeroAsiento().isBlank()) {
            return "El numero de asiento es obligatorio";
        }
        if (a.getUbicacion() == null || !UBICACIONES_VALIDAS.contains(a.getUbicacion().toUpperCase())) {
            return "Ubicacion invalida. Usa: VENTANA, PASILLO o CENTRO";
        }
        a.setAvion(avion.get());
        a.setTipoClase(clase.get());
        a.setNumeroAsiento(a.getNumeroAsiento().trim().toUpperCase());
        a.setUbicacion(a.getUbicacion().toUpperCase());
        return null;
    }
}