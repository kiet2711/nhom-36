package com.auction.exception;

public class UnauthorizedException extends AuctionException {
    public UnauthorizedException(String action) {
        super("Bạn không có quyền thực hiện: " + action);
    }
}