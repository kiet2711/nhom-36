package com.auction.network.observer;

import com.auction.model.Auction;

/**
 * THỂ HIỆN OBSERVER PATTERN (Mẫu thiết kế Quan sát viên)
 * Interface này đóng vai trò là "Observer" (người quan sát).
 * Khi trạng thái của phiên đấu giá thay đổi (có người đặt giá mới),
 * "Subject" (AuctionService) sẽ gọi phương thức onAuctionUpdated() 
 * để thông báo cho tất cả các Observer đang đăng ký.
 */
public interface AuctionObserver {
    void onAuctionUpdated(Auction auction);
}
