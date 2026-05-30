package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

public class BiddingController {

    @FXML private Label     titleLabel;
    @FXML private Label     typeLabel;
    @FXML private Label     currentPriceLabel;
    @FXML private Label     leadingBidderLabel;
    @FXML private Label     endTimeLabel;
    @FXML private Label     statusLabel;
    @FXML private Label     countdownLabel;
    @FXML private Label     clockLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label     resultLabel;
    @FXML private Button    placeBidBtn;

    // Auto-Bid UI
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private Button    registerAutoBidBtn;
    @FXML private Button    cancelAutoBidBtn;
    @FXML private Label     autoBidStatusLabel;

    // Chart
    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis   chartYAxis;
    private XYChart.Series<String, Number> bidSeries;

    // Winner notification
    @FXML private VBox  winnerBox;
    @FXML private Label winnerLabel;

    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

    private JsonObject      auctionData;
    private String          currentAuctionId;
    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson gson = new Gson();
    private Timeline countdownTimeline;
    private Timeline clockTimeline;

    @FXML
    public void initialize() {
        client.setPushListener(this::handlePush);
        initChart();
        startClock();
    }

    private void initChart() {
        bidSeries = new XYChart.Series<>();
        bidSeries.setName("Giá đấu giá");
        if (bidHistoryChart != null) {
            bidHistoryChart.getData().add(bidSeries);
        }
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (clockLabel != null) clockLabel.setText("🕐 " + LocalDateTime.now().format(CLOCK_FMT));
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown() {
        if (auctionData == null || countdownLabel == null) return;
        try {
            LocalDateTime end = LocalDateTime.parse(auctionData.get("endTime").getAsString());
            long totalSecs = ChronoUnit.SECONDS.between(LocalDateTime.now(), end);
            if (totalSecs <= 0) {
                countdownLabel.setText("⏱ Hết giờ!");
                countdownLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            } else {
                long h = totalSecs / 3600, m = (totalSecs % 3600) / 60, s = totalSecs % 60;
                countdownLabel.setText(String.format("⏱ Còn lại: %02d:%02d:%02d", h, m, s));
                if (totalSecs <= 60) {
                    countdownLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
                } else {
                    countdownLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #059669;");
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadAuction(JsonObject data) {
        this.auctionData      = data;
        this.currentAuctionId = data.get("id").getAsString();
        refreshUI(data);
        addChartDataPoint(data);
        startCountdown();
    }

    private void handlePush(String jsonData) {
        try {
            JsonObject updated = JsonParser.parseString(jsonData).getAsJsonObject();
            if (!updated.get("id").getAsString().equals(currentAuctionId)) return;
            this.auctionData = updated;
            Platform.runLater(() -> {
                refreshUI(updated);
                addChartDataPoint(updated);
            });
        } catch (Exception e) {
            System.err.println("Lỗi xử lý PUSH: " + e.getMessage());
        }
    }

    private void refreshUI(JsonObject data) {
        titleLabel.setText(data.get("itemName").getAsString());
        JsonElement details = data.get("itemDetails");
        typeLabel.setText(details != null && !details.isJsonNull() ? details.getAsString() : data.get("itemType").getAsString());
        currentPriceLabel.setText(String.format("%,.0f đ", data.get("currentPrice").getAsDouble()));
        JsonElement leader = data.get("leadingBidder");
        leadingBidderLabel.setText(leader == null || leader.isJsonNull() ? "Chưa có" : leader.getAsString());
        endTimeLabel.setText(data.get("endTime").getAsString().replace("T", " "));
        String status = data.get("status").getAsString();
        statusLabel.setText("Trạng thái: " + status);
        
        boolean ended = status.equals("FINISHED") || status.equals("CANCELED");
        boolean isBidder = "BIDDER".equals(SessionManager.getCurrentRole());
        
        // Show winner notification
        if (ended && winnerBox != null) {
            winnerBox.setVisible(true);
            winnerBox.setManaged(true);
            if (status.equals("FINISHED")) {
                String winnerId = (leader == null || leader.isJsonNull()) ? "Không xác định" : leader.getAsString();
                boolean isWinner = winnerId.equals(SessionManager.getCurrentUserId());
                if (isWinner) {
                    winnerLabel.setText("🎉 CHÚC MỪNG! Bạn đã thắng phiên đấu giá này với giá " 
                            + String.format("%,.0f đ", data.get("currentPrice").getAsDouble()) + "!");
                    winnerBox.setStyle("-fx-background-color: #d1fae5; -fx-padding: 10; -fx-background-radius: 5;");
                    winnerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #065f46;");
                } else {
                    winnerLabel.setText("🏆 Phiên kết thúc! Người thắng: " + winnerId 
                            + " | Giá cuối: " + String.format("%,.0f đ", data.get("currentPrice").getAsDouble()));
                    winnerBox.setStyle("-fx-background-color: #fef3c7; -fx-padding: 10; -fx-background-radius: 5;");
                    winnerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #b45309;");
                }
            } else {
                winnerLabel.setText("❌ Phiên đấu giá đã bị hủy.");
                winnerBox.setStyle("-fx-background-color: #fee2e2; -fx-padding: 10; -fx-background-radius: 5;");
                winnerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");
            }
        }
        
        if (!isBidder) {
            setControlsVisible(false);
        } else {
            if (placeBidBtn != null) placeBidBtn.setDisable(ended);
            if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(ended);
            if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(ended);
            if (bidAmountField != null) bidAmountField.setDisable(ended);
            if (autoBidMaxField != null) autoBidMaxField.setDisable(ended);
            if (autoBidIncrementField != null) autoBidIncrementField.setDisable(ended);
        }
        
        if (ended && !isBidder) {
            resultLabel.setStyle("-fx-text-fill: orange;");
            resultLabel.setText("Chỉ Bidder mới được phép đặt giá.");
        }
    }

    private void setControlsVisible(boolean visible) {
        if (placeBidBtn != null) { placeBidBtn.setVisible(visible); placeBidBtn.setManaged(visible); }
        if (bidAmountField != null) { bidAmountField.setVisible(visible); bidAmountField.setManaged(visible); }
        if (registerAutoBidBtn != null) { registerAutoBidBtn.setVisible(visible); registerAutoBidBtn.setManaged(visible); }
        if (cancelAutoBidBtn != null) { cancelAutoBidBtn.setVisible(visible); cancelAutoBidBtn.setManaged(visible); }
        if (autoBidMaxField != null) { autoBidMaxField.setVisible(visible); autoBidMaxField.setManaged(visible); }
        if (autoBidIncrementField != null) { autoBidIncrementField.setVisible(visible); autoBidIncrementField.setManaged(visible); }
    }

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
        if (amountText.isEmpty()) { showError("Vui lòng nhập giá."); return; }
        double amount;
        try { amount = Double.parseDouble(amountText); }
        catch (NumberFormatException e) { showError("Giá không hợp lệ."); return; }

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
                        resultLabel.setText("Đặt giá thành công: " + String.format("%,.0f đ", amount));
                        bidAmountField.clear();
                    } else {
                        showError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (placeBidBtn != null) placeBidBtn.setDisable(false);
                    showError("Lỗi kết nối: " + e.getMessage());
                });
            }
        });
    }

    // ======================== AUTO-BID HANDLERS ========================

    @FXML
    public void handleRegisterAutoBid() {
        String maxText = autoBidMaxField != null ? autoBidMaxField.getText().trim() : "";
        String incText = autoBidIncrementField != null ? autoBidIncrementField.getText().trim() : "";

        if (maxText.isEmpty() || incText.isEmpty()) {
            showAutoBidError("Vui lòng nhập đầy đủ giá tối đa và bước nhảy.");
            return;
        }

        double maxBid, increment;
        try {
            maxBid    = Double.parseDouble(maxText);
            increment = Double.parseDouble(incText);
        } catch (NumberFormatException e) {
            showAutoBidError("Giá không hợp lệ.");
            return;
        }

        if (maxBid <= 0 || increment <= 0) {
            showAutoBidError("Giá tối đa và bước nhảy phải lớn hơn 0.");
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
                        if (autoBidStatusLabel != null) {
                            autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                            autoBidStatusLabel.setText("✅ Auto-bid đang chạy: tối đa "
                                    + String.format("%,.0f", maxBid) + " đ, bước " 
                                    + String.format("%,.0f", increment) + " đ");
                        }
                        if (autoBidMaxField != null) autoBidMaxField.clear();
                        if (autoBidIncrementField != null) autoBidIncrementField.clear();
                    } else {
                        showAutoBidError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (registerAutoBidBtn != null) registerAutoBidBtn.setDisable(false);
                    showAutoBidError("Lỗi kết nối: " + e.getMessage());
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
                        if (autoBidStatusLabel != null) {
                            autoBidStatusLabel.setStyle("-fx-text-fill: orange;");
                            autoBidStatusLabel.setText("Đã tắt auto-bid.");
                        }
                    } else {
                        showAutoBidError(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (cancelAutoBidBtn != null) cancelAutoBidBtn.setDisable(false);
                    showAutoBidError("Lỗi kết nối: " + e.getMessage());
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
        if (countdownTimeline != null) countdownTimeline.stop();
        if (clockTimeline != null) clockTimeline.stop();
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        resultLabel.setStyle("-fx-text-fill: red;");
        resultLabel.setText(msg);
    }
}
