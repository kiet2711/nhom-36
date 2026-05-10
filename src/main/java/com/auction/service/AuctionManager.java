package com.auction.service;

import com.auction.model.Auction;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private static AuctionManager instance;
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public void addAuction(Auction auction)        { auctions.put(auction.getId(), auction); }
    public Optional<Auction> getAuction(String id) { return Optional.ofNullable(auctions.get(id)); }
    public Collection<Auction> getAllAuctions()     { return auctions.values(); }
    public void removeAuction(String id)           { auctions.remove(id); }
}
