package com.techstore.security;

import com.techstore.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long preAuthExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.jwt.preauth-expiration-ms}") long preAuthExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.preAuthExpirationMs = preAuthExpirationMs;
    }

    public String generateToken(User u) {
        return build(u, "FULL", expirationMs);
    }

    public String generatePreAuthToken(User u) {
        return build(u, "PRE_AUTH", preAuthExpirationMs);
    }

    private String build(User u, String type, long ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("role", u.getRole().name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}