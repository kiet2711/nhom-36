package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.Executors;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colType;
    @FXML private TableColumn<JsonObject, String> colPrice;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colEnd;
    @FXML private Button createAuctionBtn;
    @FXML private Button adminBtn;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> auctionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername()
                + " (" + SessionManager.getCurrentRole() + ")");

        String role = SessionManager.getCurrentRole();
        createAuctionBtn.setVisible("SELLER".equals(role));
        if (adminBtn != null) adminBtn.setVisible("ADMIN".equals(role));

        colName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("%,.0f d",
                        d.getValue().get("currentPrice").getAsDouble())));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colEnd.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().get("endTime").getAsString().replace("T", " ")));

        auctionTable.setItems(auctionList);
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request  req = new Request(CommandType.GET_AUCTIONS, null);
                Response res = client.send(req);
                if (res.isOk()) {
                    JsonArray arr = JsonParser.parseString(res.getData()).getAsJsonArray();
                    Platform.runLater(() -> {
                        auctionList.clear();
                        arr.forEach(e -> auctionList.add(e.getAsJsonObject()));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Loi", "Khong the tai danh sach: " + e.getMessage()));
            }
        });
    }

    @FXML
    public void handleJoinAuction() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thong bao", "Vui long chon mot phien dau gia."); return; }
        String status = selected.get("status").getAsString();
        if (status.equals("FINISHED") || status.equals("CANCELED")) {
            AlertUtil.warning("Thong bao", "Phien dau gia nay da ket thuc."); return;
        }
        try {
            BiddingController ctrl = SceneManager.switchToAndGetController("Bidding.fxml");
            ctrl.loadAuction(selected);
        } catch (Exception e) {
            AlertUtil.error("Loi", "Khong the mo man hinh: " + e.getMessage());
        }
    }

    @FXML
    public void handleCreateAuction() {
        try { SceneManager.switchTo("SellerDashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Loi", e.getMessage()); }
    }

    @FXML
    public void handleAdmin() {
        try { SceneManager.switchTo("AdminDashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Loi", e.getMessage()); }
    }

    @FXML
    public void handleLogout() {
        SessionManager.clear();
        try { SceneManager.switchTo("Login.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
