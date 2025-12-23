package org.example.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final SecretKey JWT_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    public static final long JWT_EXPIRATION = 86400000; // 24 hours

    public static SecretKey getJwtKey() {
        return JWT_KEY;
    }

    public static String generateToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(JWT_KEY)
                .compact();
    }
}