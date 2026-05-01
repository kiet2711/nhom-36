package com.auction.util;

/** Lưu thông tin user đang đăng nhập, dùng chung toàn app */
public class SessionManager {

    private static String currentUserId;
    private static String currentUsername;
    private static String currentRole;

    public static void setCurrentUser(String id, String username, String role) {
        currentUserId   = id;
        currentUsername = username;
        currentRole     = role;
    }

    public static String getCurrentUserId()   { return currentUserId; }
    public static String getCurrentUsername() { return currentUsername; }
    public static String getCurrentRole()     { return currentRole; }

    public static void clear() {
        currentUserId   = null;
        currentUsername = null;
        currentRole     = null;
    }
}