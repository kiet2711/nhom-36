package com.auction.service;

import com.auction.db.AuctionDAO;
import com.auction.db.BidTransactionDAO;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Item;
import com.auction.observer.AuctionObserver;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private static AuctionService instance;

    private final AuctionDAO auctionDAO         = new AuctionDAO();
    private final BidTransactionDAO bidTxDAO    = new BidTransactionDAO();
    private final AuctionManager manager        = AuctionManager.getInstance();

    // Lock per auction để tránh lost update khi bid đồng thời
    private final Map<String, ReentrantLock> locks = new HashMap<>();

    // Danh sách observer để push realtime (Tuần 4 dùng)
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private AuctionService() {
        // Load các auction từ DB vào memory khi khởi động
        try {
            auctionDAO.findAll().forEach(manager::addAuction);
            System.out.println("Loaded " + manager.getAllAuctions().size()
                    + " auction(s) từ database.");
        } catch (SQLException e) {
            System.err.println("Lỗi load auction: " + e.getMessage());
        }
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) instance = new AuctionService();
        return instance;
    }

    /** Tạo phiên đấu giá mới */
    public Auction createAuction(Item item, String sellerId,
                                 LocalDateTime endTime) throws SQLException {
        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, sellerId,
                LocalDateTime.now(), endTime);
        auctionDAO.save(auction);
        manager.addAuction(auction);
        locks.put(id, new ReentrantLock());
        notifyObservers(auction);
        return auction;
    }

    /** Đặt giá — thread-safe per auction */
    public BidTransaction placeBid(String auctionId,
                                   String bidderId, double amount)
            throws Exception {

        Auction auction = manager.getAuction(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá."));

        // Lock riêng cho từng auction để các phiên khác không bị block
        ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            BidTransaction tx = auction.placeBid(bidderId, amount);
            // Persist ngay lập tức
            auctionDAO.updateBid(auctionId, amount, bidderId,
                    auction.getStatus().name());
            bidTxDAO.save(tx);
            notifyObservers(auction);
            return tx;
        } finally {
            lock.unlock();
        }
    }

    /** Lấy tất cả auction */
    public Collection<Auction> getAllAuctions() {
        return manager.getAllAuctions();
    }

    /** Đóng phiên khi hết giờ */
    public void closeAuction(String auctionId) throws SQLException {
        manager.getAuction(auctionId).ifPresent(auction -> {
            auction.close();
            try {
                auctionDAO.updateStatus(auctionId, auction.getStatus().name());
                notifyObservers(auction);
            } catch (SQLException e) {
                System.err.println("Lỗi close auction: " + e.getMessage());
            }
        });
    }

    // --- Observer pattern (Tuần 4 mở rộng) ---
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Auction auction) {
        observers.forEach(o -> o.onAuctionUpdated(auction));
    }
}