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
