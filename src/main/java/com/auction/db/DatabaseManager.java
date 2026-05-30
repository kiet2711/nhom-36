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
            
            // Seed BIDDER (tài khoản mẫu để test)
            ResultSet rsBidder = st.executeQuery("SELECT COUNT(*) FROM users WHERE username='bidder1'");
            if (rsBidder.getInt(1) == 0) {
                st.execute("INSERT INTO users(id, username, password, role) VALUES('bidder-001', 'bidder1', 'bidder123', 'BIDDER')");
                st.execute("INSERT INTO users(id, username, password, role) VALUES('bidder-002', 'bidder2', 'bidder123', 'BIDDER')");
                System.out.println("Đã tạo tài khoản bidder mẫu: bidder1/bidder123, bidder2/bidder123");
            }
            
            // Seed Auctions
            ResultSet rsAuctions = st.executeQuery("SELECT COUNT(*) FROM auctions");
            if (rsAuctions.getInt(1) == 0) {
                String now = java.time.LocalDateTime.now().toString();
                String end1h = java.time.LocalDateTime.now().plusHours(1).toString();
                String end6h = java.time.LocalDateTime.now().plusHours(6).toString();
                String end24h = java.time.LocalDateTime.now().plusHours(24).toString();
                String end48h = java.time.LocalDateTime.now().plusHours(48).toString();
                
                // --- Sản phẩm ELECTRONICS ---
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-1', 'iPhone 15 Pro Max', 'Mới 100%, màu Titan Tự nhiên, 256GB', 'ELECTRONICS', 25000000)");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-2', 'MacBook Pro M3 14 inch', 'Chip M3, RAM 18GB, SSD 512GB', 'ELECTRONICS', 35000000)");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-3', 'Sony WH-1000XM5', 'Tai nghe chống ồn cao cấp, màu đen', 'ELECTRONICS', 5000000)");
                
                // --- Sản phẩm ART ---
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-4', 'Tranh Hoa Hướng Dương', 'Chép tay 1:1 theo Van Gogh, sơn dầu trên canvas', 'ART', 8000000)");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-5', 'Tượng Phật Bà Quan Âm', 'Gỗ trầm hương nguyên khối, cao 40cm', 'ART', 15000000)");
                
                // --- Sản phẩm VEHICLE ---
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-6', 'Mercedes S450 2023', 'Xe chạy lướt 1000km, full option', 'VEHICLE', 2000000000)");
                st.execute("INSERT INTO items(id, name, description, type, starting_price) VALUES('item-7', 'Honda SH 150i', 'Đời 2024, màu đen nhám, ODO 500km', 'VEHICLE', 80000000)");
                
                // --- Tạo phiên đấu giá ---
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-1', 'item-1', 'seller-001', 25000000, 'OPEN', '" + now + "', '" + end1h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-2', 'item-2', 'seller-001', 35000000, 'OPEN', '" + now + "', '" + end6h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-3', 'item-3', 'seller-001', 5000000, 'OPEN', '" + now + "', '" + end24h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-4', 'item-4', 'seller-001', 8000000, 'OPEN', '" + now + "', '" + end24h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-5', 'item-5', 'seller-001', 15000000, 'OPEN', '" + now + "', '" + end48h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-6', 'item-6', 'seller-001', 2000000000, 'OPEN', '" + now + "', '" + end48h + "')");
                st.execute("INSERT INTO auctions(id, item_id, seller_id, current_price, status, start_time, end_time) VALUES('auc-7', 'item-7', 'seller-001', 80000000, 'OPEN', '" + now + "', '" + end6h + "')");
                
                System.out.println("Đã tự động tạo 7 phiên đấu giá mẫu (Seller: seller1 / seller123).");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi seed data: " + e.getMessage());
        }
    }
}
