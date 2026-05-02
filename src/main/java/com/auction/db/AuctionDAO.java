package com.auction.db;

import com.auction.factory.ItemFactory;
import com.auction.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionDAO {

    private final Connection conn;

    public AuctionDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public String save(Auction auction) throws SQLException {
        // 1. Lưu item trước
        String itemSql = """
            INSERT OR IGNORE INTO items(id, name, description, type, starting_price)
            VALUES(?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            ps.setString(1, auction.getItem().getId());
            ps.setString(2, auction.getItem().getName());
            ps.setString(3, auction.getItem().getDescription());
            ps.setString(4, auction.getItem().getType());
            ps.setDouble(5, auction.getItem().getStartingPrice());
            ps.executeUpdate();
        }

        // 2. Lưu auction
        String sql = """
            INSERT INTO auctions(id, item_id, seller_id, current_price,
                                 leading_bidder, status, start_time, end_time)
            VALUES(?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItem().getId());
            ps.setString(3, auction.getSellerId());
            ps.setDouble(4, auction.getCurrentPrice());
            ps.setString(5, auction.getLeadingBidderId());
            ps.setString(6, auction.getStatus().name());
            ps.setString(7, auction.getStartTime().toString());
            ps.setString(8, auction.getEndTime().toString());
            ps.executeUpdate();
        }
        return auction.getId();
    }

    public void updateBid(String auctionId, double newPrice,
                          String leadingBidder, String status) throws SQLException {
        String sql = """
            UPDATE auctions
            SET current_price=?, leading_bidder=?, status=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, leadingBidder);
            ps.setString(3, status);
            ps.setString(4, auctionId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(String auctionId, String status) throws SQLException {
        String sql = "UPDATE auctions SET status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, auctionId);
            ps.executeUpdate();
        }
    }

    public List<Auction> findAll() throws SQLException {
        String sql = """
            SELECT a.*, i.name, i.description, i.type, i.starting_price
            FROM auctions a JOIN items i ON a.item_id = i.id
            ORDER BY a.end_time ASC
            """;
        List<Auction> result = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(mapAuction(rs));
        }
        return result;
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        Item item = ItemFactory.create(
                rs.getString("type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("starting_price")
        );
        // Đặt lại id đúng từ DB
        item.setId(rs.getString("item_id"));

        Auction a = new Auction(
                rs.getString("id"),
                item,
                rs.getString("seller_id"),
                LocalDateTime.parse(rs.getString("start_time")),
                LocalDateTime.parse(rs.getString("end_time"))
        );
        // Phản ánh trạng thái thật từ DB
        String status = rs.getString("status");
        if (!status.equals("OPEN")) {
            // Dùng reflection-free approach: close() sẽ tự set FINISHED/CANCELED
            // nên ta map trực tiếp qua force-set bằng cách gọi internal setter
            a.forceStatus(Auction.Status.valueOf(status));
        }
        a.forceCurrentPrice(rs.getDouble("current_price"));
        a.forceLeadingBidder(rs.getString("leading_bidder"));
        return a;
    }
}