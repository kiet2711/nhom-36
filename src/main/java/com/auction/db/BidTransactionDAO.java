package com.auction.db;

import com.auction.model.BidTransaction;

import java.sql.*;

public class BidTransactionDAO {

    private final Connection conn;

    public BidTransactionDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public void save(BidTransaction tx) throws SQLException {
        String sql = """
            INSERT INTO bid_transactions(id, auction_id, bidder_id, amount, bid_time)
            VALUES(?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getId());
            ps.setString(2, tx.getAuctionId());
            ps.setString(3, tx.getBidderId());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getBidTime().toString());
            ps.executeUpdate();
        }
    }
}