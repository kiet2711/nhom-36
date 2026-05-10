package com.auction.exception;

public class AuctionClosedException extends AuctionException {
    public AuctionClosedException(String auctionId) {
        super("Phiên đấu giá [" + auctionId + "] đã kết thúc.");
    }
}