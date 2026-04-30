package com.auction.network;

import com.auction.db.UserDAO;
import com.auction.model.User;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();
    protected PrintWriter out;

    private final UserDAO userDAO=new UserDAO();
    private User loggedInUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            out = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                Request req = gson.fromJson(line, Request.class);
                Response res = dispatch(req);
                out.println(gson.toJson(res));
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
        }
    }

    private Response dispatch(Request req) {
        if (req.getCommand() == null) return Response.error("Lệnh không hợp lệ");
        return switch (req.getCommand()) {
            case LOGIN    -> handleLogin(req);
            case REGISTER -> handleRegister(req);
            default       -> Response.error("Lệnh chưa được hỗ trợ: " + req.getCommand());
        };
    }

    private Response handleLogin(Request req) {
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            Optional<User> user = userDAO.login(username, password);
            if (user.isPresent()) {
                loggedInUser = user.get();
                JsonObject result = new JsonObject();
                result.addProperty("id",       loggedInUser.getId());
                result.addProperty("username", loggedInUser.getUsername());
                result.addProperty("role",     loggedInUser.getRole());
                return Response.ok(result.toString());
            } else {
                return Response.error("Sai tên đăng nhập hoặc mật khẩu.");
            }
        } catch (Exception e) {
            return Response.error("Lỗi xử lý login: " + e.getMessage());
        }
    }

    private Response handleRegister(Request req) {
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();
            String role     = data.get("role").getAsString();

            if (username.isBlank() || password.isBlank()) {
                return Response.error("Tên đăng nhập và mật khẩu không được trống.");
            }
            if (!role.equals("BIDDER") && !role.equals("SELLER")) {
                return Response.error("Vai trò không hợp lệ.");
            }

            boolean success = userDAO.register(username, password, role);
            return success
                    ? Response.ok("Đăng ký thành công! Vui lòng đăng nhập.")
                    : Response.error("Tên đăng nhập đã tồn tại.");
        } catch (Exception e) {
            return Response.error("Lỗi xử lý đăng ký: " + e.getMessage());
        }
    }

    public void send(Response response) {
        if (out != null) out.println(gson.toJson(response));
    }
}