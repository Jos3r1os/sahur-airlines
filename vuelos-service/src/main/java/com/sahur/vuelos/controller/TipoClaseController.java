package com.sahur.vuelos.controller;

import com.sahur.vuelos.model.TipoClase;
import com.sahur.vuelos.repository.AsientoRepository;
import com.sahur.vuelos.repository.TipoClaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/clases")
public class TipoClaseController {

    private final TipoClaseRepository repository;
    private final AsientoRepository asientoRepository;

    public TipoClaseController(TipoClaseRepository repository, AsientoRepository asientoRepository) {
        this.repository = repository;
        this.asientoRepository = asientoRepository;
    }

    @GetMapping
    public List<TipoClase> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoClase> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoClase tipoClase) {
        String error = validar(tipoClase);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByNombreIgnoreCase(tipoClase.getNombre().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una clase con ese nombre");
        }
        tipoClase.setIdClase(null);
        tipoClase.setNombre(tipoClase.getNombre().trim());
        return ResponseEntity.ok(repository.save(tipoClase));
    }

    // Modificar: PUT /clases/1
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody TipoClase datos) {
        Optional<TipoClase> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByNombreIgnoreCaseAndIdClaseNot(datos.getNombre().trim(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otra clase con ese nombre");
        }
        TipoClase tipoClase = encontrada.get();
        tipoClase.setNombre(datos.getNombre().trim());
        tipoClase.setPorcentajeRecargo(datos.getPorcentajeRecargo());
        return ResponseEntity.ok(repository.save(tipoClase));
    }

    // Eliminar: DELETE /clases/1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (asientoRepository.existsByTipoClaseIdClase(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: hay asientos de esta clase");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Clase eliminada");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    private String validar(TipoClase c) {
        if (c.getNombre() == null || c.getNombre().isBlank()) {
            return "El nombre es obligatorio";
        }
        if (c.getPorcentajeRecargo() == null || c.getPorcentajeRecargo() < 0) {
            return "El porcentaje de recargo es obligatorio y no puede ser negativo";
        }
        return null;
    }
}