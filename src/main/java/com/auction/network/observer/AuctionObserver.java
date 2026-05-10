package com.auction.network.observer;

import com.auction.model.Auction;

public interface AuctionObserver {
    void onAuctionUpdated(Auction auction);
}
