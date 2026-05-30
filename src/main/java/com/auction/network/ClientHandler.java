package com.auction.network;

import com.auction.factory.ItemFactory;
import com.auction.model.*;
import com.auction.network.observer.AuctionObserver;
import com.auction.service.AuctionService;
import com.auction.db.UserDAO;
import com.auction.db.BidTransactionDAO;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collection;

public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket socket;
    private final Gson gson = new Gson();
    /* CHANGED: protected → private — chỉ truy cập nội bộ class */
    private PrintWriter out;

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
            case GET_MY_BIDS    -> handleGetMyBids();
            case GET_WON_AUCTIONS -> handleGetWonAuctions();
            case GET_BID_HISTORY   -> handleGetBidHistory(req);
            case CREATE_AUCTION    -> handleCreateAuction(req);
            case PLACE_BID         -> handlePlaceBid(req);
            case REGISTER_AUTO_BID -> handleRegisterAutoBid(req);
            case CANCEL_AUTO_BID   -> handleCancelAutoBid(req);
            case CANCEL_AUCTION    -> handleCancelAuction(req);
            case GET_ALL_USERS     -> handleGetAllUsers();
            case UPDATE_USER_STATUS -> handleUpdateUserStatus(req);
            case DELETE_USER       -> handleDeleteUser(req);
            case ADMIN_FORCE_FINISH_AUCTION -> handleAdminForceFinishAuction(req);
            case GET_ALL_BIDS      -> handleGetAllBids();
            default -> Response.error("Lệnh chưa hỗ trợ: " + req.getCommand());
        };
    }

    // --- Auction handlers ---

    private Response handleGetBidHistory(Request req) {
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            BidTransactionDAO txDao = new BidTransactionDAO();
            var history = txDao.findByAuction(auctionId);
            JsonArray arr = new JsonArray();
            for (var record : history) {
                JsonObject obj = new JsonObject();
                obj.addProperty("bidderId", record.bidderId);
                obj.addProperty("amount", record.amount);
                obj.addProperty("bidTime", record.bidTime);
                arr.add(obj);
            }
            return Response.ok(arr.toString());
        } catch (Exception e) {
            return Response.error("Lỗi lấy lịch sử bid: " + e.getMessage());
        }
    }

    private Response handleGetAuctions() {
        try {
            Collection<Auction> auctions = auctionSvc.getAllAuctions();
            JsonArray arr = new JsonArray();
            for (Auction a : auctions) {
                // Chỉ trả về các phiên đang hoạt động (OPEN/RUNNING) cho Dashboard chính
                if (a.isActive()) {
                    arr.add(auctionToJson(a));
                }
            }
            return Response.ok(arr.toString());
        } catch (Exception e) {
            return Response.error("Lỗi lấy danh sách: " + e.getMessage());
        }
    }

    private Response handleGetWonAuctions() {
        Response loginCheck = requireLogin();
        if (loginCheck != null) return loginCheck;
        try {
            Collection<Auction> auctions = auctionSvc.getWonAuctions(loggedInUser.getId());
            JsonArray arr = new JsonArray();
            for (Auction a : auctions) {
                arr.add(auctionToJson(a));
            }
            return Response.ok(arr.toString());
        } catch (Exception e) {
            return Response.error("Lỗi lấy danh sách đã thắng: " + e.getMessage());
        }
    }

    private Response handleGetMyBids() {
        Response loginCheck = requireLogin();
        if (loginCheck != null) return loginCheck;
        if (!"BIDDER".equals(loggedInUser.getRole())) {
            return Response.error("Chỉ Bidder mới có thể xem danh sách đã đấu giá.");
        }
        try {
            Collection<Auction> auctions = auctionSvc.getAuctionsByBidder(loggedInUser.getId());
            JsonArray arr = new JsonArray();
            for (Auction a : auctions) {
                arr.add(auctionToJson(a));
            }
            return Response.ok(arr.toString());
        } catch (Exception e) {
            return Response.error("Lỗi lấy danh sách phiên đã tham gia: " + e.getMessage());
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

    private Response handleCancelAuction(Request req) {
        if (loggedInUser == null) return Response.error("Bạn chưa đăng nhập.");
        if (!loggedInUser.getRole().equals("SELLER") && !loggedInUser.getRole().equals("ADMIN")) {
            return Response.error("Bạn không có quyền hủy phiên đấu giá.");
        }
        try {
            JsonObject data  = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            
            if (loggedInUser.getRole().equals("ADMIN")) {
                auctionSvc.adminCancelAuction(auctionId);
            } else {
                auctionSvc.cancelAuction(auctionId, loggedInUser.getId());
            }
            return Response.ok("Đã hủy phiên.");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    // ======================== ADMIN HANDLERS ========================

    private Response requireAdmin() {
        if (loggedInUser == null) return Response.error("Bạn chưa đăng nhập.");
        if (!"ADMIN".equals(loggedInUser.getRole())) return Response.error("Không có quyền Admin.");
        return null;
    }

    private Response handleGetAllUsers() {
        Response auth = requireAdmin(); if (auth != null) return auth;
        UserDAO userDao = new UserDAO();
        return Response.ok(userDao.getAllUsers().toString());
    }

    private Response handleUpdateUserStatus(Request req) {
        Response auth = requireAdmin(); if (auth != null) return auth;
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String userId = data.get("userId").getAsString();
            String status = data.get("status").getAsString();
            UserDAO userDao = new UserDAO();
            if (userDao.updateStatus(userId, status)) {
                return Response.ok("Đã cập nhật trạng thái user.");
            } else {
                return Response.error("Không tìm thấy user.");
            }
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    private Response handleDeleteUser(Request req) {
        Response auth = requireAdmin(); if (auth != null) return auth;
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String userId = data.get("userId").getAsString();
            UserDAO userDao = new UserDAO();
            if (userDao.deleteUser(userId)) {
                return Response.ok("Đã xóa user thành công.");
            } else {
                return Response.error("Lỗi khi xóa user.");
            }
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    private Response handleAdminForceFinishAuction(Request req) {
        Response auth = requireAdmin(); if (auth != null) return auth;
        try {
            JsonObject data = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            auctionSvc.closeAuction(auctionId); // Đóng ngay lập tức
            return Response.ok("Đã kết thúc phiên đấu giá.");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    private Response handleGetAllBids() {
        Response auth = requireAdmin(); if (auth != null) return auth;
        BidTransactionDAO txDao = new BidTransactionDAO();
        return Response.ok(txDao.getAllTransactions().toString());
    }

    // ======================== COMMON HELPERS ========================

    private Response handlePlaceBid(Request req) {
        /* CHANGED: extract duplicated login check → requireLogin() */
        Response loginCheck = requireLogin();
        if (loginCheck != null) return loginCheck;
        if (!"BIDDER".equals(loggedInUser.getRole())) {
            return Response.error("Chỉ Bidder mới có thể đặt giá.");
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

    // --- Auto-Bid handlers ---

    private Response handleRegisterAutoBid(Request req) {
        Response loginCheck = requireLogin();
        if (loginCheck != null) return loginCheck;
        if (!"BIDDER".equals(loggedInUser.getRole())) {
            return Response.error("Chỉ Bidder mới có thể đăng ký auto-bid.");
        }
        try {
            JsonObject data  = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();
            double maxBid    = data.get("maxBid").getAsDouble();
            double increment = data.get("increment").getAsDouble();

            auctionSvc.registerAutoBid(auctionId, loggedInUser.getId(),
                    maxBid, increment);

            JsonObject result = new JsonObject();
            result.addProperty("message", "Đã đăng ký auto-bid thành công.");
            result.addProperty("auctionId", auctionId);
            result.addProperty("maxBid", maxBid);
            result.addProperty("increment", increment);
            return Response.ok(result.toString());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    private Response handleCancelAutoBid(Request req) {
        Response loginCheck = requireLogin();
        if (loginCheck != null) return loginCheck;
        if (!"BIDDER".equals(loggedInUser.getRole())) {
            return Response.error("Chỉ Bidder mới có thể hủy auto-bid.");
        }
        try {
            JsonObject data  = JsonParser.parseString(req.getData()).getAsJsonObject();
            String auctionId = data.get("auctionId").getAsString();

            auctionSvc.cancelAutoBid(auctionId, loggedInUser.getId());

            return Response.ok("Đã hủy auto-bid thành công.");
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

    /* CHANGED: public → package-private — chỉ AuctionServer cùng package gọi */
    void send(Response res) {
        if (out != null) out.println(gson.toJson(res));
    }

    /* CHANGED: extract duplicated login check dùng ở 4 handler */
    private Response requireLogin() {
        return (loggedInUser == null) ? Response.error("Bạn chưa đăng nhập.") : null;
    }

    /* CHANGED: đổi tên biến 'o' → 'json' cho rõ nghĩa */
    private JsonObject auctionToJson(Auction a) {
        JsonObject json = new JsonObject();
        json.addProperty("id",              a.getId());
        json.addProperty("itemName",        a.getItem().getName());
        json.addProperty("itemType",        a.getItem().getType());
        json.addProperty("itemDescription", a.getItem().getDescription());
        json.addProperty("itemDetails",     a.getItem().getDetails());
        json.addProperty("currentPrice",    a.getCurrentPrice());
        json.addProperty("startingPrice",   a.getItem().getStartingPrice());
        json.addProperty("status",          a.getStatus().name());
        json.addProperty("startTime",       a.getStartTime().toString());
        json.addProperty("endTime",         a.getEndTime().toString());
        json.addProperty("leadingBidder",   a.getLeadingBidderId());
        json.addProperty("sellerId",        a.getSellerId());
        return json;
    }
}