package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction extends Entity {

    public enum Status { OPEN, RUNNING, FINISHED, CANCELED }

    private final Item      item;
    private final String    sellerId;
    private double          currentPrice;
    private String          leadingBidderId;
    private Status          status;
    private final LocalDateTime startTime;
    private LocalDateTime   endTime;
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
            throw new AuctionClosedException(this.id);
        }
        if (amount <= currentPrice) {
            throw new InvalidBidException(amount, currentPrice);
        }
        if (bidderId.equals(sellerId)) {
            throw new UnauthorizedException("Người bán không thể tự đấu giá.");
        }
    }

    public void close() {
        if (status != Status.FINISHED && status != Status.CANCELED) {
            this.status = (leadingBidderId != null) ? Status.FINISHED : Status.CANCELED;
        }
    }

    public boolean isActive() {
        return status == Status.OPEN || status == Status.RUNNING;
    }

    // --- Getters ---
    public Item          getItem()            { return item; }
    public String        getSellerId()        { return sellerId; }
    public double        getCurrentPrice()    { return currentPrice; }
    public String        getLeadingBidderId() { return leadingBidderId; }
    public Status        getStatus()          { return status; }
    public LocalDateTime getStartTime()       { return startTime; }
    public LocalDateTime getEndTime()         { return endTime; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    // --- Package-private setters dùng cho DAO phục hồi trạng thái ---
    public void forceStatus(Status s)         { this.status = s; }
    public void forceCurrentPrice(double p)   { this.currentPrice = p; }
    public void forceLeadingBidder(String id) { this.leadingBidderId = id; }
}
