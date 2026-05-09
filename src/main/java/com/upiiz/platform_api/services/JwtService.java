package com.upiiz.platform_api.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;         // base64 preferido
    private final String issuer;
    private final long accessMinutes;
    private final long refreshDays;

    private SecretKey key;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer:upiiz-platform}") String issuer,
            @Value("${app.security.jwt.access-exp-min:${app.security.jwt.access-minutes:60}}") long accessMinutes,
            @Value("${app.security.jwt.refresh-exp-days:${app.security.jwt.refresh-days:7}}") long refreshDays
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    @PostConstruct
    void init() {
        // Intenta base64; si falla, usa bytes directos (útil en dev)
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            this.key = Keys.hmacShaKeyFor(decoded);
        } catch (IllegalArgumentException ex) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String access(Map<String,Object> claims, String subject){
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(java.util.Date.from(Instant.now()))
                .expiration(java.util.Date.from(Instant.now().plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String refresh(String subject){
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(java.util.Date.from(Instant.now()))
                .expiration(java.util.Date.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public String subject(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
