package com.sahur.empleados.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiracionMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiracion-ms}") long expiracionMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Long idEmpleado, String usuario) {
        Date ahora = new Date();
        Date vence = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(usuario)
                .claim("idEmpleado", idEmpleado)
                .issuedAt(ahora)
                .expiration(vence)
                .signWith(key)
                .compact();
    }

    public Claims leerToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}