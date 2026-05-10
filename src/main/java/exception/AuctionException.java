package com.auction.exception;

/** Base exception cho toàn bộ hệ thống đấu giá */
public class AuctionException extends RuntimeException {
    public AuctionException(String message) {
        super(message);
    }
    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}