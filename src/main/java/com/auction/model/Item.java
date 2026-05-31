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
    public void setName(String name){ this.name = name; }
    public String getDescription()  { return description; }
    public void setDescription(String description){ this.description = description; }
    public double getStartingPrice(){ return startingPrice; }
    public void setStartingPrice(double startingPrice){ this.startingPrice = startingPrice; }

    /** Mỗi loại item mô tả thêm thông tin riêng */
    public abstract String getType();

    /** 
     * THỂ HIỆN TÍNH ĐA HÌNH: Trả về thông tin chi tiết đặc thù của từng loại sản phẩm.
     */
    public abstract String getDetails();

    @Override
    public String toString() {
        return "[" + getType() + "] " + name + " - Giá khởi điểm: " + startingPrice;
    }
}