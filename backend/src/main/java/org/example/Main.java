package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mindrot.jbcrypt.BCrypt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;

public class Main {
    static final int PORT = 8080;
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // In-memory cache fallback
    static final Map<Long, Agent> AGENTS = new HashMap<>();
    static long ID_GEN = 1;

    // DB config (can be overridden with env vars)
    static final String DB_URL = System.getenv().getOrDefault("JDBC_URL", "jdbc:mysql://localhost:3306/ai_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    static final String DB_USER = System.getenv().getOrDefault("JDBC_USER", "ai_user");
    static final String DB_PASS = System.getenv().getOrDefault("JDBC_PASS", "ai_pass");
    // JWT key (use env JWT_SECRET or generate)
    static final SecretKey JWT_KEY;
    static {
        String s = System.getenv("JWT_SECRET");
        if (s != null && !s.isEmpty()) {
            JWT_KEY = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
            System.out.println("Using JWT secret from env");
        } else {
            JWT_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            System.out.println("Generated ephemeral JWT key");
        }
    }

    public static void main(String[] args) throws Exception {
        // Try to load DB driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL driver loaded. JDBC URL=" + DB_URL);
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found on classpath: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/agents", new AgentsHandler());
        server.createContext("/api/agents/", new AgentActionHandler());
        server.createContext("/", new StaticHandler());
        server.createContext("/api/knowledge-bases", new KBHandler());
        server.createContext("/api/knowledge-bases/", new KBHandler());
        server.createContext("/api/workflows", new WorkflowHandler());
        server.createContext("/api/plugins", new PluginHandler());
        server.createContext("/api/auth", new AuthHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Backend server started at http://localhost:" + PORT);
    }

    static class Agent {
        public long id;
        public String name;
        public String description;
        public String system_prompt;
        public String model_config;
        public String status = "draft";
        public String created_at = Instant.now().toString();
        public String updated_at = Instant.now().toString();
    }

    static class AgentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleList(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
            } else {
                sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            // Try reading from DB
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
                sendJson(exchange, 200, list);
                return;
            } catch (SQLException ex) {
                // Fallback to in-memory
                sendJson(exchange, 200, AGENTS.values());
            }
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            long uid = getUserIdFromAuth(exchange);
            if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
            String body = readAll(exchange.getRequestBody());
            Agent a;
            try {
                a = GSON.fromJson(body, Agent.class);
            } catch (Exception e) {
                sendJson(exchange, 400, Map.of("error", "invalid json"));
                return;
            }
            if (a == null || a.name == null || a.name.trim().isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "name is required"));
                return;
            }
            a.status = a.status == null ? "draft" : a.status;
            a.created_at = Instant.now().toString();
            a.updated_at = a.created_at;
            // Insert into DB
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String sql = "INSERT INTO agent (name,description,system_prompt,model_config,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, a.name);
                    ps.setString(2, a.description);
                    ps.setString(3, a.system_prompt);
                    ps.setString(4, a.model_config);
                    ps.setString(5, a.status);
                    ps.setString(6, a.created_at);
                    ps.setString(7, a.updated_at);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) a.id = keys.getLong(1);
                    }
                }
                sendJson(exchange, 201, a);
                return;
            } catch (SQLException ex) {
                // fallback to in-memory
                long id = ID_GEN++;
                a.id = id;
                AGENTS.put(id, a);
                sendJson(exchange, 201, a);
            }
        }
    }

    static class AgentActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            // Expecting /api/agents/{id}/test
            if (path.matches("/api/agents/\\d+/test")) {
                if ("POST".equalsIgnoreCase(method)) {
                    handleTest(exchange);
                } else {
                    sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                }
                return;
            }
            sendJson(exchange, 404, Map.of("error", "Not Found"));
        }

        private void handleTest(HttpExchange exchange) throws IOException {
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                sendJson(exchange, 400, Map.of("error", "DEEPSEEK_API_KEY environment variable not set"));
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            long id = Long.parseLong(parts[3]);
            Agent a = null;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT id,name,system_prompt,model_config,status FROM agent WHERE id=?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            a = new Agent();
                            a.id = rs.getLong("id");
                            a.name = rs.getString("name");
                            a.system_prompt = rs.getString("system_prompt");
                            a.model_config = rs.getString("model_config");
                            a.status = rs.getString("status");
                        }
                    }
                }
            } catch (SQLException ex) {
                // ignore and fallback
            }
            if (a == null) a = AGENTS.get(id);
            if (a == null) {
                sendJson(exchange, 404, Map.of("error", "agent not found"));
                return;
            }
            String body = readAll(exchange.getRequestBody());
            Map input;
            try {
                input = GSON.fromJson(body, Map.class);
            } catch (Exception e) {
                sendJson(exchange, 400, Map.of("error", "invalid json"));
                return;
            }
            String userInput = input != null && input.get("input") != null ? input.get("input").toString() : "";
            if (a.system_prompt == null || a.system_prompt.trim().isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "system_prompt not configured"));
                return;
            }
            // Mocked response: combine system prompt and user input
            String reply = "[模拟回复]\n系统提示: " + a.system_prompt + "\n用户输入: " + userInput + "\nAI: 这是一个模拟回答，用于本地测试。";
            sendJson(exchange, 200, Map.of("reply", reply));
        }
    }

    // Knowledge base endpoints and others
    static class KBHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/knowledge-bases") && "GET".equalsIgnoreCase(method)) {
                handleListKB(exchange);
                return;
            }
            if (path.equals("/api/knowledge-bases") && "POST".equalsIgnoreCase(method)) {
                handleCreateKB(exchange);
                return;
            }
            if (path.matches("/api/knowledge-bases/\\d+/documents") && "POST".equalsIgnoreCase(method)) {
                handleUploadDocument(exchange);
                return;
            }
            if (path.matches("/api/knowledge-bases/\\d+/embed") && "POST".equalsIgnoreCase(method)) {
                handleEmbedKB(exchange);
                return;
            }
            if (path.matches("/api/knowledge-bases/\\d+/query") && "POST".equalsIgnoreCase(method)) {
                handleQueryKB(exchange);
                return;
            }
            sendJson(exchange, 404, Map.of("error", "Not Found"));
        }

        private void handleListKB(HttpExchange exchange) throws IOException {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                ArrayList<Map<String,Object>> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT id,name,description,chunk_size,chunk_overlap,created_at,updated_at FROM knowledge_base ORDER BY updated_at DESC")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String,Object> m = new HashMap<>();
                            m.put("id", rs.getLong("id"));
                            m.put("name", rs.getString("name"));
                            m.put("description", rs.getString("description"));
                            m.put("chunk_size", rs.getInt("chunk_size"));
                            m.put("chunk_overlap", rs.getInt("chunk_overlap"));
                            list.add(m);
                        }
                    }
                }
                sendJson(exchange, 200, list);
                return;
            } catch (SQLException e) {
                sendJson(exchange, 500, Map.of("error", "db error"));
            }
        }

        private void handleCreateKB(HttpExchange exchange) throws IOException {
            long uid = getUserIdFromAuth(exchange);
            if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
            String body = readAll(exchange.getRequestBody());
            Map input;
            try { input = GSON.fromJson(body, Map.class); } catch (Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
            String name = input.getOrDefault("name","").toString();
            if (name.isEmpty()) { sendJson(exchange,400,Map.of("error","name required")); return; }
            String description = input.getOrDefault("description","").toString();
            int chunk_size = ((Number)input.getOrDefault("chunk_size",512)).intValue();
            int chunk_overlap = ((Number)input.getOrDefault("chunk_overlap",50)).intValue();
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String sql = "INSERT INTO knowledge_base (name,description,chunk_size,chunk_overlap,created_at,updated_at) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                    String now = Instant.now().toString();
                    ps.setString(1,name); ps.setString(2,description); ps.setInt(3,chunk_size); ps.setInt(4,chunk_overlap); ps.setString(5,now); ps.setString(6,now);
                    ps.executeUpdate();
                    long id=0; try(ResultSet k=ps.getGeneratedKeys()){ if(k.next()) id=k.getLong(1); }
                    sendJson(exchange,201,Map.of("id",id,"name",name)); return;
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
        }

        private void handleUploadDocument(HttpExchange exchange) throws IOException {
            long uid = getUserIdFromAuth(exchange);
            if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
            // Expect JSON: { "filename": "...", "content": "..." }
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            long kbId = Long.parseLong(parts[3]);
            String body = readAll(exchange.getRequestBody());
            Map input;
            try { input = GSON.fromJson(body, Map.class); } catch (Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
            String filename = input.getOrDefault("filename","uploaded.txt").toString();
            String content = input.getOrDefault("content","").toString();
            if (content.isEmpty()) { sendJson(exchange,400,Map.of("error","content empty")); return; }
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                String sql = "INSERT INTO document (knowledge_base_id,filename,content,chunks,vector_ids,status,uploaded_at) VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                    ps.setLong(1,kbId); ps.setString(2,filename); ps.setString(3,content); ps.setString(4,null); ps.setString(5,null); ps.setString(6,"processing"); ps.setString(7,Instant.now().toString());
                    ps.executeUpdate(); long id=0; try(ResultSet k=ps.getGeneratedKeys()){ if(k.next()) id=k.getLong(1); }
                    // In a real system we would enqueue async processing for chunking/embedding
                    sendJson(exchange,201,Map.of("id",id,"status","processing")); return;
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
        }

        private void handleEmbedKB(HttpExchange exchange) throws IOException {
            // require auth
            long uid = getUserIdFromAuth(exchange);
            if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
            String path = exchange.getRequestURI().getPath(); String[] parts = path.split("/"); long kbId = Long.parseLong(parts[3]);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                try (PreparedStatement ps = conn.prepareStatement("SELECT id,content FROM document WHERE knowledge_base_id=?")){
                    ps.setLong(1,kbId);
                    try (ResultSet rs = ps.executeQuery()){
                        ArrayList<Map<String,Object>> updated = new ArrayList<>();
                        while (rs.next()){
                            long docId = rs.getLong("id"); String content = rs.getString("content");
                            double[] emb = vectorize(content);
                            String embJson = embeddingToJson(emb);
                            try (PreparedStatement ups = conn.prepareStatement("UPDATE document SET vector_ids=?, status=? WHERE id=?")){
                                ups.setString(1, embJson); ups.setString(2, "ready"); ups.setLong(3, docId); ups.executeUpdate();
                            }
                            updated.add(Map.of("id",docId));
                        }
                        sendJson(exchange,200,Map.of("updated",updated.size())); return;
                    }
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
        }

        private void handleQueryKB(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); String[] parts = path.split("/"); long kbId = Long.parseLong(parts[3]);
            String body = readAll(exchange.getRequestBody()); Map input; try{ input = GSON.fromJson(body, Map.class);}catch(Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
            String query = input.getOrDefault("query","" ).toString(); int topK = ((Number)input.getOrDefault("top_k",5)).intValue();
            if (query.isEmpty()){ sendJson(exchange,400,Map.of("error","query required")); return; }
            double[] qv = vectorize(query);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                try (PreparedStatement ps = conn.prepareStatement("SELECT id,filename,content,vector_ids FROM document WHERE knowledge_base_id=? AND vector_ids IS NOT NULL")){
                    ps.setLong(1,kbId);
                    try (ResultSet rs = ps.executeQuery()){
                        ArrayList<Map<String,Object>> hits = new ArrayList<>();
                        while (rs.next()){
                            long docId = rs.getLong("id"); String filename = rs.getString("filename"); String content = rs.getString("content"); String vecJson = rs.getString("vector_ids");
                            double[] emb = parseEmbedding(vecJson);
                            double score = cosine(qv, emb);
                            hits.add(Map.of("id",docId,"filename",filename,"score",score,"snippet", content!=null? (content.length()>200?content.substring(0,200):content):""));
                        }
                        List<Map<String,Object>> top = hits.stream().sorted(Comparator.comparingDouble(m -> -((Number)m.get("score")).doubleValue())).limit(topK).collect(Collectors.toList());
                        sendJson(exchange,200,Map.of("results",top)); return;
                    }
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
        }
    }

    static class WorkflowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method) && exchange.getRequestURI().getPath().equals("/api/workflows")){
                long uid = getUserIdFromAuth(exchange);
                if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
                String body = readAll(exchange.getRequestBody());
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                    String sql = "INSERT INTO workflow (name,description,nodes,edges,config,created_at,updated_at) VALUES (?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                        Map input = GSON.fromJson(body, Map.class);
                        String name = input.getOrDefault("name","").toString();
                        String desc = input.getOrDefault("description","").toString();
                        String nodes = GSON.toJson(input.getOrDefault("nodes", new ArrayList<>()));
                        String edges = GSON.toJson(input.getOrDefault("edges", new ArrayList<>()));
                        String config = GSON.toJson(input.getOrDefault("config", new HashMap<>()));
                        String now = Instant.now().toString();
                        ps.setString(1,name); ps.setString(2,desc); ps.setString(3,nodes); ps.setString(4,edges); ps.setString(5,config); ps.setString(6,now); ps.setString(7,now);
                        ps.executeUpdate(); long id=0; try(ResultSet k=ps.getGeneratedKeys()){ if(k.next()) id=k.getLong(1); }
                        sendJson(exchange,201,Map.of("id",id)); return;
                    }
                } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
            }
            sendJson(exchange,404,Map.of("error","Not Found"));
        }
    }

    static class PluginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/plugins") && "GET".equalsIgnoreCase(method)){
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                    ArrayList<Map<String,Object>> list = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement("SELECT id,name,description,type,status,created_at FROM plugin ORDER BY created_at DESC")){
                        try (ResultSet rs = ps.executeQuery()){
                            while (rs.next()){
                                Map<String,Object> m = new HashMap<>();
                                m.put("id", rs.getLong("id")); m.put("name", rs.getString("name")); m.put("description", rs.getString("description")); m.put("type", rs.getString("type")); m.put("status", rs.getString("status"));
                                list.add(m);
                            }
                        }
                    }
                    sendJson(exchange,200,list); return;
                } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
            }
            if (path.equals("/api/plugins") && "POST".equalsIgnoreCase(method)){
                long uid = getUserIdFromAuth(exchange);
                if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
                String body = readAll(exchange.getRequestBody()); Map input; try{ input = GSON.fromJson(body, Map.class);}catch(Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
                String name = input.getOrDefault("name","" ).toString(); if(name.isEmpty()){ sendJson(exchange,400,Map.of("error","name required")); return; }
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                    String sql = "INSERT INTO plugin (name,description,type,openapi_spec,config,status,created_at) VALUES (?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                        String now = Instant.now().toString();
                        ps.setString(1, name); ps.setString(2, input.getOrDefault("description"," ").toString()); ps.setString(3, input.getOrDefault("type","custom").toString()); ps.setString(4, GSON.toJson(input.getOrDefault("openapi_spec", new HashMap<>()))); ps.setString(5, GSON.toJson(input.getOrDefault("config", new HashMap<>()))); ps.setString(6, input.getOrDefault("status","disabled").toString()); ps.setString(7, now);
                        ps.executeUpdate(); long id=0; try(ResultSet k=ps.getGeneratedKeys()){ if(k.next()) id=k.getLong(1); }
                        sendJson(exchange,201,Map.of("id",id)); return;
                    }
                } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
            }
            sendJson(exchange,404,Map.of("error","Not Found"));
        }
    }

    static final Map<String, Long> TOKENS = new ConcurrentHashMap<>(); // token -> userId

    static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/auth/register") && "POST".equalsIgnoreCase(method)){
                handleRegister(exchange); return;
            }
            if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)){
                handleLogin(exchange); return;
            }
            if (path.equals("/api/auth/me") && "GET".equalsIgnoreCase(method)){
                handleMe(exchange); return;
            }
            sendJson(exchange,404,Map.of("error","Not Found"));
        }

        private void handleMe(HttpExchange exchange) throws IOException {
            long uid = getUserIdFromAuth(exchange);
            if (uid <= 0) { sendJson(exchange,401,Map.of("error","unauthorized")); return; }
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                String sql = "SELECT id,username,display_name FROM user WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)){
                    ps.setLong(1, uid);
                    try (ResultSet rs = ps.executeQuery()){
                        if (rs.next()){
                            Map<String,Object> m = new HashMap<>();
                            m.put("id", rs.getLong("id"));
                            m.put("username", rs.getString("username"));
                            m.put("display_name", rs.getString("display_name"));
                            sendJson(exchange,200,m); return;
                        }
                    }
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","db error")); }
            sendJson(exchange,404,Map.of("error","user not found"));
        }

        private void handleRegister(HttpExchange exchange) throws IOException {
            String body = readAll(exchange.getRequestBody());
            Map input; try{ input = GSON.fromJson(body, Map.class);}catch(Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
            String username = input.getOrDefault("username","").toString();
            String password = input.getOrDefault("password","").toString();
            String display = input.getOrDefault("display_name","").toString();
            
            // 用户名验证
            if (username.isEmpty()){ sendJson(exchange,400,Map.of("error","用户名不能为空")); return; }
            if (username.length() < 3 || username.length() > 20){ sendJson(exchange,400,Map.of("error","用户名长度必须在3-20个字符之间")); return; }
            if (!username.matches("^[a-zA-Z0-9_]+$")){ sendJson(exchange,400,Map.of("error","用户名只能包含字母、数字和下划线")); return; }
            
            // 密码验证
            if (password.isEmpty()){ sendJson(exchange,400,Map.of("error","密码不能为空")); return; }
            if (password.length() < 6 || password.length() > 20){ sendJson(exchange,400,Map.of("error","密码长度必须在6-20个字符之间")); return; }
            // 简化密码验证，避免正则表达式转义问题
            boolean hasLetter = false, hasDigit = false;
            for (char c : password.toCharArray()) {
                if (Character.isLetter(c)) hasLetter = true;
                if (Character.isDigit(c)) hasDigit = true;
            }
            if (!hasLetter || !hasDigit) {
                sendJson(exchange,400,Map.of("error","密码必须包含至少一个字母和一个数字")); return;
            }
            
            // 显示名验证
            if (display.length() > 50){ sendJson(exchange,400,Map.of("error","显示名长度不能超过50个字符")); return; }
            
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                String sql = "INSERT INTO user (username,password_hash,display_name) VALUES (?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                    ps.setString(1, username); ps.setString(2, hash); ps.setString(3, display);
                    ps.executeUpdate(); long id=0; try(ResultSet k=ps.getGeneratedKeys()){ if(k.next()) id=k.getLong(1); }
                    // return jwt
                    String token = createJwtForUser(id);
                    sendJson(exchange,201,Map.of("id",id,"username",username,"token",token)); return;
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","用户名已存在")); }
        }

        private void handleLogin(HttpExchange exchange) throws IOException {
            String body = readAll(exchange.getRequestBody()); Map input; try{ input = GSON.fromJson(body, Map.class);}catch(Exception e){ sendJson(exchange,400,Map.of("error","invalid json")); return; }
            String username = input.getOrDefault("username","").toString();
            String password = input.getOrDefault("password","").toString();
            if (username.isEmpty() || password.isEmpty()){ sendJson(exchange,400,Map.of("error","用户名和密码不能为空")); return; }
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)){
                String sql = "SELECT id,password_hash FROM user WHERE username=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)){
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()){
                        if (rs.next()){
                            long id = rs.getLong("id");
                            String stored = rs.getString("password_hash");
                            if (stored != null && BCrypt.checkpw(password, stored)){
                                String token = createJwtForUser(id);
                                sendJson(exchange,200,Map.of("token",token,"user_id",id)); return;
                            } else {
                                sendJson(exchange,401,Map.of("error","密码错误")); return;
                            }
                        } else {
                            sendJson(exchange,401,Map.of("error","用户名不存在")); return;
                        }
                    }
                }
            } catch (SQLException ex){ sendJson(exchange,500,Map.of("error","数据库错误")); }
        }
    }

    static String sha256(String s){
        try{ MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8)); StringBuilder sb = new StringBuilder(); for(byte x: b) sb.append(String.format("%02x", x)); return sb.toString(); }catch(NoSuchAlgorithmException e){ return s; }
    }

    // JWT helpers
    static String createJwtForUser(long userId){
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
                .signWith(JWT_KEY)
                .compact();
    }

    static long parseJwtUserId(String token){
        try{
            var claims = Jwts.parserBuilder().setSigningKey(JWT_KEY).build().parseClaimsJws(token).getBody();
            return Long.parseLong(claims.getSubject());
        }catch(Exception e){
            return -1;
        }
    }

    static long getUserIdFromAuth(HttpExchange exchange){
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null) return -1;
        if (auth.toLowerCase().startsWith("bearer ")){
            String token = auth.substring(7).trim();
            return parseJwtUserId(token);
        }
        return -1;
    }

    // Simple vectorizer: n-dim hashing of tokens -> double[]
    static final int EMB_DIM = 128;
    static double[] vectorize(String text){
        double[] v = new double[EMB_DIM];
        if (text == null) return v;
        String[] toks = text.toLowerCase().split("\\\\W+");
        for(String t: toks){
            if (t.isEmpty()) continue;
            int h = Math.abs(t.hashCode());
            int idx = h % EMB_DIM;
            v[idx] += 1.0;
        }
        // normalize
        double sum = 0; for(double x: v) sum += x*x; sum = Math.sqrt(sum); if (sum > 0){ for(int i=0;i<v.length;i++) v[i] /= sum; }
        return v;
    }

    static double cosine(double[] a, double[] b){
        if (a==null || b==null || a.length!=b.length) return -1.0;
        double s=0; for(int i=0;i<a.length;i++) s += a[i]*b[i];
        return s;
    }

    static double[] parseEmbedding(String json){
        try{
            return GSON.fromJson(json, double[].class);
        }catch(Exception e){ return null; }
    }

    static String embeddingToJson(double[] v){ return GSON.toJson(v); }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            InputStream is = Main.class.getResourceAsStream("/static" + path);
            if (is == null) {
                sendJson(exchange, 404, Map.of("error", "static file not found: " + path));
                return;
            }
            byte[] data = readAllBytes(is);
            String contentType = guessContentType(path);
            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    // Utilities
    static void sendJson(HttpExchange exchange, int code, Object obj) throws IOException {
        String resp = GSON.toJson(obj);
        byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
        Headers h = exchange.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String readAll(InputStream is) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = is.read(buffer)) != -1) baos.write(buffer, 0, n);
        return baos.toByteArray();
    }

    static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        return "text/plain";
    }
}