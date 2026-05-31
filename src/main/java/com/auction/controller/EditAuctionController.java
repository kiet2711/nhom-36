package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;

public class EditAuctionController {

    @FXML private TextField        nameField;
    @FXML private TextField        descField;
    @FXML private TextField        priceField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker       endDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Label            resultLabel;

    private final AuctionClient client = AuctionClient.getInstance();
    private String currentAuctionId;

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        // Setup hour spinner (0-23)
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        hourSpinner.setEditable(true);

        // Setup minute spinner (0-59)
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        minuteSpinner.setEditable(true);
    }

    public void loadAuction(JsonObject auction) {
        currentAuctionId = auction.get("id").getAsString();
        nameField.setText(auction.get("itemName").getAsString());
        if (auction.has("itemDescription") && !auction.get("itemDescription").isJsonNull()) {
            descField.setText(auction.get("itemDescription").getAsString());
        }
        priceField.setText(String.format("%.0f", auction.get("currentPrice").getAsDouble()));
        typeCombo.setValue(auction.get("itemType").getAsString());

        LocalDateTime endTime = LocalDateTime.parse(auction.get("endTime").getAsString());
        endDatePicker.setValue(endTime.toLocalDate());
        hourSpinner.getValueFactory().setValue(endTime.getHour());
        minuteSpinner.getValueFactory().setValue(endTime.getMinute());
    }

    @FXML
    public void handleSave() {
        String name     = nameField.getText().trim();
        String desc     = descField.getText().trim();
        String priceStr = priceField.getText().trim();
        LocalDate date  = endDatePicker.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || date == null) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Vui lòng điền đầy đủ thông tin."); return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Giá không hợp lệ."); return;
        }

        int hour   = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        if (endTime.isBefore(LocalDateTime.now())) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Thời gian kết thúc phải ở tương lai."); return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", currentAuctionId);
                payload.addProperty("name", name);
                payload.addProperty("description", desc);
                payload.addProperty("startingPrice", price);
                payload.addProperty("endTime", endTime.toString());
                Request  req = new Request(CommandType.UPDATE_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) {
                        AlertUtil.info("Thành công", "Đã lưu thay đổi.");
                        handleBack();
                    } else {
                        resultLabel.setStyle("-fx-text-fill: red;");
                        resultLabel.setText(res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    resultLabel.setStyle("-fx-text-fill: red;");
                    resultLabel.setText("Lỗi: " + e.getMessage());
                });
            }
        });
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("SellerDashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
