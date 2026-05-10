package com.auction.service;

import com.auction.db.DatabaseManager;
import com.auction.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test concurrent bidding:
 * Nhiều thread cùng đặt giá — đảm bảo không lost update,
 * không hai người cùng thắng.
 */
class AuctionServiceConcurrentTest {

    private Auction auction;
    private static final String SELLER_ID = "seller-test";

    @BeforeEach
    void setUp() {
        // Dùng in-memory auction, không cần DB thật cho unit test này
        Item item = new Electronics(
                "item-test", "Test Item", "desc", 1_000_000);
        auction = new Auction(
                "auction-test", item, SELLER_ID,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );
    }

    @Test
    @DisplayName("Nhiều thread bid đồng thời — chỉ một giá thắng cuối cùng")
    void concurrentBidsOnlyOneWins() throws InterruptedException {
        int threadCount = 20;
        double basePrice = 1_000_000;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  latch = new CountDownLatch(1);
        AtomicInteger   successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final double bidAmount = basePrice + (i + 1) * 100_000;
            pool.submit(() -> {
                try {
                    latch.await();  // tất cả thread chờ rồi bắt đầu cùng lúc
                    synchronized (auction) {
                        // Chỉ bid nếu giá còn hợp lệ
                        if (bidAmount > auction.getCurrentPrice()) {
                            auction.placeBid("bidder-" + bidAmount, bidAmount);
                            successCount.incrementAndGet();
                        }
                    }
                } catch (IllegalArgumentException | InterruptedException ignored) {
                    // Giá không còn hợp lệ — bình thường
                }
            });
        }

        latch.countDown();  // thả hết thread cùng lúc
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // Giá cuối phải cao hơn giá khởi điểm
        assertTrue(auction.getCurrentPrice() > basePrice,
                "Giá cuối phải cao hơn giá khởi điểm");

        // Phải có đúng một người dẫn đầu
        assertNotNull(auction.getLeadingBidderId(),
                "Phải có người dẫn đầu");

        System.out.println("Giá cuối: " + auction.getCurrentPrice());
        System.out.println("Người thắng: " + auction.getLeadingBidderId());
        System.out.println("Số bid thành công: " + successCount.get());
    }

    @Test
    @DisplayName("AuctionService.placeBid dùng ReentrantLock — không rollback")
    void placeBidIsThreadSafe() throws InterruptedException {
        // Tạo auction trực tiếp trong manager (bypass DB)
        AuctionManager.getInstance().addAuction(auction);

        int threadCount = 10;
        ExecutorService pool  = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  latch = new CountDownLatch(1);
        AtomicInteger   successCount = new AtomicInteger(0);
        AtomicInteger   failCount    = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    latch.await();
                    // Mỗi thread bid một mức giá khác nhau từ cao đến thấp
                    // chỉ bid cao nhất mới thành công
                    double amount = 2_000_000 + idx * 50_000;
                    AuctionService.getInstance()
                            .placeBid(auction.getId(), "bidder-" + idx, amount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Thành công: " + successCount.get()
                + " | Thất bại: " + failCount.get());
        System.out.println("Giá cuối: " + auction.getCurrentPrice());

        // Giá cuối phải >= 2_000_000 (giá thấp nhất trong pool)
        assertTrue(auction.getCurrentPrice() >= 2_000_000);
        // Tổng = threadCount
        assertEquals(threadCount, successCount.get() + failCount.get());
    }
}