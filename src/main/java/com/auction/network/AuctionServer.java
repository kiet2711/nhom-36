package com.auction.network;

import com.auction.db.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {

    private static final int PORT = 9999;

    public void start() {
        // Khởi tạo database và schema trước
        DatabaseManager.getInstance();

        System.out.println("Server khởi động tại cổng " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client kết nối: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}