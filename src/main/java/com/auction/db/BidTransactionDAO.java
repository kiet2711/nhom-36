package com.auction.db;

import com.auction.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    private final Connection conn;

    public BidTransactionDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public void save(BidTransaction tx) throws SQLException {
        String sql = "INSERT INTO bid_transactions(id, auction_id, bidder_id, amount, bid_time) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getId());
            ps.setString(2, tx.getAuctionId());
            ps.setString(3, tx.getBidderId());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getBidTime().toString());
            ps.executeUpdate();
        }
    }

    /** Lấy toàn bộ lịch sử bid của 1 phiên, sắp xếp theo thời gian */
    public List<BidRecord> findByAuction(String auctionId) throws SQLException {
        String sql = "SELECT bidder_id, amount, bid_time FROM bid_transactions " +
                     "WHERE auction_id = ? ORDER BY bid_time ASC";
        List<BidRecord> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new BidRecord(
                            rs.getString("bidder_id"),
                            rs.getDouble("amount"),
                            rs.getString("bid_time")
                    ));
                }
            }
        }
        return result;
    }

    // ================= ADMIN FUNCTIONS =================

    public com.google.gson.JsonArray getAllTransactions() {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        String sql = "SELECT bt.id, bt.auction_id, a.item_id, i.name as item_name, bt.bidder_id, u.username, bt.amount, bt.bid_time " +
                     "FROM bid_transactions bt " +
                     "JOIN auctions a ON bt.auction_id = a.id " +
                     "JOIN items i ON a.item_id = i.id " +
                     "JOIN users u ON bt.bidder_id = u.id " +
                     "ORDER BY bt.bid_time DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", rs.getString("id"));
                obj.addProperty("auctionId", rs.getString("auction_id"));
                obj.addProperty("itemName", rs.getString("item_name"));
                obj.addProperty("bidderId", rs.getString("bidder_id"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("amount", rs.getDouble("amount"));
                obj.addProperty("bidTime", rs.getString("bid_time"));
                arr.add(obj);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getAllTransactions: " + e.getMessage());
        }
        return arr;
    }

    /** DTO đơn giản cho kết quả query bid history */
    public static class BidRecord {
        public final String bidderId;
        public final double amount;
        public final String bidTime;

        public BidRecord(String bidderId, double amount, String bidTime) {
            this.bidderId = bidderId;
            this.amount   = amount;
            this.bidTime  = bidTime;
        }
    }
}
