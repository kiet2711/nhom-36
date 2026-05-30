package com.auction.model;

public class Vehicle extends Item {
    private int mileage = 0; // Thuộc tính riêng của Xe cộ

    public Vehicle(String id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override public String getType() { return "VEHICLE"; }

    @Override
    public String getDetails() {
        return "Loại: Xe cộ | ODO (số km đã đi): " + mileage + " km";
    }
}