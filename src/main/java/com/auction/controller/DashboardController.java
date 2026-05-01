package com.auction.controller;

import com.auction.network.*;
import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.Executors;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<JsonObject> auctionTable;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colType;
    @FXML private TableColumn<JsonObject, String> colPrice;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colEnd;
    @FXML private Button createAuctionBtn;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> auctionList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername()
                + " (" + SessionManager.getCurrentRole() + ")");

        // Ẩn nút tạo đấu giá nếu không phải Seller
        createAuctionBtn.setVisible("SELLER".equals(SessionManager.getCurrentRole()));

        // Bind columns
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("%,.0f đ",
                        d.getValue().get("currentPrice").getAsDouble())));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("endTime").getAsString().replace("T", " ")));

        auctionTable.setItems(auctionList);

        // Lắng nghe PUSH từ server trong background thread
        startPushListener();
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request req = new Request(CommandType.GET_AUCTIONS, null);
                Response res = client.send(req);
                if (res.isOk()) {
                    JsonArray arr = JsonParser.parseString(res.getData()).getAsJsonArray();
                    Platform.runLater(() -> {
                        auctionList.clear();
                        arr.forEach(e -> auctionList.add(e.getAsJsonObject()));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        showAlert("Lỗi", "Không thể tải danh sách: " + e.getMessage()));
            }
        });
    }

    @FXML
    public void handleJoinAuction() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một phiên đấu giá.");
            return;
        }
        String status = selected.get("status").getAsString();
        if (status.equals("FINISHED") || status.equals("CANCELED")) {
            showAlert("Thông báo", "Phiên đấu giá này đã kết thúc.");
            return;
        }
        try {
            BiddingController ctrl =
                    SceneManager.switchToAndGetController("Bidding.fxml");
            ctrl.loadAuction(selected);
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể mở màn hình đấu giá: " + e.getMessage());
        }
    }

    @FXML
    public void handleCreateAuction() {
        try {
            SceneManager.switchTo("CreateAuction.fxml");
        } catch (Exception e) {
            showAlert("Lỗi", "Không mở được màn hình tạo đấu giá: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.clear();
        try { SceneManager.switchTo("Login.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /** Thread riêng lắng nghe PUSH realtime từ server */
    private void startPushListener() {
        // Sẽ implement đầy đủ ở Tuần 4
        // Tuần 3 dùng refresh thủ công là đủ
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}