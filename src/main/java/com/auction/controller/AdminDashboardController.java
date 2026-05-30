package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class AdminDashboardController {

    // Tab: Auctions
    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colAuctionId;
    @FXML private TableColumn<JsonObject, String> colAuctionName;
    @FXML private TableColumn<JsonObject, String> colAuctionPrice;
    @FXML private TableColumn<JsonObject, String> colAuctionStatus;
    @FXML private TableColumn<JsonObject, String> colAuctionSeller;
    @FXML private TableColumn<JsonObject, String> colAuctionEnd;

    // Tab: Users
    @FXML private TableView<JsonObject>           userTable;
    @FXML private TableColumn<JsonObject, String> colUserId;
    @FXML private TableColumn<JsonObject, String> colUsername;
    @FXML private TableColumn<JsonObject, String> colUserRole;
    @FXML private TableColumn<JsonObject, String> colUserStatus;

    // Tab: Bids
    @FXML private TableView<JsonObject>           bidTable;
    @FXML private TableColumn<JsonObject, String> colBidTime;
    @FXML private TableColumn<JsonObject, String> colBidAuction;
    @FXML private TableColumn<JsonObject, String> colBidUsername;
    @FXML private TableColumn<JsonObject, String> colBidAmount;

    // Tab: Stats
    @FXML private Label statTotal;
    @FXML private Label statRunning;
    @FXML private Label statFinished;
    @FXML private Label statCanceled;
    @FXML private Label statTotalUsers;
    @FXML private Label statActiveUsers;
    @FXML private Label statLockedUsers;
    @FXML private Label statTotalBids;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> auctionList = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> userList    = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> bidList     = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Init Auction Table
        colAuctionId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id").getAsString().substring(0, 8) + "..."));
        colAuctionName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colAuctionPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f đ", d.getValue().get("currentPrice").getAsDouble())));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colAuctionSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("sellerId").getAsString().substring(0, 8) + "..."));
        colAuctionEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("endTime").getAsString().replace("T", " ")));
        auctionTable.setItems(auctionList);

        // Init User Table
        colUserId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id").getAsString()));
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("username").getAsString()));
        colUserRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("role").getAsString()));
        colUserStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
        userTable.setItems(userList);

        // Init Bid Table
        colBidTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("bidTime").getAsString().replace("T", " ")));
        colBidAuction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colBidUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("username").getAsString()));
        colBidAmount.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f đ", d.getValue().get("amount").getAsDouble())));
        bidTable.setItems(bidList);

        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Load Auctions
                Response resAuc = client.send(new Request(CommandType.GET_AUCTIONS, null));
                // Load Users
                Response resUsr = client.send(new Request(CommandType.GET_ALL_USERS, null));
                // Load Bids
                Response resBid = client.send(new Request(CommandType.GET_ALL_BIDS, null));

                Platform.runLater(() -> {
                    // Update Auctions
                    if (resAuc.isOk()) {
                        JsonArray arr = JsonParser.parseString(resAuc.getData()).getAsJsonArray();
                        auctionList.clear();
                        AtomicLong run = new AtomicLong(), fin = new AtomicLong(), canc = new AtomicLong();
                        arr.forEach(e -> {
                            JsonObject o = e.getAsJsonObject();
                            auctionList.add(o);
                            switch (o.get("status").getAsString()) {
                                case "RUNNING"  -> run.incrementAndGet();
                                case "FINISHED" -> fin.incrementAndGet();
                                case "CANCELED" -> canc.incrementAndGet();
                            }
                        });
                        statTotal.setText("Tổng số phiên: " + auctionList.size());
                        statRunning.setText("Đang chạy (RUNNING): " + run.get());
                        statFinished.setText("Đã kết thúc (FINISHED): " + fin.get());
                        statCanceled.setText("Đã hủy (CANCELED): " + canc.get());
                    }

                    // Update Users
                    if (resUsr.isOk()) {
                        JsonArray arr = JsonParser.parseString(resUsr.getData()).getAsJsonArray();
                        userList.clear();
                        AtomicLong act = new AtomicLong(), lck = new AtomicLong();
                        arr.forEach(e -> {
                            JsonObject o = e.getAsJsonObject();
                            userList.add(o);
                            if ("LOCKED".equals(o.get("status").getAsString())) lck.incrementAndGet();
                            else act.incrementAndGet();
                        });
                        statTotalUsers.setText("Tổng User: " + userList.size());
                        statActiveUsers.setText("Đang hoạt động: " + act.get());
                        statLockedUsers.setText("Bị khóa: " + lck.get());
                    }

                    // Update Bids
                    if (resBid.isOk()) {
                        JsonArray arr = JsonParser.parseString(resBid.getData()).getAsJsonArray();
                        bidList.clear();
                        arr.forEach(e -> bidList.add(e.getAsJsonObject()));
                        statTotalBids.setText("Tổng số lượt đặt giá (Bids): " + bidList.size());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", "Không thể tải dữ liệu: " + e.getMessage()));
            }
        });
    }

    // ================= AUCTION ACTIONS =================

    @FXML
    public void handleForceFinish() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn phiên cần đóng sớm."); return; }
        if (!"RUNNING".equals(selected.get("status").getAsString()) && !"OPEN".equals(selected.get("status").getAsString())) {
            AlertUtil.warning("Thông báo", "Chỉ có thể đóng phiên đang chạy hoặc mở."); return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Response res = client.send(new Request(CommandType.ADMIN_FORCE_FINISH_AUCTION, payload.toString()));
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thành công", "Đã kết thúc phiên (Force Finish)."); handleRefresh(); }
                    else AlertUtil.error("Lỗi", res.getData());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleForceCancel() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn phiên cần hủy."); return; }
        if (!"RUNNING".equals(selected.get("status").getAsString()) && !"OPEN".equals(selected.get("status").getAsString())) {
            AlertUtil.warning("Thông báo", "Chỉ có thể hủy phiên đang chạy hoặc mở."); return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Response res = client.send(new Request(CommandType.CANCEL_AUCTION, payload.toString()));
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thành công", "Đã hủy phiên đấu giá."); handleRefresh(); }
                    else AlertUtil.error("Lỗi", res.getData());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    // ================= USER ACTIONS =================

    @FXML
    public void handleToggleUserLock() {
        JsonObject selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn user."); return; }
        if ("ADMIN".equals(selected.get("role").getAsString())) {
            AlertUtil.warning("Cảnh báo", "Không thể thao tác lên ADMIN khác."); return;
        }

        String currentStatus = selected.get("status").getAsString();
        String newStatus = "LOCKED".equals(currentStatus) ? "ACTIVE" : "LOCKED";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("userId", selected.get("id").getAsString());
                payload.addProperty("status", newStatus);
                Response res = client.send(new Request(CommandType.UPDATE_USER_STATUS, payload.toString()));
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thành công", "Đã đổi trạng thái user thành " + newStatus); handleRefresh(); }
                    else AlertUtil.error("Lỗi", res.getData());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleDeleteUser() {
        JsonObject selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn user cần xóa."); return; }
        if ("ADMIN".equals(selected.get("role").getAsString())) {
            AlertUtil.warning("Cảnh báo", "Không thể xóa ADMIN."); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hành động này sẽ XÓA VĨNH VIỄN user và toàn bộ phiên đấu giá/lịch sử bid của họ. Bạn có chắc chắn không?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        JsonObject payload = new JsonObject();
                        payload.addProperty("userId", selected.get("id").getAsString());
                        Response res = client.send(new Request(CommandType.DELETE_USER, payload.toString()));
                        Platform.runLater(() -> {
                            if (res.isOk()) { AlertUtil.info("Thành công", "Đã xóa user."); handleRefresh(); }
                            else AlertUtil.error("Lỗi", res.getData());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }
}
