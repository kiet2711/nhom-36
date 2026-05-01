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

    @FXML private Label titleLabel;
    @FXML private Label typeLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leadingBidderLabel;
    @FXML private Label endTimeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label resultLabel;

    private JsonObject auctionData;
    private final AuctionClient client = AuctionClient.getInstance();

    /** Được gọi từ DashboardController sau khi switch scene */
    public void loadAuction(JsonObject data) {
        this.auctionData = data;
        refreshUI(data);
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
    }

    @FXML
    public void handlePlaceBid() {
        String amountText = bidAmountField.getText().trim();
        if (amountText.isEmpty()) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Vui lòng nhập giá.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Giá không hợp lệ.");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId",
                        auctionData.get("id").getAsString());
                payload.addProperty("amount", amount);

                Request req = new Request(CommandType.PLACE_BID, payload.toString());
                Response res = client.send(req);

                Platform.runLater(() -> {
                    if (res.isOk()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("Đặt giá thành công: "
                                + String.format("%,.0f đ", amount));
                        // Cập nhật UI local luôn
                        auctionData.addProperty("currentPrice", amount);
                        auctionData.addProperty("leadingBidder",
                                SessionManager.getCurrentUserId());
                        refreshUI(auctionData);
                        bidAmountField.clear();
                    } else {
                        resultLabel.setStyle("-fx-text-fill: red;");
                        resultLabel.setText(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    resultLabel.setStyle("-fx-text-fill: red;");
                    resultLabel.setText("Lỗi kết nối: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}