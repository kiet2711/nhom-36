package com.auction.model;

public abstract class Item extends Entity {

    protected String name;
    protected String description;
    protected double startingPrice;

    public Item(String id, String name, String description, double startingPrice) {
        this.id           = id;
        this.name         = name;
        this.description  = description;
        this.startingPrice = startingPrice;
    }

    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public double getStartingPrice(){ return startingPrice; }

    /** Mỗi loại item mô tả thêm thông tin riêng */
    public abstract String getType();

    @Override
    public String toString() {
        return "[" + getType() + "] " + name + " - Giá khởi điểm: " + startingPrice;
    }
}