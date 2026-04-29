package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        // Tuần 2 sẽ kết nối thật với server
        errorLabel.setText("Chưa kết nối server (Tuần 2)");
    }

    @FXML
    public void goToRegister() {
        // Tuần 2 sẽ chuyển màn hình
    }
}