package com.auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private String auctionId;
    private String bidderId;
    private double amount;
    private LocalDateTime bidTime;

    public BidTransaction(String id, String auctionId,
                          String bidderId, double amount) {
        this.id        = id;
        this.auctionId = auctionId;
        this.bidderId  = bidderId;
        this.amount    = amount;
        this.bidTime   = LocalDateTime.now();
    }

    public String getAuctionId()    { return auctionId; }
    public String getBidderId()     { return bidderId; }
    public double getAmount()       { return amount; }
    public LocalDateTime getBidTime(){ return bidTime; }

    @Override
    public String toString() {
        return "Bid " + amount + " bởi " + bidderId + " lúc " + bidTime;
    }
}