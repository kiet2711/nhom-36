package com.auction.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Bidder có role BIDDER")
    void bidderRole() {
        User u = new Bidder("1", "alice", "pass");
        assertEquals("BIDDER", u.getRole());
    }

    @Test
    @DisplayName("Seller có role SELLER")
    void sellerRole() {
        User u = new Seller("2", "bob", "pass");
        assertEquals("SELLER", u.getRole());
    }

    @Test
    @DisplayName("Admin có role ADMIN")
    void adminRole() {
        User u = new Admin("3", "admin", "pass");
        assertEquals("ADMIN", u.getRole());
    }

    @Test
    @DisplayName("describe() trả về thông tin đúng")
    void describeContainsUsername() {
        User u = new Bidder("1", "alice", "pass");
        assertTrue(u.describe().contains("alice"));
    }

    @Test
    @DisplayName("Polymorphism — gọi describe() qua kiểu User")
    void polymorphismDescribe() {
        User[] users = {
                new Bidder("1", "alice", "pass"),
                new Seller("2", "bob",   "pass"),
                new Admin ("3", "carol", "pass")
        };
        // Mỗi user phải describe() được — không throw
        for (User u : users) {
            assertDoesNotThrow(u::describe);
        }
    }
}