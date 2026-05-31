package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction extends Entity {

    public enum Status { OPEN, RUNNING, FINISHED, CANCELED, PAID }

    // ★ Anti-sniping constants (dễ config)
    public static final long SNIPE_WINDOW_SECONDS    = 60; // X giây cuối trước endTime
    public static final long SNIPE_EXTENSION_SECONDS = 60; // Extend thêm Y giây

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

    /**
     * Đặt giá cho phiên đấu giá.
     * Validate trước khi cập nhật currentPrice, leadingBidderId, status.
     *
     * @param bidderId ID người đặt giá
     * @param amount   số tiền đặt (phải lớn hơn currentPrice)
     * @return transaction mô tả bid vừa thực hiện
     * @throws AuctionClosedException nếu phiên đã kết thúc
     * @throws InvalidBidException    nếu giá không hợp lệ
     * @throws UnauthorizedException  nếu seller tự bid
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
            throw new AuctionClosedException(this.id);
        }
        if (amount <= currentPrice) {
            throw new InvalidBidException(amount, currentPrice);
        }
        if (bidderId.equals(sellerId)) {
            throw new UnauthorizedException("Người bán không thể tự đấu giá.");
        }
    }

    /** Đóng phiên đấu giá. FINISHED nếu có bidder, CANCELED nếu không. */
    public void close() {
        if (status != Status.FINISHED && status != Status.CANCELED) {
            this.status = (leadingBidderId != null) ? Status.FINISHED : Status.CANCELED;
        }
    }

    /** Kiểm tra phiên đấu giá còn hoạt động (OPEN hoặc RUNNING). */
    public boolean isActive() {
        return status == Status.OPEN || status == Status.RUNNING;
    }

    /**
     * Anti-sniping: gia hạn endTime nếu bid xảy ra trong {@link #SNIPE_WINDOW_SECONDS}
     * giây cuối trước khi kết thúc.
     *
     * @return true nếu endTime đã được gia hạn, false nếu không cần
     */
    public boolean checkAndExtendEndTime() {
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);

        if (secondsRemaining >= 0 && secondsRemaining <= SNIPE_WINDOW_SECONDS) {
            this.endTime = this.endTime.plusSeconds(SNIPE_EXTENSION_SECONDS);
            System.out.println("Anti-sniping: endTime extended to " + this.endTime);
            return true;
        }
        return false;
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
    public void setEndTime(LocalDateTime t)   { this.endTime = t; }
    public void forceLeadingBidder(String id) { this.leadingBidderId = id; }
}
