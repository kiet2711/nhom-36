package com.auction;

import com.auction.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.init(stage);
        stage.setTitle("Hệ thống Đấu giá");
        SceneManager.switchTo("Login.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}