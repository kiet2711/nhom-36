package com.auction.db;

import com.auction.model.*;

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
            if (rs.next()) return Optional.of(mapUser(rs));
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
        return switch (role) {
            case "SELLER" -> new Seller(id, username, password);
            case "ADMIN"  -> new Admin(id, username, password);
            default       -> new Bidder(id, username, password);
        };
    }
}
