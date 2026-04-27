package com.auction.model;

public class Electronics extends Item {
    public Electronics(String id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override public String getType() { return "ELECTRONICS"; }
}