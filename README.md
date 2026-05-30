# 🏷️ Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

> **Bài tập lớn môn Lập trình Nâng cao — Nhóm 36**

Hệ thống đấu giá trực tuyến Client-Server sử dụng Java, JavaFX, Socket và SQLite. Hỗ trợ đấu giá thời gian thực, đặt giá tự động (Auto-Bid), chống sniping, và quản trị hệ thống.

---

## 📋 Mục Lục

- [Tính năng](#-tính-năng)
- [Kiến trúc & Design Pattern](#-kiến-trúc--design-pattern)
- [Công nghệ](#-công-nghệ)
- [Cài đặt & Chạy](#-cài-đặt--chạy)
- [Tài khoản mẫu](#-tài-khoản-mẫu)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Unit Test](#-unit-test)

---

## ✨ Tính Năng

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
- 📊 Xem thống kê toàn bộ hệ thống
- 🔒 Đóng phiên đấu giá bất kỳ
- 📋 Quản lý tất cả phiên đấu giá

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

### Bước 3: Chạy Server
```bash
# Cách 1: Chạy trực tiếp từ IDE (IntelliJ/Eclipse)
# → Run file: com.auction.network.AuctionServer

# Cách 2: Chạy bằng Maven
mvn exec:java -Dexec.mainClass="com.auction.network.AuctionServer"
```

### Bước 4: Chạy Client
```bash
# Mở terminal mới
# → Run file: com.auction.ClientApp

# Hoặc:
mvn javafx:run
```

> **Lưu ý**: Chạy Server trước, sau đó mới chạy Client. Có thể mở nhiều Client cùng lúc để test đấu giá.

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

## 📝 Coding Convention

- **Google Java Style Guide**
- JavaDoc cho các class và method quan trọng
- Comment giải thích Design Pattern bằng tiếng Việt
- Package theo chức năng (model, service, network, db, controller)

---

## 👥 Thành Viên Nhóm 36

| STT | Họ và Tên | MSSV | Vai trò |
|-----|-----------|------|---------|
| 1 | | | |
| 2 | | | |
| 3 | | | |

---

## 📄 License

Dự án này được phát triển phục vụ mục đích học tập tại trường đại học.
