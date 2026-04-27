package com.auction.model;

public class Vehicle extends Item {
    public Vehicle(String id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override public String getType() { return "VEHICLE"; }
}