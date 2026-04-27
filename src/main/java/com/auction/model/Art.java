package com.auction.model;

public class Art extends Item {
    public Art(String id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override public String getType() { return "ART"; }
}