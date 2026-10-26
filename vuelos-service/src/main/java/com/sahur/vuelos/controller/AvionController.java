package com.sahur.vuelos.controller;

import com.sahur.vuelos.model.Avion;
import com.sahur.vuelos.repository.AsientoRepository;
import com.sahur.vuelos.repository.AvionRepository;
import com.sahur.vuelos.repository.VueloRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/aviones")
public class AvionController {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVO", "MANTENIMIENTO", "INACTIVO");

    private final AvionRepository repository;
    private final VueloRepository vueloRepository;
    private final AsientoRepository asientoRepository;

    public AvionController(AvionRepository repository,
                           VueloRepository vueloRepository,
                           AsientoRepository asientoRepository) {
        this.repository = repository;
        this.vueloRepository = vueloRepository;
        this.asientoRepository = asientoRepository;
    }

    @GetMapping
    public List<Avion> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Avion> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Avion avion) {
        // Si no mandan estado, el avion nuevo queda ACTIVO
        if (vacio(avion.getEstado())) {
            avion.setEstado("ACTIVO");
        }
        String error = validar(avion);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        avion.setMatricula(avion.getMatricula().trim().toUpperCase());
        if (repository.existsByMatricula(avion.getMatricula())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un avion con esa matricula");
        }
        avion.setIdAvion(null);
        avion.setEstado(avion.getEstado().toUpperCase());
        return ResponseEntity.ok(repository.save(avion));
    }

    // Modificar: PUT /aviones/1
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Avion datos) {
        Optional<Avion> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        String matricula = datos.getMatricula().trim().toUpperCase();
        if (repository.existsByMatriculaAndIdAvionNot(matricula, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otro avion con esa matricula");
        }
        // La capacidad no puede quedar por debajo de los asientos que ya tiene registrados
        long asientos = asientoRepository.countByAvionIdAvion(id);
        if (datos.getCapacidad() < asientos) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La capacidad no puede ser menor a los " + asientos + " asientos que ya tiene el avion");
        }

        Avion avion = encontrado.get();
        avion.setMatricula(matricula);
        avion.setModelo(datos.getModelo());
        avion.setCapacidad(datos.getCapacidad());
        avion.setEstado(datos.getEstado().toUpperCase());
        return ResponseEntity.ok(repository.save(avion));
    }

    // Eliminar: DELETE /aviones/1
    // No se elimina si tiene vuelos o asientos; en ese caso se pone INACTIVO con PUT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (vueloRepository.existsByAvionIdAvion(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el avion tiene vuelos. Cambia su estado a INACTIVO");
        }
        if (asientoRepository.existsByAvionIdAvion(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el avion tiene asientos registrados");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Avion eliminado");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    private String validar(Avion a) {
        if (vacio(a.getMatricula())) {
            return "La matricula es obligatoria";
        }
        if (vacio(a.getModelo())) {
            return "El modelo es obligatorio";
        }
        if (a.getCapacidad() == null || a.getCapacidad() <= 0) {
            return "La capacidad debe ser mayor a cero";
        }
        if (vacio(a.getEstado()) || !ESTADOS_VALIDOS.contains(a.getEstado().toUpperCase())) {
            return "Estado invalido. Usa: ACTIVO, MANTENIMIENTO o INACTIVO";
        }
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}