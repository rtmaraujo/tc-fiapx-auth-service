package com.fiapx.auth.security;

import com.fiapx.auth.domain.Role;
import com.fiapx.auth.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret() == null
            ? new byte[0]
            : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("fiapx.jwt.secret deve ter pelo menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(propriedadesExpiracao());
        return Jwts.builder()
            .subject(usuario.getId().toString())
            .issuer(properties.getIssuer())
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiracao))
            .claim("email", usuario.getEmail())
            .claim("nome", usuario.getNome())
            .claim("role", usuario.getRole().name())
            .signWith(key)
            .compact();
    }

    public long expiresInSeconds() {
        return propriedadesExpiracao().toSeconds();
    }

    public UsuarioPrincipal extrairPrincipal(String token) {
        Claims claims = validar(token).getPayload();
        return new UsuarioPrincipal(
            UUID.fromString(claims.getSubject()),
            claims.get("nome", String.class),
            claims.get("email", String.class),
            Role.valueOf(claims.get("role", String.class))
        );
    }

    public Jws<Claims> validar(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.getIssuer())
            .build()
            .parseSignedClaims(token);
    }

    private Duration propriedadesExpiracao() {
        return properties.getExpiration() == null ? Duration.ofHours(2) : properties.getExpiration();
    }
}
