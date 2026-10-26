package com.sahur.vuelos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<String> servicioNoDisponible(ResourceAccessException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("No se pudo conectar con reservas-service. Revisa que este corriendo.");
    }
}