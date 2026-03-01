package com.waitlist.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHmacSha256Algorithm}")
    private String jwtSecret;

    @Value("${app.jwt.expiration.days:0}")
    private int jwtExpirationDays;

    @Value("${app.jwt.expiration.hours:0}")
    private int jwtExpirationHours;

    @Value("${app.jwt.expiration.minutes:5}")
    private int jwtExpirationMinutes;

    private long calculateExpirationMs() {
        return (long) jwtExpirationDays * 24 * 60 * 60 * 1000 +
                (long) jwtExpirationHours * 60 * 60 * 1000 +
               (long) jwtExpirationMinutes * 60 * 1000;
    }

    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + calculateExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
