package com.auction.service;

import com.auction.db.AuctionDAO;
import com.auction.db.BidTransactionDAO;
import com.auction.exception.AuctionNotFoundException;
import com.auction.model.Auction;
import com.auction.model.AutoBid;
import com.auction.model.BidTransaction;
import com.auction.model.Item;
import com.auction.network.observer.AuctionObserver;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service trung tâm quản lý toàn bộ nghiệp vụ đấu giá.
 * 
 * THỂ HIỆN CÁC DESIGN PATTERN:
 * 1. SINGLETON PATTERN: Đảm bảo chỉ có duy nhất 1 instance của AuctionService 
 *    được tạo ra và sử dụng chung trên toàn hệ thống (qua getInstance()).
 * 2. OBSERVER PATTERN: Đóng vai trò là "Subject", quản lý danh sách các Observer (ClientHandler).
 *    Khi có thay đổi (ví dụ: có người đặt giá), nó sẽ gọi notifyObservers() để báo cho tất cả.
 * 
 * THỂ HIỆN XỬ LÝ CONCURRENT BIDDING (Đồng thời):
 * - Sử dụng ReentrantLock (khóa) riêng cho từng phiên đấu giá. Đảm bảo tại một thời điểm
 *   chỉ có 1 thread được thao tác đặt giá trên 1 phiên, tránh lỗi "lost update" hay "race condition".
 * - Sử dụng ConcurrentHashMap và CopyOnWriteArrayList để đảm bảo an toàn luồng (thread-safe).
 */
public class AuctionService {

    private static AuctionService instance;

    private final AuctionDAO         auctionDAO          = new AuctionDAO();
    /* CHANGED: đổi tên bidTxDAO → bidTransactionDAO cho rõ nghĩa */
    private final BidTransactionDAO  bidTransactionDAO   = new BidTransactionDAO();
    /* CHANGED: đổi tên manager → auctionManager cho rõ nghĩa */
    private final AuctionManager     auctionManager      = AuctionManager.getInstance();
    /* CHANGED: HashMap → ConcurrentHashMap — fix thread-safety khi computeIfAbsent từ nhiều thread */
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final List<AuctionObserver> observers         = new CopyOnWriteArrayList<>();

    /* CHANGED: HashMap → ConcurrentHashMap — autoBidRegistry cũng được truy cập từ nhiều thread */
    private final Map<String, PriorityQueue<AutoBid>> autoBidRegistry = new ConcurrentHashMap<>();

    private AuctionService() {
        try {
            auctionDAO.findAll().forEach(a -> {
                auctionManager.addAuction(a);
                auctionLocks.put(a.getId(), new ReentrantLock());
            });
            System.out.println("Loaded " + auctionManager.getAllAuctions().size() + " auction(s) từ database.");
        } catch (SQLException e) {
            System.err.println("Lỗi load auction: " + e.getMessage());
        }
    }

    /**
     * THỂ HIỆN SINGLETON PATTERN: 
     * Khởi tạo lazy (chỉ tạo khi được gọi lần đầu) và thread-safe (nhờ từ khóa synchronized).
     */
    public static synchronized AuctionService getInstance() {
        if (instance == null) instance = new AuctionService();
        return instance;
    }

    // ======================== CORE API ========================

