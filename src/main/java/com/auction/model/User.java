package com.auction.model;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }

    /** Mỗi subclass tự mô tả thông tin của mình */
    public abstract String describe();
}