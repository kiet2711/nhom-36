package com.auction.model;

public class Electronics extends Item {
    private int warrantyMonths = 12; // Thuộc tính riêng của Điện tử

    public Electronics(String id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override public String getType() { return "ELECTRONICS"; }

    @Override
    public String getDetails() {
        return "Loại: Điện tử | Bảo hành: " + warrantyMonths + " tháng";
    }
}