package com.auction.model;

import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private static final String SELLER_ID = "seller-001";
    private static final String BIDDER_A  = "bidder-A";
    private static final String BIDDER_B  = "bidder-B";

    @BeforeEach
    void setUp() {
        Item item = new Electronics("item-1", "Laptop", "Gaming laptop", 10_000_000);
        auction = new Auction(
                "auction-1", item, SELLER_ID,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );
    }

    @Test
    @DisplayName("Giá khởi điểm bằng giá sản phẩm")
    void initialPriceEqualsStartingPrice() {
        assertEquals(10_000_000, auction.getCurrentPrice());
    }

    @Test
    @DisplayName("Đặt giá hợp lệ — cập nhật giá và người dẫn đầu")
    void validBidUpdatesState() {
        auction.placeBid(BIDDER_A, 11_000_000);
        assertEquals(11_000_000, auction.getCurrentPrice());
        assertEquals(BIDDER_A, auction.getLeadingBidderId());
        assertEquals(Auction.Status.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("Đặt giá thấp hơn giá hiện tại — ném exception")
    void bidBelowCurrentPriceThrows() {
        auction.placeBid(BIDDER_A, 11_000_000);
        assertThrows(InvalidBidException.class,
                () -> auction.placeBid(BIDDER_B, 9_000_000));
    }

    @Test
    @DisplayName("Đặt giá bằng giá hiện tại — ném exception")
    void bidEqualCurrentPriceThrows() {
        assertThrows(InvalidBidException.class,
                () -> auction.placeBid(BIDDER_A, 10_000_000));
    }

    @Test
    @DisplayName("Người bán không được tự đấu giá")
    void sellerCannotBid() {
        assertThrows(UnauthorizedException.class,
                () -> auction.placeBid(SELLER_ID, 15_000_000));
    }

    @Test
    @DisplayName("Đặt giá sau khi phiên đóng — ném exception")
    void bidAfterClosedThrows() {
        auction.placeBid(BIDDER_A, 11_000_000);
        auction.close();
        assertThrows(AuctionClosedException.class,
                () -> auction.placeBid(BIDDER_B, 12_000_000));
    }

    @Test
    @DisplayName("Đóng phiên có người bid — status FINISHED")
    void closeWithBidderFinished() {
        auction.placeBid(BIDDER_A, 11_000_000);
        auction.close();
        assertEquals(Auction.Status.FINISHED, auction.getStatus());
    }

    @Test
    @DisplayName("Đóng phiên không có ai bid — status CANCELED")
    void closeWithoutBidderCanceled() {
        auction.close();
        assertEquals(Auction.Status.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("Lịch sử bid ghi nhận đúng số lần")
    void bidHistoryTracksCorrectly() {
        auction.placeBid(BIDDER_A, 11_000_000);
        auction.placeBid(BIDDER_B, 12_000_000);
        auction.placeBid(BIDDER_A, 13_000_000);
        assertEquals(3, auction.getBidHistory().size());
    }

    @Test
    @DisplayName("isActive() trả về false sau khi đóng")
    void isActiveReturnsFalseAfterClose() {
        auction.placeBid(BIDDER_A, 11_000_000);
        assertTrue(auction.isActive());
        auction.close();
        assertFalse(auction.isActive());
    }
}