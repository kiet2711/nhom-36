package com.auction.model;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;
    protected String status = "ACTIVE"; // Thêm trạng thái mặc định

    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
    public String getStatus()   { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Mỗi subclass tự mô tả thông tin của mình */
    public abstract String describe();
}