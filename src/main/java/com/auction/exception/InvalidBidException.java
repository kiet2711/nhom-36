package com.auction.exception;

public class InvalidBidException extends AuctionException {
    private final double attemptedAmount;
    private final double currentPrice;

    public InvalidBidException(double attemptedAmount, double currentPrice) {
        super(String.format(
                "Giá đặt %.0f phải cao hơn giá hiện tại %.0f.",
                attemptedAmount, currentPrice));
        this.attemptedAmount = attemptedAmount;
        this.currentPrice    = currentPrice;
    }

    public double getAttemptedAmount() { return attemptedAmount; }
    public double getCurrentPrice()    { return currentPrice; }
}