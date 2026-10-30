package com.sahur.empleados.controller;

import com.sahur.empleados.client.ReservasClient;
import com.sahur.empleados.dto.LoginRequest;
import com.sahur.empleados.model.Empleado;
import com.sahur.empleados.repository.EmpleadoRepository;
import com.sahur.empleados.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/empleados")
public class EmpleadoController {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVO", "INACTIVO");

    private final EmpleadoRepository repository;
    private final JwtUtil jwtUtil;
    private final ReservasClient reservas;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public EmpleadoController(EmpleadoRepository repository, JwtUtil jwtUtil, ReservasClient reservas) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.reservas = reservas;
    }

    @GetMapping
    public List<Empleado> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empleado> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Empleado empleado) {
        if (vacio(empleado.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El password es obligatorio");
        }
        if (vacio(empleado.getEstado())) {
            empleado.setEstado("ACTIVO");
        }
        String error = validar(empleado);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByUsuario(empleado.getUsuario())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un empleado con ese usuario");
        }
        empleado.setIdEmpleado(null);
        empleado.setEstado(empleado.getEstado().toUpperCase());
        empleado.setPassword(encoder.encode(empleado.getPassword()));
        return ResponseEntity.ok(repository.save(empleado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id, @RequestBody Empleado datos) {
        Optional<Empleado> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String error = validar(datos);
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (repository.existsByUsuarioAndIdEmpleadoNot(datos.getUsuario(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe otro empleado con ese usuario");
        }
        Empleado empleado = encontrado.get();
        empleado.setUsuario(datos.getUsuario());
        empleado.setNombres(datos.getNombres());
        empleado.setApellidos(datos.getApellidos());
        empleado.setPuesto(datos.getPuesto());
        empleado.setEstado(datos.getEstado().toUpperCase());
        if (!vacio(datos.getPassword())) {
            empleado.setPassword(encoder.encode(datos.getPassword()));
        }
        return ResponseEntity.ok(repository.save(empleado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (reservas.tieneReservas(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: el empleado tiene ventas registradas. Cambia su estado a INACTIVO");
        }
        repository.deleteById(id);
        return ResponseEntity.ok("Empleado eliminado");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Empleado> encontrado = repository.findByUsuario(request.getUsuario());

        if (encontrado.isEmpty() || !encoder.matches(request.getPassword(), encontrado.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o password incorrectos");
        }

        Empleado empleado = encontrado.get();
        if ("INACTIVO".equals(empleado.getEstado())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("El empleado esta INACTIVO");
        }

        String token = jwtUtil.generarToken(empleado.getIdEmpleado(), empleado.getUsuario());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "idEmpleado", empleado.getIdEmpleado(),
                "usuario", empleado.getUsuario()
        ));
    }

    private String validar(Empleado e) {
        if (vacio(e.getUsuario())) {
            return "El usuario es obligatorio";
        }
        if (vacio(e.getNombres())) {
            return "Los nombres son obligatorios";
        }
        if (vacio(e.getApellidos())) {
            return "Los apellidos son obligatorios";
        }
        if (vacio(e.getPuesto())) {
            return "El puesto es obligatorio";
        }
        if (vacio(e.getEstado()) || !ESTADOS_VALIDOS.contains(e.getEstado().toUpperCase())) {
            return "Estado invalido. Usa: ACTIVO o INACTIVO";
        }
        return null;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}