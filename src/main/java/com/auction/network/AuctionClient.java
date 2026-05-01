package com.auction.network;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

/** Client-side socket, dùng chung cho toàn bộ session */
public class AuctionClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private static AuctionClient instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    private AuctionClient() {}

    public static AuctionClient getInstance() {
        if (instance == null) instance = new AuctionClient();
        return instance;
    }

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("Kết nối server thành công.");
    }

    /** Gửi request, chờ response đồng bộ */
    public Response send(Request request) throws IOException {
        out.println(gson.toJson(request));
        String line = in.readLine();
        return gson.fromJson(line, Response.class);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}