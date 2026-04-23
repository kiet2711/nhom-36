package com.auction.model;

public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
    }

    @Override
    public String describe() {
        return "Seller: " + username;
    }
}