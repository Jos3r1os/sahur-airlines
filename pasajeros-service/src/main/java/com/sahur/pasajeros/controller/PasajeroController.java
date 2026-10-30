package com.sahur.pasajeros.controller;

import com.sahur.pasajeros.client.ReservasClient;
import com.sahur.pasajeros.model.Pasajero;
import com.sahur.pasajeros.repository.PasajeroRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pasajeros")
public class PasajeroController {

    private final PasajeroRepository repository;
    private final ReservasClient reservas;

    public PasajeroController(PasajeroRepository repository, ReservasClient reservas) {
        this.repository = repository;
        this.reservas = reservas;
    }

    @GetMapping
    public List<Pasajero> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pasajero> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/documento/{numero}")
    public ResponseEntity<Pasajero> buscarPorDocumento(@PathVariable String numero) {
        return repository.findByNumeroDocumento(numero)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Pasajero pasajero) {
        String error = validar(pasajero);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByNumeroDocumento(pasajero.getNumeroDocumento())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un pasajero con ese documento");
        }
        pasajero.setIdPasajero(null);
        return ResponseEntity.ok(repository.save(pasajero));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Pasajero datos) {
        Optional<Pasajero> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByNumeroDocumentoAndIdPasajeroNot(datos.getNumeroDocumento(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otro pasajero con ese documento");
        }
        Pasajero pasajero = encontrado.get();
        pasajero.setNumeroDocumento(datos.getNumeroDocumento());
        pasajero.setNombres(datos.getNombres());
        pasajero.setApellidos(datos.getApellidos());
        pasajero.setFechaNacimiento(datos.getFechaNacimiento());
        pasajero.setTelefono(datos.getTelefono());
        pasajero.setCorreo(datos.getCorreo());
        return ResponseEntity.ok(repository.save(pasajero));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (reservas.tieneReservas(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el pasajero tiene reservas registradas");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Pasajero eliminado");
    }

    private String validar(Pasajero p) {
        if (vacio(p.getNumeroDocumento())) {
            return "El numero de documento es obligatorio";
        }
        if (vacio(p.getNombres())) {
            return "Los nombres son obligatorios";
        }
        if (vacio(p.getApellidos())) {
            return "Los apellidos son obligatorios";
        }
        if (p.getFechaNacimiento() != null && p.getFechaNacimiento().isAfter(LocalDate.now())) {
            return "La fecha de nacimiento no puede ser en el futuro";
        }
        if (!vacio(p.getCorreo()) && !p.getCorreo().contains("@")) {
            return "El correo no es valido";
        }
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}