package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction extends Entity {

    public enum Status { OPEN, RUNNING, FINISHED, CANCELED }

    private final Item item;
    private final String sellerId;
    private double currentPrice;
    private String leadingBidderId;   // null nếu chưa ai bid
    private Status status;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<BidTransaction> bidHistory;

    public Auction(String id, Item item, String sellerId,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.id              = id;
        this.item            = item;
        this.sellerId        = sellerId;
        this.currentPrice    = item.getStartingPrice();
        this.leadingBidderId = null;
        this.status          = Status.OPEN;
        this.startTime       = startTime;
        this.endTime         = endTime;
        this.bidHistory      = new ArrayList<>();
    }

    /**
     * Đặt giá. Trả về BidTransaction nếu hợp lệ, ném exception nếu không.
     * Phương thức này KHÔNG synchronized — AuctionService sẽ lock bên ngoài.
     */
    public BidTransaction placeBid(String bidderId, double amount) {
        validateBid(bidderId, amount);

        String txId = java.util.UUID.randomUUID().toString();
        BidTransaction tx = new BidTransaction(txId, this.id, bidderId, amount);

        this.currentPrice    = amount;
        this.leadingBidderId = bidderId;
        this.status          = Status.RUNNING;
        this.bidHistory.add(tx);
        return tx;
    }

    private void validateBid(String bidderId, double amount) {
        if (status == Status.FINISHED || status == Status.CANCELED) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc.");
        }
        if (amount <= currentPrice) {
            throw new IllegalArgumentException(
                    "Giá đặt phải cao hơn giá hiện tại (" + currentPrice + ").");
        }
        if (bidderId.equals(sellerId)) {
            throw new IllegalArgumentException("Người bán không thể tự đấu giá.");
        }
    }

    /** Đóng phiên khi hết giờ */
    public void close() {
        if (status != Status.FINISHED && status != Status.CANCELED) {
            this.status = (leadingBidderId != null) ? Status.FINISHED : Status.CANCELED;
        }
    }

    // --- Getters ---
    public Item           getItem()             { return item; }
    public String         getSellerId()         { return sellerId; }
    public double         getCurrentPrice()     { return currentPrice; }
    public String         getLeadingBidderId()  { return leadingBidderId; }
    public Status         getStatus()           { return status; }
    public LocalDateTime  getStartTime()        { return startTime; }
    public LocalDateTime  getEndTime()          { return endTime; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public boolean isActive() {
        return status == Status.OPEN || status == Status.RUNNING;
    }
    public void forceStatus(Status s)         { this.status = s; }
    public void forceCurrentPrice(double p)   { this.currentPrice = p; }
    public void forceLeadingBidder(String id) { this.leadingBidderId = id; }
}