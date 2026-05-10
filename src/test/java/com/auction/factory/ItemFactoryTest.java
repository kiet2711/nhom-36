package com.auction.factory;

import com.auction.model.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    @DisplayName("Factory tạo đúng loại Electronics")
    void createElectronics() {
        Item item = ItemFactory.create("ELECTRONICS", "Phone", "desc", 5_000_000);
        assertInstanceOf(Electronics.class, item);
        assertEquals("ELECTRONICS", item.getType());
        assertEquals("Phone", item.getName());
    }

    @Test
    @DisplayName("Factory tạo đúng loại Art")
    void createArt() {
        Item item = ItemFactory.create("ART", "Painting", "desc", 3_000_000);
        assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("Factory tạo đúng loại Vehicle")
    void createVehicle() {
        Item item = ItemFactory.create("VEHICLE", "Motorbike", "desc", 20_000_000);
        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("Factory ném exception với loại không hợp lệ")
    void createInvalidTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.create("UNKNOWN", "X", "desc", 0));
    }

    @Test
    @DisplayName("Factory tạo ID không null và không rỗng")
    void createdItemHasId() {
        Item item = ItemFactory.create("ART", "X", "desc", 1000);
        assertNotNull(item.getId());
        assertFalse(item.getId().isBlank());
    }

    @Test
    @DisplayName("Factory phân biệt hoa thường — type lowercase cũng chạy")
    void caseInsensitiveType() {
        assertDoesNotThrow(() -> ItemFactory.create("electronics", "X", "desc", 1000));
    }
}