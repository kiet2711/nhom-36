package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // ★ Auto-Bid UI elements
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private Button    registerAutoBidBtn;
    @FXML private Button    cancelAutoBidBtn;
    @FXML private Label     autoBidStatusLabel;

    // ★ Bid History Chart
    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis   chartYAxis;
    private XYChart.Series<String, Number> bidSeries;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private JsonObject      auctionData;
    private String          currentAuctionId;
    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        client.setPushListener(this::handlePush);
        initChart();
    }

    private void initChart() {
        bidSeries = new XYChart.Series<>();
        bidSeries.setName("Giá đấu giá");
        if (bidHistoryChart != null) {
            bidHistoryChart.getData().add(bidSeries);
        }
    }

    public void loadAuction(JsonObject data) {
        this.auctionData      = data;
        this.currentAuctionId = data.get("id").getAsString();
        refreshUI(data);
        // Add initial data point to chart
        addChartDataPoint(data);
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
        boolean isBidder = "BIDDER".equals(SessionManager.getCurrentRole());
        boolean disableBidding = ended || !isBidder;
        
        if (!isBidder) {
            // Hide the controls entirely for non-bidders
            if (placeBidBtn != null) { placeBidBtn.setVisible(false); placeBidBtn.setManaged(false); }
            if (bidAmountField != null) { bidAmountField.setVisible(false); bidAmountField.setManaged(false); }
            if (registerAutoBidBtn != null) { registerAutoBidBtn.setVisible(false); registerAutoBidBtn.setManaged(false); }
            if (cancelAutoBidBtn != null) { cancelAutoBidBtn.setVisible(false); cancelAutoBidBtn.setManaged(false); }
            if (autoBidMaxField != null) { autoBidMaxField.setVisible(false); autoBidMaxField.setManaged(false); }
            if (autoBidIncrementField != null) { autoBidIncrementField.setVisible(false); autoBidIncrementField.setManaged(false); }
        } else {
            // Enable/disable based on whether auction ended
            if (placeBidBtn != null) placeBidBtn.setDisable(ended);
            if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(ended);
            if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(ended);
        }
        
        if (ended) {
            resultLabel.setStyle("-fx-text-fill: gray;");
            resultLabel.setText("Phien dau gia da ket thuc.");
            if (autoBidStatusLabel != null) {
                autoBidStatusLabel.setStyle("-fx-text-fill: gray;");
                autoBidStatusLabel.setText("Phien da ket thuc - Auto-bid khong kha dung.");
            }
        } else if (!isBidder) {
            resultLabel.setStyle("-fx-text-fill: orange;");
            resultLabel.setText("Chỉ Bidder mới được phép đặt giá.");
            if (autoBidStatusLabel != null) {
                autoBidStatusLabel.setStyle("-fx-text-fill: orange;");
                autoBidStatusLabel.setText("Chỉ Bidder mới được phép đăng ký auto-bid.");
            }
        }
        
        // ★ Add data point to bid history chart
        addChartDataPoint(data);
    }

    /**
     * ★ Thêm data point vào bid history chart.
     * Trục X = timestamp (HH:mm:ss), Trục Y = currentPrice.
     */
    private void addChartDataPoint(JsonObject data) {
        if (bidHistoryChart == null || bidSeries == null) return;
        try {
            double price = data.get("currentPrice").getAsDouble();
            String timestamp = LocalDateTime.now().format(TIME_FMT);
            bidSeries.getData().add(new XYChart.Data<>(timestamp, price));
        } catch (Exception e) {
            System.err.println("Chart update error: " + e.getMessage());
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

    // ======================== AUTO-BID HANDLERS ========================

    @FXML
    public void handleRegisterAutoBid() {
        String maxText = autoBidMaxField.getText().trim();
        String incText = autoBidIncrementField.getText().trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            showAutoBidError("Vui long nhap day du gia toi da va buoc nhay.");
            return;
        }

        double maxBid, increment;
        try {
            maxBid    = Double.parseDouble(maxText);
            increment = Double.parseDouble(incText);
        } catch (NumberFormatException e) {
            showAutoBidError("Gia khong hop le.");
            return;
        }

        if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                payload.addProperty("maxBid", maxBid);
                payload.addProperty("increment", increment);
                Request  req = new Request(CommandType.REGISTER_AUTO_BID, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(false);
                    if (res.isOk()) {
                        autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                        autoBidStatusLabel.setText("Auto-bid dang hoat dong: toi da "
                                + String.format("%,.0f", maxBid) + " d, buoc nhay "
                                + String.format("%,.0f", increment) + " d");
                        autoBidMaxField.clear();
                        autoBidIncrementField.clear();
                    } else {
                        showAutoBidError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(false);
                    showAutoBidError("Loi ket noi: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleCancelAutoBid() {
        if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                Request  req = new Request(CommandType.CANCEL_AUTO_BID, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(false);
                    if (res.isOk()) {
                        autoBidStatusLabel.setStyle("-fx-text-fill: orange;");
                        autoBidStatusLabel.setText("Da huy auto-bid.");
                    } else {
                        showAutoBidError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(false);
                    showAutoBidError("Loi ket noi: " + e.getMessage());
                });
            }
        });
    }

    private void showAutoBidError(String msg) {
        if (autoBidStatusLabel != null) {
            autoBidStatusLabel.setStyle("-fx-text-fill: red;");
            autoBidStatusLabel.setText(msg);
        }
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
