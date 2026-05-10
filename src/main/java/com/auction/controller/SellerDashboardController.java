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

public class SellerDashboardController {

    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colType;
    @FXML private TableColumn<JsonObject, String> colPrice;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colLeader;
    @FXML private TableColumn<JsonObject, String> colEnd;
    @FXML private Label                           statusLabel;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f d", d.getValue().get("currentPrice").getAsDouble())));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
        colLeader.setCellValueFactory(d -> {
            JsonElement leader = d.getValue().get("leadingBidder");
            return new SimpleStringProperty(leader == null || leader.isJsonNull() ? "Chua co" : leader.getAsString());
        });
        colEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("endTime").getAsString().replace("T", " ")));
        auctionTable.setItems(list);
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request  req     = new Request(CommandType.GET_AUCTIONS, null);
                Response res     = client.send(req);
                if (!res.isOk()) return;
                String sellerId  = SessionManager.getCurrentUserId();
                JsonArray all    = JsonParser.parseString(res.getData()).getAsJsonArray();
                Platform.runLater(() -> {
                    list.clear();
                    all.forEach(e -> {
                        JsonObject o = e.getAsJsonObject();
                        if (sellerId.equals(o.get("sellerId").getAsString())) list.add(o);
                    });
                    statusLabel.setText("Tong: " + list.size() + " phien");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Loi", e.getMessage()));
            }
        });
    }

    @FXML
    public void handleCreate() {
        try { SceneManager.switchTo("CreateAuction.fxml"); }
        catch (Exception e) { AlertUtil.error("Loi", e.getMessage()); }
    }

    @FXML
    public void handleCancel() {
        JsonObject selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.warning("Thong bao", "Vui long chon phien can huy."); return; }
        String status = selected.get("status").getAsString();
        if (!status.equals("OPEN") && !status.equals("RUNNING")) {
            AlertUtil.warning("Thong bao", "Chi co the huy phien dang mo."); return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("auctionId", selected.get("id").getAsString());
                Request  req = new Request(CommandType.CANCEL_AUCTION, payload.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) { AlertUtil.info("Thanh cong", "Da huy phien."); handleRefresh(); }
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