    /**
     * Tạo phiên đấu giá mới.
     *
     * @param item     sản phẩm cần đấu giá
     * @param sellerId ID người bán
     * @param endTime  thời điểm kết thúc dự kiến
     * @return phiên đấu giá vừa tạo
     * @throws SQLException nếu lỗi persist xuống DB
     */
    public Auction createAuction(Item item, String sellerId,
                                 LocalDateTime endTime) throws SQLException {
        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, sellerId, LocalDateTime.now(), endTime);
        auctionDAO.save(auction);
        auctionManager.addAuction(auction);
        auctionLocks.put(id, new ReentrantLock());
        notifyObservers(auction);
        return auction;
    }

    /**
     * Đặt giá cho phiên đấu giá. Xử lý atomic trong lock:
     * bid → persist → anti-sniping → notify → trigger auto-bid.
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  ID người đặt giá
     * @param amount    số tiền đặt
     * @return transaction đã tạo
     * @throws Exception nếu auction không tồn tại, đã đóng, hoặc giá không hợp lệ
     */
    public BidTransaction placeBid(String auctionId, String bidderId, double amount)
            throws Exception {
        Auction auction = findAuctionOrThrow(auctionId);

        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            BidTransaction tx = auction.placeBid(bidderId, amount);
            /* CHANGED: extract hàm trùng lặp → persistBidAndNotify() */
            persistBidAndNotify(auctionId, auction, tx);

            // Trigger auto-bid processing (vẫn trong lock → thread-safe)
            processAutoBids(auctionId, auction);

            return tx;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Trả về tất cả phiên đấu giá (bao gồm cả đã kết thúc).
     */
    public Collection<Auction> getAllAuctions() { return auctionManager.getAllAuctions(); }

    /**
     * Trả về các phiên đấu giá mà bidder đã tham gia đặt giá.
     */
    public Collection<Auction> getAuctionsByBidder(String bidderId) throws SQLException {
        return auctionDAO.findAuctionsByBidder(bidderId);
    }

    /**
     * Trả về các phiên đấu giá mà bidder đã thắng (FINISHED + leadingBidder = bidderId).
     */
    public Collection<Auction> getWonAuctions(String bidderId) throws SQLException {
        return auctionDAO.findWonAuctions(bidderId);
    }

    /**
     * Đóng phiên đấu giá và dọn dẹp auto-bid registry.
     *
     * @param auctionId ID phiên cần đóng
     * @throws SQLException nếu lỗi cập nhật DB
     */
    public void closeAuction(String auctionId) throws SQLException {
        auctionManager.getAuction(auctionId).ifPresent(auction -> {
            auction.close();
            try {
                auctionDAO.updateStatus(auctionId, auction.getStatus().name());
                notifyObservers(auction);
            } catch (SQLException e) {
                System.err.println("Lỗi close auction: " + e.getMessage());
            }
        });

        autoBidRegistry.remove(auctionId);
    }

    /**
     * Hủy phiên đấu giá (chỉ dùng cho Seller).
     */
    public void cancelAuction(String auctionId, String sellerId) throws Exception {
        Auction auction = findAuctionOrThrow(auctionId);
        if (!auction.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Chỉ người tạo mới có thể hủy phiên.");
        }
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            if (!auction.isActive()) {
                throw new IllegalStateException("Chỉ có thể hủy phiên đang hoạt động.");
            }
            auction.forceStatus(Auction.Status.CANCELED);
            auctionDAO.updateStatus(auctionId, Auction.Status.CANCELED.name());
            notifyObservers(auction);
        } finally {
            lock.unlock();
        }
        autoBidRegistry.remove(auctionId);
    }

    /**
     * Hủy phiên đấu giá (chỉ dùng cho Admin).
     */
    public void adminCancelAuction(String auctionId) throws Exception {
        Auction auction = findAuctionOrThrow(auctionId);
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            if (!auction.isActive()) {
                throw new IllegalStateException("Chỉ có thể hủy phiên đang hoạt động.");
            }
            auction.forceStatus(Auction.Status.CANCELED);
            auctionDAO.updateStatus(auctionId, Auction.Status.CANCELED.name());
            notifyObservers(auction);
        } finally {
            lock.unlock();
        }
        autoBidRegistry.remove(auctionId);
    }

    // ======================== AUTO-BID API ========================

    /**
     * Đăng ký auto-bid cho một bidder trên một phiên đấu giá.
     * Mỗi bidder chỉ có 1 auto-bid / phiên — đăng ký mới sẽ thay thế cái cũ.
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  ID người đặt auto-bid
     * @param maxBid    giá tối đa sẵn sàng trả
     * @param increment bước nhảy mỗi lần tự động đặt
     * @throws Exception nếu auction không tồn tại, đã kết thúc, hoặc bidder là seller
     */
    public void registerAutoBid(String auctionId, String bidderId,
                                double maxBid, double increment) throws Exception {
        Auction auction = findAuctionOrThrow(auctionId);

        if (!auction.isActive()) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc.");
        }
        if (bidderId.equals(auction.getSellerId())) {
            throw new IllegalArgumentException("Người bán không thể đặt auto-bid.");
        }
        if (maxBid <= 0 || increment <= 0) {
            throw new IllegalArgumentException("maxBid và increment phải lớn hơn 0.");
        }
        if (maxBid <= auction.getCurrentPrice()) {
            throw new IllegalArgumentException("maxBid phải lớn hơn giá hiện tại ("
                    + auction.getCurrentPrice() + ").");
        }

        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            PriorityQueue<AutoBid> queue = autoBidRegistry
                    .computeIfAbsent(auctionId, k -> new PriorityQueue<>());

            queue.removeIf(ab -> ab.getBidderId().equals(bidderId));

            AutoBid autoBid = new AutoBid(bidderId, auctionId, maxBid, increment);
            queue.add(autoBid);
            System.out.println("AutoBid registered: " + autoBid);

            processAutoBids(auctionId, auction);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Hủy auto-bid của một bidder cho phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  ID bidder cần hủy auto-bid
     * @throws Exception nếu auction không tồn tại
     */
    public void cancelAutoBid(String auctionId, String bidderId) throws Exception {
        findAuctionOrThrow(auctionId);

        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            PriorityQueue<AutoBid> queue = autoBidRegistry.get(auctionId);
            if (queue != null) {
                /* CHANGED: đơn giản hóa — removeIf đủ, không cần stream→forEach rồi removeIf */
                queue.removeIf(ab -> ab.getBidderId().equals(bidderId));
            }
            System.out.println("AutoBid cancelled: bidder=" + bidderId + ", auction=" + auctionId);
        } finally {
            lock.unlock();
        }
    }

    // ======================== OBSERVER ========================

    /* CHANGED: giữ public — ClientHandler (com.auction.network) cần truy cập cross-package */
    public void addObserver(AuctionObserver o)    { observers.add(o); }
    public void removeObserver(AuctionObserver o) { observers.remove(o); }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Tìm auction hoặc throw — extract pattern lặp lại ở placeBid, registerAutoBid, cancelAutoBid.
     */
    /* CHANGED: extract duplicated lookup pattern → findAuctionOrThrow() */
    private Auction findAuctionOrThrow(String auctionId) throws AuctionNotFoundException {
        return auctionManager.getAuction(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));
    }

    /**
     * Lấy lock cho auction — extract pattern computeIfAbsent lặp lại.
     */
    /* CHANGED: extract duplicated lock-acquire pattern → getLock() */
    private ReentrantLock getLock(String auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    /**
     * Persist bid + apply anti-sniping + notify observers.
     * PHẢI được gọi bên trong lock block.
     */
    /* CHANGED: extract duplicated persist+sniping+notify pattern (dùng ở placeBid và processAutoBids) */
    private void persistBidAndNotify(String auctionId, Auction auction,
                                     BidTransaction tx) throws SQLException {
        auctionDAO.updateBid(auctionId, tx.getAmount(),
                tx.getBidderId(), auction.getStatus().name());
        bidTransactionDAO.save(tx);
        applyAntiSniping(auctionId, auction);
        notifyObservers(auction);
    }

    /**
     * Xử lý vòng lặp auto-bid cho phiên đấu giá.
     * PHẢI được gọi bên trong lock block.
     * Duyệt các auto-bid theo thứ tự registeredAt (FIFO):
     * - Bỏ qua nếu bidder đang dẫn đầu (không tự bid lên chính mình)
     * - Tính nextBid = currentPrice + increment
     * - Nếu nextBid > maxBid → deactivate auto-bid
     * - Nếu hợp lệ → đặt bid, persist, notify, restart vòng lặp
     */
    private void processAutoBids(String auctionId, Auction auction) {
        PriorityQueue<AutoBid> queue = autoBidRegistry.get(auctionId);
        if (queue == null || queue.isEmpty()) return;
        if (!auction.isActive()) return;

        boolean bidPlaced = true;
        while (bidPlaced) {
            bidPlaced = false;

            List<AutoBid> candidates = new ArrayList<>(queue);
            Collections.sort(candidates);

            for (AutoBid ab : candidates) {
                if (!ab.isActive()) continue;
                if (ab.getBidderId().equals(auction.getLeadingBidderId())) continue;

                double nextBid = auction.getCurrentPrice() + ab.getIncrement();

                if (nextBid > ab.getMaxBid()) {
                    ab.setActive(false);
                    queue.remove(ab);
                    System.out.println("AutoBid deactivated (exceeded max): " + ab);
                    continue;
                }

                try {
                    BidTransaction tx = auction.placeBid(ab.getBidderId(), nextBid);
                    /* CHANGED: gọi persistBidAndNotify thay vì code trùng lặp */
                    persistBidAndNotify(auctionId, auction, tx);
                    System.out.println("AutoBid executed: " + ab.getBidderId()
                            + " bid " + nextBid + " on auction " + auctionId);
                    bidPlaced = true;
                    break;
                } catch (Exception e) {
                    System.err.println("AutoBid error: " + e.getMessage());
                    ab.setActive(false);
                    queue.remove(ab);
                }
            }
        }

        queue.removeIf(ab -> !ab.isActive());
    }

    /**
     * Kiểm tra và áp dụng anti-sniping cho phiên đấu giá.
     * PHẢI được gọi bên trong lock block.
     */
    private void applyAntiSniping(String auctionId, Auction auction) {
        if (auction.checkAndExtendEndTime()) {
            try {
                auctionDAO.updateEndTime(auctionId, auction.getEndTime());
            } catch (SQLException e) {
                System.err.println("Lỗi cập nhật endTime (anti-sniping): " + e.getMessage());
            }
        }
    }

    private void notifyObservers(Auction auction) {
        observers.forEach(o -> o.onAuctionUpdated(auction));
    }
}

