# 🏷️ Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

> **Bài tập lớn môn Lập trình Nâng cao — Nhóm 36**

Hệ thống đấu giá trực tuyến Client-Server sử dụng Java, JavaFX, Socket và SQLite. Hỗ trợ đấu giá thời gian thực, đặt giá tự động (Auto-Bid), chống sniping, và quản trị hệ thống.

---

## 📖 Mô tả bài toán và phạm vi hệ thống

**Bài toán:** Nhu cầu mua bán và đấu giá các mặt hàng trực tuyến cần một hệ thống có khả năng xử lý thời gian thực, đảm bảo tính công bằng, minh bạch và có thể phục vụ nhiều người dùng tham gia đặt giá cùng lúc.

**Phạm vi hệ thống:**
- Xây dựng hệ thống Client-Server đa luồng (multi-threading) hỗ trợ nhiều kết nối TCP đồng thời.
- Phục vụ 3 đối tượng người dùng chính: Quản trị viên (Admin), Người bán (Seller), Người mua (Bidder).
- Quản lý toàn bộ vòng đời của phiên đấu giá: từ lúc tạo, đang diễn ra (nhận giá realtime), cho đến khi kết thúc (xác định người thắng).
- Tích hợp các cơ chế nâng cao: Đặt giá tự động (Auto-Bid), chống đặt giá giây cuối (Anti-Sniping).

---

## 📋 Mục Lục

