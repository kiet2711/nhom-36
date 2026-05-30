package com.auction.db;

import com.auction.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public boolean register(String username, String password, String role) {
        String sql = "INSERT INTO users(id, username, password, role) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // username trùng
        }
    }

    public Optional<User> login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapUser(rs);
                if ("LOCKED".equals(user.getStatus())) {
                    System.err.println("Tài khoản bị khóa: " + username);
                    return Optional.empty(); // Coi như không đăng nhập được
                }
                return Optional.of(user);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi login query: " + e.getMessage());
        }
        return Optional.empty();
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String id       = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role     = rs.getString("role");
        String status   = rs.getString("status");
        
        User user = switch (role) {
            case "SELLER" -> new Seller(id, username, password);
            case "ADMIN"  -> new Admin(id, username, password);
            default       -> new Bidder(id, username, password);
        };
        if (status != null) {
            user.setStatus(status);
        }
        return user;
    }

    // ================= ADMIN FUNCTIONS =================

    public JsonArray getAllUsers() {
        JsonArray arr = new JsonArray();
        String sql = "SELECT id, username, role, status FROM users";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", rs.getString("id"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("role", rs.getString("role"));
                obj.addProperty("status", rs.getString("status") != null ? rs.getString("status") : "ACTIVE");
                arr.add(obj);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getAllUsers: " + e.getMessage());
        }
        return arr;
    }

    public boolean updateStatus(String userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateStatus: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi deleteUser: " + e.getMessage());
            return false;
        }
    }
}
