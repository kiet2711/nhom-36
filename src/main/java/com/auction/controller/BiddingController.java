package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.Executors;

public class BiddingController {

    @FXML private Label     titleLabel;
    @FXML private Label     typeLabel;
    @FXML private Label     currentPriceLabel;
    @FXML private Label     leadingBidderLabel;
    @FXML private Label     endTimeLabel;
    @FXML private Label     statusLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label     resultLabel;
    @FXML private Button    placeBidBtn;

    private JsonObject      auctionData;
    private String          currentAuctionId;
    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson      gson       = new Gson();

    @FXML
    public void initialize() {
        // Đăng ký lắng nghe PUSH từ server
        client.setPushListener(this::handlePush);
    }

    /** Được gọi từ DashboardController sau khi switch scene */
    public void loadAuction(JsonObject data) {
        this.auctionData      = data;
        this.currentAuctionId = data.get("id").getAsString();
        refreshUI(data);
    }

    /**
     * Nhận dữ liệu PUSH từ server (chạy trên background thread).
     * Chỉ cập nhật nếu đúng phiên đang xem.
     */
    private void handlePush(String jsonData) {
        try {
            JsonObject updated = JsonParser.parseString(jsonData).getAsJsonObject();
            String pushedId = updated.get("id").getAsString();
            if (!pushedId.equals(currentAuctionId)) return;

            this.auctionData = updated;
            Platform.runLater(() -> refreshUI(updated));
        } catch (Exception e) {
            System.err.println("Lỗi xử lý PUSH: " + e.getMessage());
        }
    }

    private void refreshUI(JsonObject data) {
        titleLabel.setText(data.get("itemName").getAsString());
        typeLabel.setText("Loại: " + data.get("itemType").getAsString());
        currentPriceLabel.setText(String.format("%,.0f đ",
                data.get("currentPrice").getAsDouble()));

        String leader = data.get("leadingBidder").isJsonNull()
                ? "Chưa có"
                : data.get("leadingBidder").getAsString();
        leadingBidderLabel.setText(leader);
        endTimeLabel.setText(data.get("endTime").getAsString().replace("T", " "));

        String status = data.get("status").getAsString();
        statusLabel.setText("Trạng thái: " + status);

        // Khoá nút nếu phiên kết thúc
        boolean ended = status.equals("FINISHED") || status.equals("CANCELED");
        placeBidBtn.setDisable(ended);
        if (ended) {
            resultLabel.setStyle("-fx-text-fill: gray;");
            resultLabel.setText("Phiên đấu giá đã kết thúc.");
        }
    }

    @FXML
    public void handlePlaceBid() {
        String amountText = bidAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showError("Vui lòng nhập giá.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ.");
            return;
        }

        placeBidBtn.setDisable(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                payload.addProperty("amount",    amount);

                Request  req = new Request(CommandType.PLACE_BID, payload.toString());
                Response res = client.send(req);

                Platform.runLater(() -> {
                    placeBidBtn.setDisable(false);
                    if (res.isOk()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("Đặt giá thành công: "
                                + String.format("%,.0f đ", amount));
                        bidAmountField.clear();
                        // UI sẽ tự cập nhật qua PUSH — không cần set thủ công
                    } else {
                        showError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    placeBidBtn.setDisable(false);
                    showError("Lỗi kết nối: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleBack() {
        // Huỷ push listener khi rời màn hình
        client.setPushListener(null);
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        resultLabel.setStyle("-fx-text-fill: red;");
        resultLabel.setText(msg);
    }
}