package com.sahur.vuelos.controller;

import com.sahur.vuelos.model.Ruta;
import com.sahur.vuelos.repository.AeropuertoRepository;
import com.sahur.vuelos.repository.RutaRepository;
import com.sahur.vuelos.repository.VueloRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rutas")
public class RutaController {

    private final RutaRepository rutaRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;

    public RutaController(RutaRepository rutaRepository,
                          AeropuertoRepository aeropuertoRepository,
                          VueloRepository vueloRepository) {
        this.rutaRepository = rutaRepository;
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
    }

    @GetMapping
    public List<Ruta> listar() {
        return rutaRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ruta> buscar(@PathVariable Long id) {
        return rutaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Ruta ruta) {
        String error = validar(ruta);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (rutaRepository.existsByAeropuertoOrigenAndAeropuertoDestino(ruta.getAeropuertoOrigen(), ruta.getAeropuertoDestino())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una ruta con ese origen y destino");
        }
        ruta.setIdRuta(null);
        return ResponseEntity.ok(rutaRepository.save(ruta));
    }

    // Modificar: PUT /rutas/1
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Ruta datos) {
        Optional<Ruta> encontrada = rutaRepository.findById(id);
        if (encontrada.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Ruta ruta = encontrada.get();
        boolean cambiaAeropuertos = !datos.getAeropuertoOrigen().equals(ruta.getAeropuertoOrigen())
                || !datos.getAeropuertoDestino().equals(ruta.getAeropuertoDestino());

        // Si ya hay vuelos en esta ruta, no se puede cambiar de donde sale ni a donde llega
        if (cambiaAeropuertos && vueloRepository.existsByRutaIdRuta(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede cambiar origen o destino: la ruta ya tiene vuelos");
        }
        if (rutaRepository.existsByAeropuertoOrigenAndAeropuertoDestinoAndIdRutaNot(
                datos.getAeropuertoOrigen(), datos.getAeropuertoDestino(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otra ruta con ese origen y destino");
        }

        ruta.setAeropuertoOrigen(datos.getAeropuertoOrigen());
        ruta.setAeropuertoDestino(datos.getAeropuertoDestino());
        ruta.setDistancia(datos.getDistancia());
        ruta.setDuracionAproximada(datos.getDuracionAproximada());
        return ResponseEntity.ok(rutaRepository.save(ruta));
    }

    // Eliminar: DELETE /rutas/1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!rutaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (vueloRepository.existsByRutaIdRuta(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: la ruta tiene vuelos");
        }
        rutaRepository.deleteById(id);
        return ResponseEntity.ok("Ruta eliminada");
    }

    // Reglas para crear y modificar. Devuelve el mensaje de error, o null si todo esta bien.
    // Tambien deja los codigos IATA en mayusculas.
    private String validar(Ruta r) {
        if (vacio(r.getAeropuertoOrigen()) || vacio(r.getAeropuertoDestino())) {
            return "El aeropuerto de origen y el de destino son obligatorios";
        }
        r.setAeropuertoOrigen(r.getAeropuertoOrigen().trim().toUpperCase());
        r.setAeropuertoDestino(r.getAeropuertoDestino().trim().toUpperCase());

        if (!aeropuertoRepository.existsByCodigoIata(r.getAeropuertoOrigen())) {
            return "El aeropuerto de origen no existe";
        }
        if (!aeropuertoRepository.existsByCodigoIata(r.getAeropuertoDestino())) {
            return "El aeropuerto de destino no existe";
        }
        if (r.getAeropuertoOrigen().equals(r.getAeropuertoDestino())) {
            return "Origen y destino no pueden ser iguales";
        }
        if (r.getDistancia() == null || r.getDistancia() <= 0) {
            return "La distancia (km) debe ser mayor a cero";
        }
        if (r.getDuracionAproximada() == null || r.getDuracionAproximada() <= 0) {
            return "La duracion aproximada (minutos) debe ser mayor a cero";
        }
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}