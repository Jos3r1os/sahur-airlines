package com.sahur.vuelos.controller;

import com.sahur.vuelos.model.Aeropuerto;
import com.sahur.vuelos.repository.AeropuertoRepository;
import com.sahur.vuelos.repository.RutaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/aeropuertos")
public class AeropuertoController {

    private final AeropuertoRepository repository;
    private final RutaRepository rutaRepository;

    public AeropuertoController(AeropuertoRepository repository, RutaRepository rutaRepository) {
        this.repository = repository;
        this.rutaRepository = rutaRepository;
    }

    @GetMapping
    public List<Aeropuerto> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aeropuerto> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Aeropuerto aeropuerto) {
        String error = validar(aeropuerto);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        aeropuerto.setCodigoIata(aeropuerto.getCodigoIata().trim().toUpperCase());
        if (repository.existsByCodigoIata(aeropuerto.getCodigoIata())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un aeropuerto con ese codigo IATA");
        }
        aeropuerto.setIdAeropuerto(null);
        return ResponseEntity.ok(repository.save(aeropuerto));
    }

    // Modificar: PUT /aeropuertos/1
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Aeropuerto datos) {
        Optional<Aeropuerto> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Aeropuerto aeropuerto = encontrado.get();
        String codigoNuevo = datos.getCodigoIata().trim().toUpperCase();

        if (repository.existsByCodigoIataAndIdAeropuertoNot(codigoNuevo, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otro aeropuerto con ese codigo IATA");
        }
        // Las rutas guardan el codigo IATA: si se cambia, esas rutas quedarian apuntando a un codigo que no existe
        if (!codigoNuevo.equals(aeropuerto.getCodigoIata())
                && rutaRepository.existsByAeropuertoOrigenOrAeropuertoDestino(aeropuerto.getCodigoIata(), aeropuerto.getCodigoIata())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede cambiar el codigo IATA: hay rutas que usan este aeropuerto");
        }

        aeropuerto.setNombre(datos.getNombre());
        aeropuerto.setCodigoIata(codigoNuevo);
        aeropuerto.setCiudad(datos.getCiudad());
        aeropuerto.setPais(datos.getPais());
        return ResponseEntity.ok(repository.save(aeropuerto));
    }

    // Eliminar: DELETE /aeropuertos/1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Optional<Aeropuerto> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String codigo = encontrado.get().getCodigoIata();
        if (rutaRepository.existsByAeropuertoOrigenOrAeropuertoDestino(codigo, codigo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: hay rutas que usan este aeropuerto");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Aeropuerto eliminado");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    private String validar(Aeropuerto a) {
        if (vacio(a.getNombre())) {
            return "El nombre es obligatorio";
        }
        if (vacio(a.getCodigoIata()) || !a.getCodigoIata().trim().matches("[A-Za-z]{3}")) {
            return "El codigo IATA debe tener exactamente 3 letras";
        }
        if (vacio(a.getCiudad())) {
            return "La ciudad es obligatoria";
        }
        if (vacio(a.getPais())) {
            return "El pais es obligatorio";
        }
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}