package com.auction.controller;

import com.auction.network.*;
import com.auction.util.*;
import com.google.gson.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private TabPane mainTabPane;
    @FXML private Tab wonTab;

    // Active auctions table
    @FXML private TableView<JsonObject>           auctionTable;
    @FXML private TableColumn<JsonObject, String> colName;
    @FXML private TableColumn<JsonObject, String> colType;
    @FXML private TableColumn<JsonObject, String> colPrice;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colEnd;
    @FXML private TableColumn<JsonObject, String> colCountdown;

    // Won auctions table
    @FXML private TableView<JsonObject>           wonTable;
    @FXML private TableColumn<JsonObject, String> colWonName;
    @FXML private TableColumn<JsonObject, String> colWonType;
    @FXML private TableColumn<JsonObject, String> colWonPrice;
    @FXML private TableColumn<JsonObject, String> colWonEnd;
    @FXML private TableColumn<JsonObject, String> colWonDesc;
    @FXML private TableColumn<JsonObject, String> colWonStatus;
    @FXML private TableColumn<JsonObject, Void> colWonAction;

    @FXML private Button createAuctionBtn;
    @FXML private Button adminBtn;
    @FXML private CheckBox filterMyBidsCheck;

    private final AuctionClient client = AuctionClient.getInstance();
    private final ObservableList<JsonObject> auctionList = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> wonList     = FXCollections.observableArrayList();

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
    private Timeline clockTimeline;
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername()
                + " (" + SessionManager.getCurrentRole() + ")");

        String role = SessionManager.getCurrentRole();
        createAuctionBtn.setVisible("SELLER".equals(role));
        if (adminBtn != null) adminBtn.setVisible("ADMIN".equals(role));
        if (filterMyBidsCheck != null) {
            boolean isBidder = "BIDDER".equals(role);
            filterMyBidsCheck.setVisible(isBidder);
            filterMyBidsCheck.setManaged(isBidder);
        }

        // Hide won tab for non-bidders
        if (wonTab != null && !"BIDDER".equals(role)) {
            mainTabPane.getTabs().remove(wonTab);
        }

        // Active auctions columns
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
        // Countdown calculated live via cell factory
        colCountdown.setCellValueFactory(d -> {
            try {
                LocalDateTime endT = LocalDateTime.parse(d.getValue().get("endTime").getAsString());
                long sRemain = ChronoUnit.SECONDS.between(LocalDateTime.now(), endT);
                if (sRemain <= 0) return new SimpleStringProperty("Hết giờ");
                return new SimpleStringProperty(String.format("%02d:%02d:%02d",
                        sRemain / 3600, (sRemain % 3600) / 60, sRemain % 60));
            } catch (Exception ex) {
                return new SimpleStringProperty("--");
            }
        });

        auctionTable.setItems(auctionList);

        // Won auctions columns
        if (wonTable != null) {
            colWonName.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().get("itemName").getAsString()));
            colWonType.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().get("itemType").getAsString()));
            colWonPrice.setCellValueFactory(d ->
                    new SimpleStringProperty(String.format("%,.0f đ",
                            d.getValue().get("currentPrice").getAsDouble())));
            colWonEnd.setCellValueFactory(d ->
                    new SimpleStringProperty(
                            d.getValue().get("endTime").getAsString().replace("T", " ")));
            colWonDesc.setCellValueFactory(d -> {
                JsonElement desc = d.getValue().get("itemDescription");
                return new SimpleStringProperty(desc == null || desc.isJsonNull() ? "" : desc.getAsString());
            });
            colWonStatus.setCellValueFactory(d -> {
                String status = d.getValue().get("status").getAsString();
                return new SimpleStringProperty("PAID".equals(status) ? "Đã thanh toán" : "Chưa thanh toán");
            });
            
            colWonAction.setCellFactory(param -> new TableCell<JsonObject, Void>() {
                private final Button payBtn = new Button("Thanh toán");
                {
                    payBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                    payBtn.setOnAction(event -> {
                        JsonObject auction = getTableView().getItems().get(getIndex());
                        handlePayWonAuction(auction);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        JsonObject auction = getTableView().getItems().get(getIndex());
                        if ("PAID".equals(auction.get("status").getAsString())) {
                            setGraphic(null); // Hide button if already paid
                        } else {
                            setGraphic(payBtn);
                        }
                    }
                }
            });
            
            wonTable.setItems(wonList);
        }

        // ★ Đăng ký PUSH listener để nhận cập nhật realtime từ Server
        client.setPushListener(this::handlePush);

        // Start real-time clock
        startClock();
        // Start countdown refresh (mỗi giây refresh bảng + tự xóa phiên hết giờ)
        startCountdownRefresh();

        handleRefresh();
    }

    /**
     * ★ Xử lý PUSH từ server khi phiên đấu giá thay đổi.
     * - Nếu phiên FINISHED/CANCELED → xóa khỏi bảng + hiện popup thông báo
     * - Nếu phiên vẫn active → cập nhật giá/trạng thái trên bảng
     */
    private void handlePush(String jsonData) {
        try {
            JsonObject updated = JsonParser.parseString(jsonData).getAsJsonObject();
            String auctionId = updated.get("id").getAsString();
            String status = updated.get("status").getAsString();

            Platform.runLater(() -> {
                if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
                    // ★ Xóa phiên khỏi danh sách đang diễn ra
                    auctionList.removeIf(o -> o.get("id").getAsString().equals(auctionId));

                    // ★ Hiện thông báo popup cho người dùng
                    if ("FINISHED".equals(status)) {
                        String itemName = updated.get("itemName").getAsString();
                        JsonElement leader = updated.get("leadingBidder");
                        String winnerId = (leader == null || leader.isJsonNull()) ? null : leader.getAsString();
                        String price = String.format("%,.0f đ", updated.get("currentPrice").getAsDouble());

                        if (winnerId != null && winnerId.equals(SessionManager.getCurrentUserId())) {
                            AlertUtil.info("🎉 CHÚC MỪNG!",
                                    "Bạn đã THẮNG phiên đấu giá \"" + itemName + "\" với giá " + price + "!\n"
                                    + "Xem chi tiết trong tab \"Đã Thắng\".");
                            // Refresh won list
                            loadWonAuctions();
                        } else {
                            String winner = (winnerId != null) ? winnerId : "Không có";
                            AlertUtil.info("Phiên kết thúc",
                                    "Phiên \"" + itemName + "\" đã kết thúc.\n"
                                    + "Người thắng: " + winner + "\nGiá cuối: " + price);
                        }
                    } else {
                        String itemName = updated.get("itemName").getAsString();
                        AlertUtil.warning("Phiên bị hủy",
                                "Phiên \"" + itemName + "\" đã bị hủy bởi người bán.");
                    }
                } else {
                    // Cập nhật phiên vẫn active trên bảng (giá mới, người dẫn đầu mới)
                    for (int i = 0; i < auctionList.size(); i++) {
                        if (auctionList.get(i).get("id").getAsString().equals(auctionId)) {
                            auctionList.set(i, updated);
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Dashboard PUSH error: " + e.getMessage());
        }
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText("🕐 " + LocalDateTime.now().format(CLOCK_FMT))
        ));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void startCountdownRefresh() {
        // Mỗi giây: refresh đếm ngược + tự động xóa phiên đã hết giờ
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            auctionTable.refresh(); // force re-calculate countdown cells
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    @FXML
    public void handleRefresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CommandType cmd = (filterMyBidsCheck != null && filterMyBidsCheck.isSelected()) 
                                    ? CommandType.GET_MY_BIDS 
                                    : CommandType.GET_AUCTIONS;
                Request  req = new Request(cmd, null);
                Response res = client.send(req);
                if (res.isOk()) {
                    JsonArray arr = JsonParser.parseString(res.getData()).getAsJsonArray();
                    Platform.runLater(() -> {
                        auctionList.clear();
                        arr.forEach(e -> auctionList.add(e.getAsJsonObject()));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", "Không thể tải danh sách: " + e.getMessage()));
            }
        });

        // Also load won auctions for bidders
        loadWonAuctions();
    }

    private void loadWonAuctions() {
        if ("BIDDER".equals(SessionManager.getCurrentRole())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Request  req = new Request(CommandType.GET_WON_AUCTIONS, null);
                    Response res = client.send(req);
                    if (res.isOk()) {
                        JsonArray arr = JsonParser.parseString(res.getData()).getAsJsonArray();
                        Platform.runLater(() -> {
                            wonList.clear();
                            arr.forEach(e -> wonList.add(e.getAsJsonObject()));
                        });
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    @FXML
    public void handleJoinAuction() {
        JsonObject selected = null;
        if (wonTab != null && mainTabPane.getSelectionModel().getSelectedItem() == wonTab) {
            selected = wonTable.getSelectionModel().getSelectedItem();
        } else {
            selected = auctionTable.getSelectionModel().getSelectedItem();
        }
        
        if (selected == null) { AlertUtil.warning("Thông báo", "Vui lòng chọn một phiên đấu giá."); return; }
        
        try {
            stopTimelines();
            client.setPushListener(null); // clear push listener trước khi chuyển màn
            ItemDetailController ctrl = SceneManager.switchToAndGetController("ItemDetail.fxml");
            ctrl.loadAuction(selected);
        } catch (Exception e) {
            AlertUtil.error("Lỗi", "Không thể mở màn hình: " + e.getMessage());
        }
    }

    private void handlePayWonAuction(JsonObject auction) {
        String auctionId = auction.get("id").getAsString();
        JsonObject reqData = new JsonObject();
        reqData.addProperty("auctionId", auctionId);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Request req = new Request(CommandType.PAY_AUCTION, reqData.toString());
                Response res = client.send(req);
                Platform.runLater(() -> {
                    if (res.isOk()) {
                        AlertUtil.info("Thành công", "Đã thanh toán thành công!");
                        loadWonAuctions(); // Refresh the list
                    } else {
                        AlertUtil.error("Lỗi thanh toán", res.getData());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.error("Lỗi", "Không thể thanh toán: " + e.getMessage()));
            }
        });
    }

    @FXML
    public void handleCreateAuction() {
        try { stopTimelines(); client.setPushListener(null); SceneManager.switchTo("SellerDashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }

    @FXML
    public void handleAdmin() {
        try { stopTimelines(); client.setPushListener(null); SceneManager.switchTo("AdminDashboard.fxml"); }
        catch (Exception e) { AlertUtil.error("Lỗi", e.getMessage()); }
    }

    @FXML
    public void handleLogout() {
        SessionManager.clear();
        try { stopTimelines(); client.setPushListener(null); SceneManager.switchTo("Login.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void stopTimelines() {
        if (clockTimeline != null) clockTimeline.stop();
        if (countdownTimeline != null) countdownTimeline.stop();
    }
}
