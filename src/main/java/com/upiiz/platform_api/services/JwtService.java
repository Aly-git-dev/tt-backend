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

    static final long MAX_ACCESS_MINUTES = 30L * 24L * 60L;
    static final long MAX_REFRESH_DAYS = 30L;

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
        this.accessMinutes = clamp(accessMinutes, 1, MAX_ACCESS_MINUTES);
        this.refreshDays = clamp(refreshDays, 1, MAX_REFRESH_DAYS);
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
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String refresh(String subject){
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public String subject(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    private long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }
}
