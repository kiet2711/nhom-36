package com.auction.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Tiện ích chuyển màn hình dùng chung */
public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) { primaryStage = stage; }

    public static void switchTo(String fxmlName) throws IOException {
        // FIX: đường dẫn dùng "/com.auction/" để khớp với thư mục resources
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/com.auction/" + fxmlName));
        if (loader.getLocation() == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlName);
        }
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
    }

    public static <T> T switchToAndGetController(String fxmlName) throws IOException {
        // FIX: đường dẫn dùng "/com.auction/" để khớp với thư mục resources
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/com.auction/" + fxmlName));
        if (loader.getLocation() == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlName);
        }
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        return loader.getController();
    }
}