package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("BIDDER", "SELLER");
    }

    @FXML
    public void handleRegister() {
        errorLabel.setText("Chưa kết nối server (Tuần 2)");
    }

    @FXML
    public void goToLogin() {}
}