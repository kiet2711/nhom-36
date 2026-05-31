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

    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colType;
    @FXML private TableColumn<JsonObject, String> colPrice;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colLeader;
    @FXML private TableColumn<JsonObject, String> colEnd;
    @FXML private Label                           statusLabel;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f đ", d.getValue().get("currentPrice").getAsDouble())));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colLeader.setCellValueFactory(d -> {
            JsonElement leader = d.getValue().get("leadingBidder");
            return new SimpleStringProperty(leader == null || leader.isJsonNull() ? "Chưa có" : leader.getAsString());
        });
        colEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("endTime").getAsString().replace("T", " ")));
        auctionTable.setItems(list);

        // ★ Push listener để cập nhật realtime
        client.setPushListener(this::handlePush);

        handleRefresh();
    }

    /** ★ Xử lý PUSH: cập nhật giá/trạng thái realtime cho phiên của Seller */
    private void handlePush(String jsonData) {
        try {
            JsonObject updated = JsonParser.parseString(jsonData).getAsJsonObject();
            String auctionId = updated.get("id").getAsString();
            String sellerId = SessionManager.getCurrentUserId();

            // Chỉ xử lý phiên của Seller hiện tại
            JsonElement sellerElem = updated.get("sellerId");
            if (sellerElem == null || !sellerId.equals(sellerElem.getAsString())) return;

            Platform.runLater(() -> {
                String status = updated.get("status").getAsString();
                if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
                    // Xóa phiên đã kết thúc khỏi bảng
                    list.removeIf(o -> o.get("id").getAsString().equals(auctionId));
                    statusLabel.setText("Tổng: " + list.size() + " phiên đang mở");

                    if ("FINISHED".equals(status)) {
                        String itemName = updated.get("itemName").getAsString();
                        JsonElement leader = updated.get("leadingBidder");
                        String winner = (leader == null || leader.isJsonNull()) ? "Không có" : leader.getAsString();
                        String price = String.format("%,.0f đ", updated.get("currentPrice").getAsDouble());
                        AlertUtil.info("Phiên kết thúc",
                                "Phiên \"" + itemName + "\" đã kết thúc!\n"
                                + "Người thắng: " + winner + "\nGiá cuối: " + price);
                    }
                } else {
                    // Cập nhật giá/trạng thái mới
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).get("id").getAsString().equals(auctionId)) {
                            list.set(i, updated);
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("SellerDashboard PUSH error: " + e.getMessage());
        }
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request  req     = new Request(CommandType.GET_AUCTIONS, null);
                Response res     = client.send(req);
                if (!res.isOk()) return;
                String sellerId  = SessionManager.getCurrentUserId();
                JsonArray all    = JsonParser.parseString(res.getData()).getAsJsonArray();
                Platform.runLater(() -> {
                    list.clear();
                    all.forEach(e -> {
                        JsonObject o = e.getAsJsonObject();
                        if (sellerId.equals(o.get("sellerId").getAsString())) list.add(o);
                    });
                    statusLabel.setText("Tổng: " + list.size() + " phiên đang mở");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleCreate() {
        try { client.setPushListener(null); SceneManager.switchTo("CreateAuction.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }

    @FXML
    public void handleCancel() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn phiên cần hủy."); return; }
        String status = selected.get("status").getAsString();
        if (!status.equals("OPEN") && !status.equals("RUNNING")) {
            AlertUtil.warning("Thông báo", "Chỉ có thể hủy phiên đang mở."); return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Request  req = new Request(CommandType.CANCEL_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thành công", "Đã hủy phiên."); handleRefresh(); }
                    else AlertUtil.error("Lỗi", res.getData());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleEdit() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn phiên cần sửa."); return; }
        String status = selected.get("status").getAsString();
        if (!status.equals("OPEN")) {
            AlertUtil.warning("Thông báo", "Chỉ có thể sửa phiên đang OPEN (chưa bắt đầu đấu giá)."); return;
        }
        try {
            client.setPushListener(null);
            EditAuctionController ctrl = SceneManager.switchToAndGetController("EditAuction.fxml");
            ctrl.loadAuction(selected);
        } catch (Exception e) {
            AlertUtil.error("Lỗi", e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        try { client.setPushListener(null); SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }
}
