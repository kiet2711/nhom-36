package com.auction.model;

import java.time.LocalDateTime;

/**
 * Đại diện cho lệnh đặt giá tự động (Auto-Bid) của một bidder cho một phiên đấu giá.
 * <p>
 * Implement {@link Comparable} để hỗ trợ PriorityQueue — ưu tiên theo thời gian đăng ký (FIFO).
 * Là in-memory state, không persist xuống DB.
 */
public class AutoBid implements Comparable<AutoBid> {

    private final String bidderId;
    private final String auctionId;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;
    private boolean active;

    public AutoBid(String bidderId, String auctionId,
                   double maxBid, double increment) {
        this.bidderId     = bidderId;
        this.auctionId    = auctionId;
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = LocalDateTime.now();
        this.active       = true;
    }

    // --- Comparable: FIFO by registeredAt ---
    @Override
    public int compareTo(AutoBid other) {
        return this.registeredAt.compareTo(other.registeredAt);
    }

    // --- Getters ---
    public String        getBidderId()     { return bidderId; }
    public String        getAuctionId()    { return auctionId; }
    public double        getMaxBid()       { return maxBid; }
    public double        getIncrement()    { return increment; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public boolean       isActive()        { return active; }

    public void setActive(boolean active)  { this.active = active; }

    @Override
    public String toString() {
        return "AutoBid{bidder=" + bidderId
                + ", auction=" + auctionId
                + ", max=" + maxBid
                + ", inc=" + increment
                + ", active=" + active + "}";
    }
}
