package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label         errorLabel;

    private final AuctionClient client = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("BIDDER", "SELLER");
        roleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role     = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            if (!client.isConnected()) client.connect();

            JsonObject payload = new JsonObject();
            payload.addProperty("username", username);
            payload.addProperty("password", password);
            payload.addProperty("role", role);

            Request req = new Request(CommandType.REGISTER, payload.toString());
            Response res = client.send(req);

            if (res.isOk()) {
                errorLabel.setStyle("-fx-text-fill: green;");
                errorLabel.setText(res.getData());
            } else {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText(res.getData());
            }
        } catch (Exception e) {
            errorLabel.setText("Lỗi kết nối: " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            SceneManager.switchTo("Login.fxml");
        } catch (Exception e) {
            errorLabel.setText("Lỗi chuyển màn hình.");
        }
    }
}