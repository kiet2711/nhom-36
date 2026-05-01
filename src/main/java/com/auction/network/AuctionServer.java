package com.auction.network;

import com.auction.db.DatabaseManager;
import com.auction.service.AuctionScheduler;
import com.auction.service.AuctionService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {

    private static final int PORT = 9999;

    public void start() {
        DatabaseManager.getInstance();    // khởi tạo DB
        AuctionService.getInstance();     // load auctions từ DB
        new AuctionScheduler().start();   // bắt đầu kiểm tra hết hạn

        System.out.println("Server khởi động tại cổng " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client kết nối: " + client.getInetAddress());
                new Thread(new ClientHandler(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}