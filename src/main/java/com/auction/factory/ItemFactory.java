package com.auction.factory;

import com.auction.model.*;

import java.util.UUID;

public class ItemFactory {

    private ItemFactory() {}

    public static Item create(String type, String name,
                              String description, double startingPrice) {
        String id = UUID.randomUUID().toString();
        return switch (type.toUpperCase()) {
            case "ELECTRONICS" -> new Electronics(id, name, description, startingPrice);
            case "ART"         -> new Art(id, name, description, startingPrice);
            case "VEHICLE"     -> new Vehicle(id, name, description, startingPrice);
            default -> throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        };
    }
}
