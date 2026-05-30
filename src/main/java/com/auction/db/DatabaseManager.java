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
            seedDataIfNeeded();
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
    
    private void seedDataIfNeeded() {
        try (Statement st = connection.createStatement()) {
            // Seed SELLER
            ResultSet rsSeller = st.executeQuery("SELECT COUNT(*) FROM users WHERE username='seller1'");
            if (rsSeller.getInt(1) == 0) {
                st.execute("INSERT INTO users(id, username, password, role) VALUES('seller-001', 'seller1', 'seller123', 'SELLER')");
            }
            
            // Seed Auctions
            ResultSet rsAuctions = st.executeQuery("SELECT COUNT(*) FROM auctions");
            if (rsAuctions.getInt(1) == 0) {
                String now = java.time.LocalDateTime.now().toString();
                String end = java.time.LocalDateTime.now().plusHours(24).toString();
                
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-1', 'iPhone 15 Pro Max', 'Mới 100%', 'ELECTRONICS', 25000000);");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-2', 'Bức tranh Hoa Hướng Dương fake', 'Chép tay 1:1', 'ART', 5000000);");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-3', 'Mercedes S450', 'Xe chạy lướt 1000km', 'VEHICLE', 2000000000);");
                
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-1', 'item-1', 'seller-001', 25000000, 'OPEN', '" + now + "', '" + end + "');");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-2', 'item-2', 'seller-001', 5000000, 'OPEN', '" + now + "', '" + end + "');");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-3', 'item-3', 'seller-001', 2000000000, 'OPEN', '" + now + "', '" + end + "');");
                
                System.out.println("Đã tự động tạo 3 phiên đấu giá mẫu (Seller: seller1 / seller123).");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi seed data: " + e.getMessage());
        }
    }
}
