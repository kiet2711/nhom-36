package com.auction.exception;

import com.auction.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        Item item = new Electronics("i1", "Laptop", "desc", 5_000_000);
        auction = new Auction("a1", item, "seller-1",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }

    @Test
    @DisplayName("InvalidBidException chứa thông tin giá đúng")
    void invalidBidExceptionHasCorrectAmounts() {
        InvalidBidException ex = assertThrows(InvalidBidException.class,
                () -> auction.placeBid("bidder-1", 3_000_000));
        assertEquals(3_000_000, ex.getAttemptedAmount());
        assertEquals(5_000_000, ex.getCurrentPrice());
    }

    @Test
    @DisplayName("AuctionClosedException khi bid vào phiên đã đóng")
    void auctionClosedExceptionThrown() {
        auction.placeBid("bidder-1", 6_000_000);
        auction.close();
        assertThrows(AuctionClosedException.class,
                () -> auction.placeBid("bidder-2", 7_000_000));
    }

    @Test
    @DisplayName("UnauthorizedException khi seller tự bid")
    void sellerBidThrowsUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> auction.placeBid("seller-1", 6_000_000));
    }

    @Test
    @DisplayName("Custom exception kế thừa AuctionException")
    void customExceptionsExtendBase() {
        assertInstanceOf(AuctionException.class,
                new InvalidBidException(1, 2));
        assertInstanceOf(AuctionException.class,
                new AuctionClosedException("x"));
        assertInstanceOf(AuctionException.class,
                new AuctionNotFoundException("x"));
        assertInstanceOf(AuctionException.class,
                new UnauthorizedException("x"));
    }
}