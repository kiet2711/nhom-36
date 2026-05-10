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
import java.util.concurrent.atomic.AtomicLong;

public class AdminDashboardController {

    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colAuctionId;
    @FXML private TableColumn<JsonObject, String> colAuctionName;
    @FXML private TableColumn<JsonObject, String> colAuctionPrice;
    @FXML private TableColumn<JsonObject, String> colAuctionStatus;
    @FXML private TableColumn<JsonObject, String> colAuctionSeller;
    @FXML private TableColumn<JsonObject, String> colAuctionEnd;
    @FXML private Label statTotal;
    @FXML private Label statRunning;
    @FXML private Label statFinished;
    @FXML private Label statCanceled;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colAuctionId.setCellValueFactory(d -> {
            String id = d.getValue().get("id").getAsString();
            return new SimpleStringProperty(id.substring(0, Math.min(8, id.length())) + "...");
        });
        colAuctionName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colAuctionPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f d", d.getValue().get("currentPrice").getAsDouble())));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colAuctionSeller.setCellValueFactory(d -> {
            String sid = d.getValue().get("sellerId").getAsString();
            return new SimpleStringProperty(sid.substring(0, Math.min(8, sid.length())) + "...");
        });
        colAuctionEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("endTime").getAsString().replace("T", " ")));
        auctionTable.setItems(list);
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request  req = new Request(CommandType.GET_AUCTIONS, null);
                Response res = client.send(req);
                if (!res.isOk()) return;
                JsonArray all = JsonParser.parseString(res.getData()).getAsJsonArray();
                AtomicLong running = new AtomicLong(), finished = new AtomicLong(), canceled = new AtomicLong();
                Platform.runLater(() -> {
                    list.clear();
                    all.forEach(e -> {
                        JsonObject o = e.getAsJsonObject();
                        list.add(o);
                        switch (o.get("status").getAsString()) {
                            case "RUNNING"  -> running.incrementAndGet();
                            case "FINISHED" -> finished.incrementAndGet();
                            case "CANCELED" -> canceled.incrementAndGet();
                        }
                    });
                    statTotal.setText("Tong so phien: " + list.size());
                    statRunning.setText("Dang chay (RUNNING): " + running.get());
                    statFinished.setText("Da ket thuc (FINISHED): " + finished.get());
                    statCanceled.setText("Da huy (CANCELED): " + canceled.get());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Loi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleForceClose() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thong bao", "Vui long chon phien can dong."); return; }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Request  req = new Request(CommandType.CANCEL_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thanh cong", "Da dong phien."); handleRefresh(); }
                    else AlertUtil.error("Loi", res.getData());
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Loi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Loi", e.getMessage()); }
    }
}
