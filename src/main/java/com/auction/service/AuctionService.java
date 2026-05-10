package com.auction.service;

import com.auction.db.AuctionDAO;
import com.auction.db.BidTransactionDAO;
import com.auction.exception.AuctionNotFoundException;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Item;
import com.auction.network.observer.AuctionObserver;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private static AuctionService instance;

    private final AuctionDAO         auctionDAO  = new AuctionDAO();
    private final BidTransactionDAO  bidTxDAO    = new BidTransactionDAO();
    private final AuctionManager     manager     = AuctionManager.getInstance();
    private final Map<String, ReentrantLock> locks = new HashMap<>();
    private final List<AuctionObserver> observers  = new CopyOnWriteArrayList<>();

    private AuctionService() {
        try {
            auctionDAO.findAll().forEach(a -> {
                manager.addAuction(a);
                locks.put(a.getId(), new ReentrantLock());
            });
            System.out.println("Loaded " + manager.getAllAuctions().size() + " auction(s) từ database.");
        } catch (SQLException e) {
            System.err.println("Lỗi load auction: " + e.getMessage());
        }
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) instance = new AuctionService();
        return instance;
    }

    public Auction createAuction(Item item, String sellerId,
                                 LocalDateTime endTime) throws SQLException {
        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, sellerId, LocalDateTime.now(), endTime);
        auctionDAO.save(auction);
        manager.addAuction(auction);
        locks.put(id, new ReentrantLock());
        notifyObservers(auction);
        return auction;
    }

    public BidTransaction placeBid(String auctionId, String bidderId, double amount)
            throws Exception {
        Auction auction = manager.getAuction(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        ReentrantLock lock = locks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            BidTransaction tx = auction.placeBid(bidderId, amount);
            auctionDAO.updateBid(auctionId, amount, bidderId, auction.getStatus().name());
            bidTxDAO.save(tx);
            notifyObservers(auction);
            return tx;
        } finally {
            lock.unlock();
        }
    }

    public Collection<Auction> getAllAuctions() { return manager.getAllAuctions(); }

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

    public void addObserver(AuctionObserver o)    { observers.add(o); }
    public void removeObserver(AuctionObserver o) { observers.remove(o); }

    private void notifyObservers(Auction auction) {
        observers.forEach(o -> o.onAuctionUpdated(auction));
    }
}
