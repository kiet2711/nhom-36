package com.auction.model;

public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password, "BIDDER");
    }

    @Override
    public String describe() {
        return "Bidder: " + username;
    }
}