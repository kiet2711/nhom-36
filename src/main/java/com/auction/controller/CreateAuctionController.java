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

public class CreateAuctionController {

    @FXML private TextField        nameField;
    @FXML private TextField        descField;
    @FXML private TextField        priceField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker       endDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Label            resultLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        typeCombo.getSelectionModel().selectFirst();

        // Setup DatePicker with default = tomorrow
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        // Setup hour spinner (0-23)
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        hourSpinner.setEditable(true);

        // Setup minute spinner (0-59)
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        minuteSpinner.setEditable(true);
    }

    @FXML
    public void handleCreate() {
        String name     = nameField.getText().trim();
        String desc     = descField.getText().trim();
        String priceStr = priceField.getText().trim();
        String type     = typeCombo.getValue();
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

        hourSpinner.commitValue();
        minuteSpinner.commitValue();
        int hour   = hourSpinner.getValue() == null ? 0 : hourSpinner.getValue();
        int minute = minuteSpinner.getValue() == null ? 0 : minuteSpinner.getValue();

        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        if (endTime.isBefore(LocalDateTime.now())) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Thời gian kết thúc phải ở tương lai."); return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("name", name);
                payload.addProperty("description", desc);
                payload.addProperty("startingPrice", price);
                payload.addProperty("type", type);
                payload.addProperty("endTime", endTime.toString());
                Request  req = new Request(CommandType.CREATE_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("✅ Tạo phiên thành công!");
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
