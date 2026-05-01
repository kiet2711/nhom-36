package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.Executors;

public class CreateAuctionController {

    @FXML private TextField nameField;
    @FXML private TextField descField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField endTimeField;
    @FXML private Label resultLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        typeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    public void handleCreate() {
        String name    = nameField.getText().trim();
        String desc    = descField.getText().trim();
        String priceStr= priceField.getText().trim();
        String type    = typeCombo.getValue();
        String endTime = endTimeField.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty() || endTime.isEmpty()) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("Vui lòng điền đầy đủ thông tin.");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("name", name);
                payload.addProperty("description", desc);
                payload.addProperty("startingPrice", Double.parseDouble(priceStr));
                payload.addProperty("type", type);
                payload.addProperty("endTime", endTime);

                Request req = new Request(CommandType.CREATE_AUCTION, payload.toString());
                Response res = client.send(req);

                Platform.runLater(() -> {
                    if (res.isOk()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("Tạo thành công!");
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
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}