- [Mô tả bài toán và phạm vi](#-mô-tả-bài-toán-và-phạm-vi-hệ-thống)
- [Danh sách chức năng đã hoàn thành](#-danh-sách-chức-năng-đã-hoàn-thành)
- [Kiến trúc & Design Pattern](#-kiến-trúc--design-pattern)
- [Công nghệ](#-công-nghệ)
- [Cài đặt & Chạy](#-cài-đặt--chạy)
- [Tài khoản mẫu](#-tài-khoản-mẫu)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Unit Test](#-unit-test)

---

## ✨ Danh Sách Chức Năng Đã Hoàn Thành

### Người mua (Bidder)
- 🔍 Xem danh sách phiên đấu giá đang diễn ra (realtime)
- 💰 Đặt giá thủ công cho phiên đấu giá
- ⚡ **Auto-Bid**: Đăng ký đặt giá tự động với giá tối đa và bước nhảy
- 📊 **Biểu đồ giá realtime**: Line chart cập nhật liên tục khi có bid mới
- ⏱️ **Đồng hồ đếm ngược**: Hiển thị thời gian còn lại cho mỗi phiên
- 🏆 **Thông báo chiến thắng**: Popup khi thắng đấu giá
- 📦 **Tab "Đã Thắng"**: Xem lại vật phẩm đã trúng đấu giá

### Người bán (Seller)
- ➕ Tạo phiên đấu giá mới (DatePicker + Spinner chọn giờ)
- 📋 Quản lý danh sách phiên của mình
- ❌ Hủy phiên đấu giá đang mở
- 🔔 Nhận thông báo realtime khi có người đặt giá

### Quản trị viên (Admin)
- 👥 **Quản lý User**: Xem danh sách, Khóa/Mở khóa tài khoản, Xóa user vi phạm (Cascade Delete).
- 📋 **Quản lý Phiên đấu giá**: Xem tất cả phiên, đóng sớm (Force Finish) để chọn ngay người thắng, hoặc Hủy phiên (Cancel) nếu có gian lận.
- 📈 **Giám sát hệ thống**: Xem toàn bộ log giao dịch (Bid History), thống kê số lượng phiên và user theo trạng thái.

### Hệ thống
- 🔐 Đăng ký / Đăng nhập (3 role: Bidder, Seller, Admin)
- 🛡️ **Anti-Sniping**: Tự động gia hạn 60s khi có bid gần kết thúc
- 🔄 **Realtime Update**: Push notification qua Observer Pattern
- 🔒 **Concurrent Bidding**: Thread-safe với ReentrantLock per-auction
- 📉 **Bid History Visualization**: Biểu đồ đường giá với lịch sử đầy đủ

---

## 🏗️ Kiến Trúc & Design Pattern

### 1. Singleton Pattern
- `AuctionService.getInstance()` — Service trung tâm duy nhất
- `AuctionManager.getInstance()` — Quản lý in-memory auctions  
- `DatabaseManager.getInstance()` — Kết nối DB duy nhất
- `AuctionClient.getInstance()` — Client socket duy nhất

### 2. Observer Pattern
- **Subject**: `AuctionService` quản lý danh sách Observer
- **Observer**: `AuctionObserver` interface, `ClientHandler` implements
- **Flow**: Khi có bid mới → `notifyObservers()` → tất cả client nhận PUSH → UI tự cập nhật

### 3. Factory Pattern
- `ItemFactory.create(type, name, desc, price)` — Tạo đối tượng `Item` theo loại (Electronics, Art, Vehicle)

### 4. Polymorphism (Đa hình)
- Lớp trừu tượng `Item` có method `getType()` và `getDetails()` 
- Các lớp con override: `Electronics` (bảo hành), `Art` (tác giả), `Vehicle` (ODO)
- Lớp trừu tượng `User` với các con `Admin`, `Bidder`, `Seller`

### 5. Concurrent Bidding
- `ReentrantLock` riêng cho từng phiên đấu giá
- `ConcurrentHashMap` cho auction registry và lock registry
- `CopyOnWriteArrayList` cho observer list

```
┌──────────┐     Socket      ┌──────────────┐
│  Client  │ ◄──────────────► │ ClientHandler│
│ (JavaFX) │   JSON Request   │  (Observer)  │
└──────────┘   /Response/Push  └──────┬───────┘
                                      │
                               ┌──────▼───────┐
                               │AuctionService │ ← Singleton + Subject
                               │  (Lock/Bid)   │
                               └──────┬───────┘
                                      │
                          ┌───────────┼───────────┐
                          ▼           ▼           ▼
                   ┌──────────┐ ┌──────────┐ ┌────────┐
                   │AuctionDAO│ │BidTxDAO  │ │UserDAO │
                   └────┬─────┘ └────┬─────┘ └───┬────┘
                        └────────────┼───────────┘
                                     ▼
                              ┌──────────┐
                              │  SQLite   │
                              │auction.db │
                              └──────────┘
```

---

## 🛠️ Công Nghệ

| Thành phần | Công nghệ |
|-----------|-----------|
| Ngôn ngữ | Java 17 |
| GUI | JavaFX 17 (FXML + CSS) |
| Database | SQLite 3 (via JDBC) |
| Networking | Java Socket (TCP) |
| Serialization | Gson (JSON) |
| Build | Maven |
| Testing | JUnit 5 |
| CI/CD | GitHub Actions |

---

## 🚀 Cài Đặt & Chạy

> **Lưu ý:** Dự án sử dụng Maven, nên các câu lệnh dòng lệnh dưới đây **chạy được trên mọi hệ điều hành (Windows, Linux, macOS)**. Không yêu cầu cấu hình đặc biệt cho từng OS.

### Yêu cầu
- **JDK 17** trở lên
- **Maven 3.8+**

### Bước 1: Clone dự án
```bash
git clone https://github.com/kiet2711/nhom-36.git
cd nhom-36
```

### Bước 2: Build
```bash
mvn clean compile
```

### Bước 3: Chạy Server (Bắt buộc chạy trước)
```bash
# Cách 1: Chạy trực tiếp từ IDE (IntelliJ/Eclipse)
# → Run file: com.auction.network.AuctionServer

# Cách 2: Chạy bằng Maven
mvn exec:java "-Dexec.mainClass=com.auction.network.AuctionServer"
```

### Bước 4: Chạy Client (Chạy sau khi Server đã khởi động)
```bash
# Mở terminal mới
# → Run file: com.auction.ClientApp

# Hoặc:
mvn javafx:run
```

> **Lưu ý**: Có thể mở nhiều Client cùng lúc (mở nhiều terminal và chạy lại lệnh ở Bước 4) để test tính năng đấu giá giữa nhiều người dùng.

---

## 👤 Tài Khoản Mẫu

Hệ thống tự động tạo tài khoản và dữ liệu mẫu khi khởi động lần đầu:

| Username | Password | Vai trò | Mô tả |
|----------|----------|---------|-------|
| `admin` | `admin123` | Admin | Quản trị hệ thống |
| `seller1` | `seller123` | Seller | Người bán (tạo sẵn 7 phiên đấu giá) |
| `bidder1` | `bidder123` | Bidder | Người mua #1 |
| `bidder2` | `bidder123` | Bidder | Người mua #2 |

### Sản phẩm đấu giá mẫu (7 phiên)
| Sản phẩm | Loại | Giá khởi điểm |
|----------|------|---------------|
| iPhone 15 Pro Max | Electronics | 25,000,000 đ |
| MacBook Pro M3 14 inch | Electronics | 35,000,000 đ |
| Sony WH-1000XM5 | Electronics | 5,000,000 đ |
| Tranh Hoa Hướng Dương | Art | 8,000,000 đ |
| Tượng Phật Bà Quan Âm | Art | 15,000,000 đ |
| Mercedes S450 2023 | Vehicle | 2,000,000,000 đ |
| Honda SH 150i | Vehicle | 80,000,000 đ |

---

## 📁 Cấu Trúc Dự Án

```
src/main/java/com/auction/
├── ClientApp.java                    # Entry point cho Client (JavaFX)
├── controller/                       # JavaFX Controllers (MVC)
│   ├── LoginController.java
│   ├── RegisterController.java
│   ├── DashboardController.java      # Màn hình chính (Bidder/Seller)
│   ├── BiddingController.java        # Màn hình đấu giá + Chart
│   ├── SellerDashboardController.java
│   ├── CreateAuctionController.java
│   └── AdminDashboardController.java
├── model/                            # Domain models + Polymorphism
│   ├── Entity.java                   # Base class (Serializable)
│   ├── User.java (abstract)          # → Admin, Bidder, Seller
│   ├── Item.java (abstract)          # → Electronics, Art, Vehicle
│   ├── Auction.java                  # Phiên đấu giá + Anti-sniping
│   ├── AutoBid.java                  # Lệnh đặt giá tự động
│   └── BidTransaction.java          # Giao dịch đặt giá
├── service/                          # Business logic
│   ├── AuctionService.java           # ★ Singleton + Observer Subject
│   ├── AuctionManager.java           # In-memory auction registry
│   └── AuctionScheduler.java         # Tự động đóng phiên hết hạn
├── network/                          # Client-Server networking
│   ├── AuctionServer.java            # TCP Server (port 9999)
│   ├── AuctionClient.java            # TCP Client (Singleton)
│   ├── ClientHandler.java            # ★ Observer + Request dispatcher
│   ├── Request.java / Response.java  # JSON protocol
│   ├── CommandType.java              # Enum các lệnh
│   └── observer/
│       └── AuctionObserver.java      # ★ Observer interface
├── db/                               # Data Access Layer
│   ├── DatabaseManager.java          # SQLite connection + Seed data
│   ├── AuctionDAO.java
│   ├── BidTransactionDAO.java
│   └── UserDAO.java
├── factory/
│   └── ItemFactory.java              # ★ Factory Pattern
├── exception/                        # Custom exceptions
│   ├── AuctionClosedException.java
│   ├── AuctionNotFoundException.java
│   ├── InvalidBidException.java
│   └── UnauthorizedException.java
└── util/
    ├── SceneManager.java             # Chuyển màn hình JavaFX
    ├── SessionManager.java           # Lưu thông tin user đăng nhập
    └── AlertUtil.java                # Hiển thị dialog

src/main/resources/
├── schema.sql                        # DDL tạo bảng SQLite
└── com.auction/
    ├── style.css                     # CSS cho JavaFX
    ├── Login.fxml
    ├── Register.fxml
    ├── Dashboard.fxml
    ├── Bidding.fxml
    ├── SellerDashboard.fxml
    ├── CreateAuction.fxml
    └── AdminDashboard.fxml

src/test/java/com/auction/
├── model/AuctionTest.java            # Test Auction logic + Anti-sniping
├── service/AuctionServiceConcurrentTest.java  # Test Concurrent Bidding
├── factory/ItemFactoryTest.java      # Test Factory Pattern
└── exception/ExceptionTest.java      # Test Custom Exceptions
```

---

## 🧪 Unit Test

Chạy toàn bộ test:
```bash
mvn test
```

**22 test cases** bao gồm:

| Test Class | Số test | Mô tả |
|-----------|---------|-------|
| `AuctionTest` | 10 | Đặt giá hợp lệ/không hợp lệ, đóng phiên, anti-sniping |
| `AuctionServiceConcurrentTest` | 2 | Đấu giá đồng thời từ nhiều thread |
| `ItemFactoryTest` | 6 | Tạo item đúng loại, xử lý loại không hợp lệ |
| `ExceptionTest` | 4 | Custom exception messages |

---

## 👥 Thành Viên Nhóm 36

| STT | Họ và Tên | Phụ trách |
|-----|-----------|-----------|
| 1 | Lữ Thanh Phúc | Server + DB |
| 2 | Nguyễn Gia Minh | Client UI |
| 3 | Đỗ Tuấn Kiệt | Model + Logic |
| 4 | Vũ Minh Quang | Network + Test |
 

Link video test và file pdf: https://drive.google.com/drive/folders/16km9XnR2N1Dpz_WD9FNCYyoEh6-TYMxc?usp=sharing

