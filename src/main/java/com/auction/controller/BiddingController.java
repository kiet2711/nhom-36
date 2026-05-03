package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BiddingController {

    @FXML private Label titleLabel;
    @FXML private Label typeLabel;
    @FXML private Label statusLabel;       // FIX: thêm field còn thiếu - có trong FXML nhưng thiếu ở đây
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

        // FIX: hiển thị trạng thái auction
        if (statusLabel != null && data.has("status")) {
            statusLabel.setText("Trạng thái: " + data.get("status").getAsString());
        }

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

        // FIX: dùng Task thay vì tạo executor mới mỗi lần (tránh thread leak)
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", auctionData.get("id").getAsString());
                payload.addProperty("amount", amount);

                Request req = new Request(CommandType.PLACE_BID, payload.toString());
                return client.send(req);
            }
        };

        task.setOnSucceeded(e -> {
            Response res = task.getValue();
            if (res.isOk()) {
                resultLabel.setStyle("-fx-text-fill: green;");
                resultLabel.setText("Đặt giá thành công: "
                        + String.format("%,.0f đ", amount));
                // FIX: dùng getCurrentUsername() thay vì getCurrentUserId()
                auctionData.addProperty("currentPrice", amount);
                auctionData.addProperty("leadingBidder",
                        SessionManager.getCurrentUsername());
                refreshUI(auctionData);
                bidAmountField.clear();
            } else {
                resultLabel.setStyle("-fx-text-fill: red;");
                resultLabel.setText(res.getData());
            }
        });

        task.setOnFailed(e -> {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Lỗi kết nối: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}