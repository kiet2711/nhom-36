package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
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
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        client.setPushListener(this::handlePush);
    }

    public void loadAuction(JsonObject data) {
        this.auctionData      = data;
        this.currentAuctionId = data.get("id").getAsString();
        refreshUI(data);
    }

    private void handlePush(String jsonData) {
        try {
            JsonObject updated = JsonParser.parseString(jsonData).getAsJsonObject();
            if (!updated.get("id").getAsString().equals(currentAuctionId)) return;
            this.auctionData = updated;
            Platform.runLater(() -> refreshUI(updated));
        } catch (Exception e) {
            System.err.println("Loi xu ly PUSH: " + e.getMessage());
        }
    }

    private void refreshUI(JsonObject data) {
        titleLabel.setText(data.get("itemName").getAsString());
        typeLabel.setText("Loai: " + data.get("itemType").getAsString());
        currentPriceLabel.setText(String.format("%,.0f d", data.get("currentPrice").getAsDouble()));
        JsonElement leader = data.get("leadingBidder");
        leadingBidderLabel.setText(leader == null || leader.isJsonNull() ? "Chua co" : leader.getAsString());
        endTimeLabel.setText(data.get("endTime").getAsString().replace("T", " "));
        String status = data.get("status").getAsString();
        statusLabel.setText("Trang thai: " + status);
        boolean ended = status.equals("FINISHED") || status.equals("CANCELED");
        if (placeBidBtn != null) placeBidBtn.setDisable(ended);
        if (ended) {
            resultLabel.setStyle("-fx-text-fill: gray;");
            resultLabel.setText("Phien dau gia da ket thuc.");
        }
    }

    @FXML
    public void handlePlaceBid() {
        String amountText = bidAmountField.getText().trim();
        if (amountText.isEmpty()) { showError("Vui long nhap gia."); return; }
        double amount;
        try { amount = Double.parseDouble(amountText); }
        catch (NumberFormatException e) { showError("Gia khong hop le."); return; }

        if (placeBidBtn != null) placeBidBtn.setDisable(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                payload.addProperty("amount", amount);
                Request  req = new Request(CommandType.PLACE_BID, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (placeBidBtn != null) placeBidBtn.setDisable(false);
                    if (res.isOk()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("Dat gia thanh cong: " + String.format("%,.0f d", amount));
                        bidAmountField.clear();
                    } else {
                        showError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (placeBidBtn != null) placeBidBtn.setDisable(false);
                    showError("Loi ket noi: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleBack() {
        client.setPushListener(null);
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        resultLabel.setStyle("-fx-text-fill: red;");
        resultLabel.setText(msg);
    }
}
