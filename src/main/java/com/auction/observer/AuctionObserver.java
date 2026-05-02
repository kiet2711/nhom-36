package com.auction.observer;

import com.auction.model.Auction;

/** Observer interface — Tuần 4 sẽ implement để push realtime */
public interface AuctionObserver {
    void onAuctionUpdated(Auction auction);
}