package com.auction.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();
    protected PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                Request req = gson.fromJson(line, Request.class);
                // Tuần 3 sẽ dispatch đến các handler tương ứng
                Response res = Response.error("Chưa xử lý: " + req.getCommand());
                out.println(gson.toJson(res));
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
        }
    }

    public void send(Response response) {
        if (out != null) out.println(gson.toJson(response));
    }
}