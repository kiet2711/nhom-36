package com.auction.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:auction.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initSchema();
            seedAdminIfNeeded();
            System.out.println("Database kết nối thành công.");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối database: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() { return connection; }

    private void initSchema() {
        try (InputStream is = getClass().getResourceAsStream("/schema.sql")) {
            if (is == null) throw new RuntimeException("Không tìm thấy schema.sql");
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String stmt : sql.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) connection.createStatement().execute(s);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Lỗi khởi tạo schema: " + e.getMessage(), e);
        }
    }

    private void seedAdminIfNeeded() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'")) {
            if (rs.getInt(1) == 0) {
                connection.createStatement().execute(
                        "INSERT INTO users(id, username, password, role) " +
                                "VALUES('admin-001', 'admin', 'admin123', 'ADMIN')"
                );
                System.out.println("Đã tạo tài khoản admin mặc định: admin / admin123");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi seed admin: " + e.getMessage());
        }
    }
}
