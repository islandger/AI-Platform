package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.Agent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DB_URL = System.getenv().getOrDefault("JDBC_URL", "jdbc:mysql://localhost:3306/ai_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String DB_USER = System.getenv().getOrDefault("JDBC_USER", "ai_user");
    private static final String DB_PASS = System.getenv().getOrDefault("JDBC_PASS", "ai_pass");

    @GetMapping
    public ResponseEntity<?> listAgents() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ArrayList<Agent> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT id,name,description,system_prompt,model_config,status,created_at,updated_at FROM agent ORDER BY updated_at DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Agent a = new Agent();
                        a.id = rs.getLong("id");
                        a.name = rs.getString("name");
                        a.description = rs.getString("description");
                        a.system_prompt = rs.getString("system_prompt");
                        a.model_config = rs.getString("model_config");
                        a.status = rs.getString("status");
                        a.created_at = rs.getString("created_at");
                        a.updated_at = rs.getString("updated_at");
                        list.add(a);
                    }
                }
            }
            return ResponseEntity.ok(list);
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database error: " + ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createAgent(@RequestBody Agent agent) {
        if (agent.name == null || agent.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        if (agent.system_prompt == null || agent.system_prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("System prompt is required");
        }

        agent.status = agent.status == null ? "draft" : agent.status;
        agent.created_at = Instant.now().toString();
        agent.updated_at = agent.created_at;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "INSERT INTO agent (name,description,system_prompt,model_config,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, agent.name);
                ps.setString(2, agent.description);
                ps.setString(3, agent.system_prompt);
                ps.setString(4, agent.model_config);
                ps.setString(5, agent.status);
                ps.setString(6, agent.created_at);
                ps.setString(7, agent.updated_at);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        agent.id = keys.getLong(1);
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(agent);
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database error: " + ex.getMessage());
        }
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<?> testAgent(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // Implement test agent logic here
        return ResponseEntity.ok(Map.of("message", "Test agent endpoint is working"));
    }
}