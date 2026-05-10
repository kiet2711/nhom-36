package com.auction.network;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Client socket hai luồng:
 *  - Luồng WRITE: gửi Request, nhận Response tương ứng (dùng BlockingQueue)
 *  - Luồng READ : nhận PUSH bất đồng bộ từ server, gọi pushListener
 */
public class AuctionClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 9999;

    private static AuctionClient instance;

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private final Gson     gson = new Gson();

    // Queue chứa response của request đồng bộ
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    // Callback khi nhận PUSH từ server
    private Consumer<String> pushListener;

    private AuctionClient() {}

    public static AuctionClient getInstance() {
        if (instance == null) instance = new AuctionClient();
        return instance;
    }

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out    = new PrintWriter(socket.getOutputStream(), true);
        in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startReadLoop();
        System.out.println("Kết nối server thành công.");
    }

    /** Vòng lặp đọc chạy trong daemon thread */
    private void startReadLoop() {
        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Response res = gson.fromJson(line, Response.class);
                    if ("PUSH".equals(res.getStatus())) {
                        // Giao cho pushListener xử lý
                        if (pushListener != null) pushListener.accept(res.getData());
                    } else {
                        // Response của một request cụ thể
                        responseQueue.put(res);
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối server: " + e.getMessage());
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    /** Gửi request, chờ response (timeout 10 giây) */
    public Response send(Request request) throws IOException, InterruptedException {
        out.println(gson.toJson(request));
        Response res = responseQueue.poll(10, TimeUnit.SECONDS);
        if (res == null) throw new IOException("Server không phản hồi (timeout).");
        return res;
    }

    public void setPushListener(Consumer<String> listener) {
        this.pushListener = listener;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}