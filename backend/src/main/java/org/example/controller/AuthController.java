package org.example.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String DB_URL = System.getenv().getOrDefault("JDBC_URL", "jdbc:mysql://localhost:3306/ai_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String DB_USER = System.getenv().getOrDefault("JDBC_USER", "ai_user");
    private static final String DB_PASS = System.getenv().getOrDefault("JDBC_PASS", "ai_pass");
    private static final SecretKey JWT_KEY = JwtUtil.getJwtKey();
    private static final long JWT_EXPIRATION = JwtUtil.JWT_EXPIRATION; // 24 hours

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT id, username, password_hash FROM user WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password_hash");
                        if (BCrypt.checkpw(password, hashedPassword)) {
                            String token = JwtUtil.generateToken(String.valueOf(rs.getLong("id")));
                            return ResponseEntity.ok(Map.of("token", token));
                        }
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error: " + ex.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        String displayName = user.get("display_name");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Check if username already exists
            String checkSql = "SELECT id FROM user WHERE username = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, username);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
                    }
                }
            }

            // Create new user
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            String insertSql = "INSERT INTO user (username, password_hash, display_name) VALUES (?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setString(1, username);
                insertPs.setString(2, hashedPassword);
                insertPs.setString(3, displayName);
                insertPs.executeUpdate();
                try (ResultSet keys = insertPs.getGeneratedKeys()) {
                    if (keys.next()) {
                        long userId = keys.getLong(1);
                        String token = JwtUtil.generateToken(String.valueOf(userId));
                        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to create user"));
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error: " + ex.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        String token = authHeader.substring(7);
        try {
            String userId = Jwts.parserBuilder()
                    .setSigningKey(JWT_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String sql = "SELECT id, username, display_name FROM user WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, Long.parseLong(userId));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> user = new HashMap<>();
                            user.put("id", rs.getLong("id"));
                            user.put("username", rs.getString("username"));
                            user.put("display_name", rs.getString("display_name"));
                            return ResponseEntity.ok(user);
                        }
                    }
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            } catch (SQLException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error: " + ex.getMessage()));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }
}