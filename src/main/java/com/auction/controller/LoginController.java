package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson gson = new Gson();

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            if (!client.isConnected()) client.connect();

            JsonObject payload = new JsonObject();
            payload.addProperty("username", username);
            payload.addProperty("password", password);

            Request req = new Request(CommandType.LOGIN, payload.toString());
            Response res = client.send(req);

            if (res.isOk()) {
                // Lưu thông tin user vào session
                JsonObject userData = gson.fromJson(res.getData(), JsonObject.class);
                SessionManager.setCurrentUser(
                        userData.get("id").getAsString(),
                        userData.get("username").getAsString(),
                        userData.get("role").getAsString()
                );
                SceneManager.switchTo("Dashboard.fxml");
            } else {
                errorLabel.setText(res.getData());
            }
        } catch (Exception e) {
            errorLabel.setText("Lỗi kết nối server: " + e.getMessage());
        }
    }

    @FXML
    public void goToRegister() {
        try {
            SceneManager.switchTo("Register.fxml");
        } catch (Exception e) {
            errorLabel.setText("Lỗi chuyển màn hình.");
        }
    }
}