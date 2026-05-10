package com.auction.exception;

public class AuctionNotFoundException extends AuctionException {
    public AuctionNotFoundException(String auctionId) {
        super("Không tìm thấy phiên đấu giá: " + auctionId);
    }
}