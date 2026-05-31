package com.auction.controller;

import com.auction.util.SceneManager;
import com.auction.util.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ItemDetailController {

    @FXML private Label nameLabel;
    @FXML private Label typeLabel;
    @FXML private Label detailLabel;
    @FXML private Label descLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label endTimeLabel;
    @FXML private Button joinBtn;

    private JsonObject auctionData;

    public void loadAuction(JsonObject auction) {
        this.auctionData = auction;
        
        nameLabel.setText(auction.get("itemName").getAsString());
        typeLabel.setText(auction.get("itemType").getAsString());
        
        JsonElement details = auction.get("itemDetails");
        detailLabel.setText(details != null && !details.isJsonNull() ? details.getAsString() : "Không có");
        
        JsonElement desc = auction.get("itemDescription");
        descLabel.setText(desc != null && !desc.isJsonNull() ? desc.getAsString() : "Không có");
        
        startPriceLabel.setText(String.format("%,.0f đ", auction.get("startingPrice").getAsDouble()));
        currentPriceLabel.setText(String.format("%,.0f đ", auction.get("currentPrice").getAsDouble()));
        
        sellerLabel.setText(auction.get("sellerId").getAsString());
        endTimeLabel.setText(auction.get("endTime").getAsString().replace("T", " "));
        
        String status = auction.get("status").getAsString();
        if (!status.equals("OPEN") && !status.equals("RUNNING")) {
            joinBtn.setText("PHIÊN ĐÃ KẾT THÚC");
            if (!"BIDDER".equals(SessionManager.getCurrentRole())) {
                joinBtn.setDisable(true);
            }
        }
        
        if (!"BIDDER".equals(SessionManager.getCurrentRole())) {
            joinBtn.setText("XEM PHIÊN ĐẤU GIÁ");
        }
    }

    @FXML
    public void handleJoinAuction() {
        try {
            BiddingController ctrl = SceneManager.switchToAndGetController("Bidding.fxml");
            ctrl.loadAuction(auctionData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBack() {
        try { SceneManager.switchTo("Dashboard.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
