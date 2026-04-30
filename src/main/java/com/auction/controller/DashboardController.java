// src/main/java/com/auction/controller/DashboardController.java
package com.auction.controller;

import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername()
                + " (" + SessionManager.getCurrentRole() + ")");
    }

    @FXML
    public void handleLogout() {
        SessionManager.clear();
        try {
            SceneManager.switchTo("Login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}