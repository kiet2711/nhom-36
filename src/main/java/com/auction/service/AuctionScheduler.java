package com.auction.service;

import com.auction.model.Auction;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Kiểm tra và đóng phiên đấu giá hết hạn mỗi 10 giây */
public class AuctionScheduler {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final AuctionService auctionService = AuctionService.getInstance();

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkExpiredAuctions,
                0, 10, TimeUnit.SECONDS);
        System.out.println("AuctionScheduler đã khởi động.");
    }

    private void checkExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionService.getAllAuctions().stream()
                .filter(Auction::isActive)
                .filter(a -> a.getEndTime().isBefore(now))
                .forEach(a -> {
                    try {
                        auctionService.closeAuction(a.getId());
                        System.out.println("Đã đóng phiên: " + a.getItem().getName());
                    } catch (Exception e) {
                        System.err.println("Lỗi đóng phiên: " + e.getMessage());
                    }
                });
    }

    public void stop() {
        scheduler.shutdown();
    }
}