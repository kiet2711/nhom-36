package com.auction.network;

import com.auction.factory.ItemFactory;
import com.auction.model.*;
import com.auction.network.observer.AuctionObserver;
import com.auction.service.AuctionService;
import com.auction.db.UserDAO;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collection;

public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket socket;
    private final Gson gson = new Gson();
    protected PrintWriter out;

    private final UserDAO userDAO           = new UserDAO();
    private final AuctionService auctionSvc = AuctionService.getInstance();
    private User loggedInUser = null;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            auctionSvc.addObserver(this);

            String line;
            while ((line = in.readLine()) != null) {
                Request req = gson.fromJson(line, Request.class);
                Response res = dispatch(req);
                if (res != null) out.println(gson.toJson(res));
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
            auctionSvc.removeObserver(this);
        }
    }

    private Response dispatch(Request req) {
        if (req.getCommand() == null) return Response.error("Lệnh không hợp lệ");
        return switch (req.getCommand()) {
            case LOGIN          -> handleLogin(req);
            case REGISTER       -> handleRegister(req);
            case GET_AUCTIONS   -> handleGetAuctions();
            case CREATE_AUCTION -> handleCreateAuction(req);
            case PLACE_BID      -> handlePlaceBid(req);
            default -> Response.error("Lệnh chưa hỗ trợ: " + req.getCommand());
        };
    }

    // --- Auction handlers ---

    private Response handleGetAuctions() {
        try {
            Collection<Auction> auctions = auctionSvc.getAllAuctions();
            JsonArray arr = new JsonArray();
            for (Auction a : auctions) {
                arr.add(auctionToJson(a));
            }
            return Response.ok(arr.toString());
        } catch (Exception e) {
            return Response.error("Lỗi lấy danh sách: " + e.getMessage());
        }
    }

    private Response handleCreateAuction(Request req) {
        if (loggedInUser == null || !loggedInUser.getRole().equals("SELLER")) {
            return Response.error("Chỉ Seller mới có thể tạo phiên đấu giá.");
        }
        try {
            JsonObject data  = JsonParser.parseString(req.getData()).getAsJsonObject();
            String type      = data.get("type").getAsString();
            String name      = data.get("name").getAsString();
            String desc      = data.get("description").getAsString();
            double price     = data.get("startingPrice").getAsDouble();
            String endTimeStr= data.get("endTime").getAsString();

            Item item = ItemFactory.create(type, name, desc, price);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            Auction auction = auctionSvc.createAuction(item,
                    loggedInUser.getId(), endTime);

            return Response.ok(auctionToJson(auction).toString());
        } catch (Exception e) {
            return Response.error("Lỗi tạo phiên: " + e.getMessage());
        }
    }

    private Response handlePlaceBid(Request req) {
        if (loggedInUser == null) {
            return Response.error("Bạn chưa đăng nhập.");
        }
        try {
            JsonObject data  = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            double amount    = data.get("amount").getAsDouble();

            BidTransaction tx = auctionSvc.placeBid(
                    auctionId, loggedInUser.getId(), amount);

            JsonObject result = new JsonObject();
            result.addProperty("txId",    tx.getId());
            result.addProperty("amount",  tx.getAmount());
            result.addProperty("bidTime", tx.getBidTime().toString());
            return Response.ok(result.toString());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    // --- Observer callback: push cập nhật tới client đang xem ---
    @Override
    public void onAuctionUpdated(Auction auction) {
        if (out == null) return;
        // Gửi dạng UPDATE_PUSH, client sẽ tự cập nhật UI
        Response push = new Response("PUSH", auctionToJson(auction).toString());
        out.println(gson.toJson(push));
    }

    // --- Login / Register (giữ nguyên từ Tuần 2) ---

    private Response handleLogin(Request req) {
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();
            var user = userDAO.login(username, password);
            if (user.isPresent()) {
                loggedInUser = user.get();
                JsonObject result = new JsonObject();
                result.addProperty("id",       loggedInUser.getId());
                result.addProperty("username", loggedInUser.getUsername());
                result.addProperty("role",     loggedInUser.getRole());
                return Response.ok(result.toString());
            }
            return Response.error("Sai tên đăng nhập hoặc mật khẩu.");
        } catch (Exception e) {
            return Response.error("Lỗi login: " + e.getMessage());
        }
    }

    private Response handleRegister(Request req) {
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();
            String role     = data.get("role").getAsString();
            boolean ok = userDAO.register(username, password, role);
            return ok ? Response.ok("Đăng ký thành công!")
                    : Response.error("Tên đăng nhập đã tồn tại.");
        } catch (Exception e) {
            return Response.error("Lỗi đăng ký: " + e.getMessage());
        }
    }

    public void send(Response res) {
        if (out != null) out.println(gson.toJson(res));
    }

    // --- Helper ---
    private JsonObject auctionToJson(Auction a) {
        JsonObject o = new JsonObject();
        o.addProperty("id",             a.getId());
        o.addProperty("itemName",       a.getItem().getName());
        o.addProperty("itemType",       a.getItem().getType());
        o.addProperty("currentPrice",   a.getCurrentPrice());
        o.addProperty("status",         a.getStatus().name());
        o.addProperty("endTime",        a.getEndTime().toString());
        o.addProperty("leadingBidder",  a.getLeadingBidderId());
        return o;
    }
}