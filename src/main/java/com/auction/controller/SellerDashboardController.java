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

public class SellerDashboardController {

    @FXML private TableView<JsonObject>              auctionTable;
    @FXML private TableColumn<JsonObject, String>    colName;
    @FXML private TableColumn<JsonObject, String>    colType;
    @FXML private TableColumn<JsonObject, String>    colPrice;
    @FXML private TableColumn<JsonObject, String>    colStatus;
    @FXML private TableColumn<JsonObject, String>    colLeader;
    @FXML private TableColumn<JsonObject, String>    colEnd;
    @FXML private Label                              statusLabel;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> list   = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("%,.0f đ",
                        d.getValue().get("currentPrice").getAsDouble())));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colLeader.setCellValueFactory(d -> {
            JsonElement leader = d.getValue().get("leadingBidder");
            return new SimpleStringProperty(
                    leader.isJsonNull() ? "Chưa có" : leader.getAsString());
        });
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("endTime").getAsString().replace("T", " ")));

        auctionTable.setItems(list);
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request  req  = new Request(CommandType.GET_AUCTIONS, null);
                Response res  = client.send(req);
                if (!res.isOk()) return;

                String sellerId = SessionManager.getCurrentUserId();
                JsonArray all   = JsonParser.parseString(res.getData()).getAsJsonArray();

                Platform.runLater(() -> {
                    list.clear();
                    all.forEach(e -> {
                        JsonObject o = e.getAsJsonObject();
                        // Chỉ hiện phiên của seller đang đăng nhập
                        if (sellerId.equals(o.get("sellerId").getAsString())) {
                            list.add(o);
                        }
                    });
                    statusLabel.setText("Tổng: " + list.size() + " phiên");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.error("Lỗi", "Không thể tải dữ liệu: " + e.getMessage()));
            }
        });
    }

    @FXML
    public void handleCreate() {
        try { SceneManager.switchTo("CreateAuction.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }

    @FXML
    public void handleCancel() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warning("Thông báo", "Vui lòng chọn phiên cần huỷ.");
            return;
        }
        String status = selected.get("status").getAsString();
        if (!status.equals("OPEN") && !status.equals("RUNNING")) {
            AlertUtil.warning("Thông báo", "Chỉ có thể huỷ phiên đang mở.");
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Request  req = new Request(CommandType.CANCEL_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) {
                        AlertUtil.info("Thành công", "Đã huỷ phiên đấu giá.");
                        handleRefresh();
                    } else {
                        AlertUtil.error("Lỗi", res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }
}