# 📚 線上書店管理系統

> 一個功能完整的全端書店電商平台，支援會員管理、購物車、訂單處理與管理員權限控制

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot)
![Vue.js](https://img.shields.io/badge/Vue.js-3.5-4FC08D?logo=vue.js)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-336791?logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?logo=jsonwebtokens)

---

## 📋 專案簡介

這是一個採用前後端分離架構的線上書店系統，後端使用 Spring Boot 提供 RESTful API，前端採用 Vue 3 建構現代化的單頁應用。系統具備完整的電商功能，包含使用者認證、商品管理、購物車、訂單流程等核心模組。

### ✨ 主要功能

- 🔐 **使用者認證系統**：註冊、登入、個人資料管理（基於 JWT）
- 📖 **書籍管理**：瀏覽書籍列表、查看詳情、搜尋功能
- 🛒 **購物車功能**：加入購物車、修改數量、結帳流程
- 📦 **訂單管理**：建立訂單、付款、取消、確認收貨
- 👨‍💼 **管理員功能**：新增/編輯書籍資訊（需管理員權限）
- 📧 **訂單通知**：付款成功後自動寄送訂單確認信（選用）

---

## 🛠️ 技術架構

### 後端技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 程式語言 |
| Spring Boot | 4.1.1 | 應用框架 |
| Spring Data JPA | 4.1.1 | ORM 持久層 |
| Spring Security | 4.1.1 | 安全認證 |
| PostgreSQL | 14+ | 資料庫 |
| JWT (jjwt) | 0.12.6 | Token 認證 |
| Lombok | latest | 簡化程式碼 |
| Maven | 3.x | 專案管理 |

### 前端技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.5 | 前端框架 |
| Vue Router | 5.3 | 路由管理 |
| Vite | 8.2 | 建構工具 |
| ESLint + Oxlint | latest | 程式碼檢查 |

---

## 📁 專案結構

```
bookstore/
├── backend/                    # Spring Boot 後端
│   ├── src/main/java/com/bookstore/
│   │   ├── controller/        # REST API 控制器
│   │   │   ├── BookController.java
│   │   │   ├── CartController.java
│   │   │   ├── OrderController.java
│   │   │   └── UserController.java
│   │   ├── entity/            # JPA 實體類別
│   │   │   ├── Book.java
│   │   │   ├── CartItem.java
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── User.java
│   │   ├── service/           # 業務邏輯層
│   │   ├── repository/        # 資料存取層
│   │   ├── dto/               # 資料傳輸物件
│   │   ├── config/            # 配置檔案
│   │   └── exception/         # 異常處理
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/             # 頁面元件
│   │   ├── router/            # 路由配置
│   │   ├── api/               # API 請求封裝
│   │   ├── composables/       # 組合式函數
│   │   ├── constants/         # 常數定義
│   │   └── assets/            # 靜態資源
│   ├── package.json
│   └── vite.config.js
└── README.md
```

---

## 🚀 快速開始

### 環境需求

- **Java**: 21 或以上
- **Node.js**: 22.18.0+ 或 24.12.0+
- **PostgreSQL**: 14 或以上
- **Maven**: 3.x

### 資料庫設定

1. 建立 PostgreSQL 資料庫：

```sql
CREATE DATABASE bookstore;
```

2. 在 `backend/src/main/resources/` 目錄下建立 `application-local.properties` 檔案（此檔案不會被提交到 Git）：

```properties
# 資料庫密碼
spring.datasource.password=your_database_password

# JWT 金鑰（建議使用至少 256 位元的隨機字串）
jwt.secret=your_jwt_secret_key_here

# 管理員帳號設定
app.admin.email=admin@example.com
app.admin.password=admin123
app.admin.name=管理員

# 【選用】Email 寄送設定（若不設定 spring.mail.host，付款功能仍可正常運作，僅不寄送郵件）
# spring.mail.host=smtp.gmail.com
# spring.mail.port=587
# spring.mail.username=your_email@gmail.com
# spring.mail.password=your_app_password
# spring.mail.properties.mail.smtp.auth=true
# spring.mail.properties.mail.smtp.starttls.enable=true
```

### 後端啟動

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

後端服務將在 `http://localhost:8080` 啟動

### 前端啟動

```bash
cd frontend
npm install
npm run dev
```

前端服務將在 `http://localhost:5173` 啟動（預設 Vite 埠號）

---

## 🌐 API 端點總覽

### 使用者相關 `/api/users`

| 方法 | 端點 | 說明 | 需要驗證 |
|------|------|------|----------|
| POST | `/register` | 註冊新使用者 | ✗ |
| POST | `/login` | 使用者登入 | ✗ |
| GET | `/me` | 取得當前使用者資訊 | ✓ |
| PUT | `/me` | 更新個人資料 | ✓ |

### 書籍相關 `/api/books`

| 方法 | 端點 | 說明 | 需要驗證 |
|------|------|------|----------|
| GET | `/` | 取得所有書籍 | ✗ |
| GET | `/{id}` | 取得單一書籍詳情 | ✗ |
| POST | `/` | 新增書籍 | ✓（管理員） |
| PUT | `/{id}` | 更新書籍資訊 | ✓（管理員） |

### 購物車相關 `/api/cart`

| 方法 | 端點 | 說明 | 需要驗證 |
|------|------|------|----------|
| GET | `/` | 取得購物車內容 | ✓ |
| POST | `/items` | 加入商品到購物車 | ✓ |
| PUT | `/items/{id}` | 更新購物車商品數量 | ✓ |
| DELETE | `/items/{id}` | 移除購物車商品 | ✓ |

### 訂單相關 `/api/orders`

| 方法 | 端點 | 說明 | 需要驗證 |
|------|------|------|----------|
| GET | `/` | 取得使用者所有訂單 | ✓ |
| POST | `/` | 建立新訂單 | ✓ |
| POST | `/{id}/pay` | 訂單付款 | ✓ |
| POST | `/{id}/cancel` | 取消訂單 | ✓ |
| POST | `/{id}/receive` | 確認收貨 | ✓ |

> 💡 需要驗證的端點需在 HTTP Header 中加入：`Authorization: Bearer <JWT_TOKEN>`

---

## 🖼️ 系統截圖

<!-- 
TODO: 新增系統截圖
建議包含以下畫面：
- 首頁書籍列表
- 書籍詳情頁面
- 購物車頁面
- 訂單管理頁面
- 管理員新增/編輯書籍頁面

範例格式：
![首頁](docs/screenshots/home.png)
![購物車](docs/screenshots/cart.png)
-->

_本專案尚未新增系統截圖，可在 `docs/screenshots/` 目錄下加入畫面擷取並更新此區塊。_

---

## 🔒 安全性

- 使用 JWT (JSON Web Token) 進行無狀態身份驗證
- 密碼採用 BCrypt 加密存儲
- Spring Security 提供端點層級的訪問控制
- 敏感設定（資料庫密碼、JWT 密鑰）透過 `application-local.properties` 隔離管理

---

## 📝 開發注意事項

1. **資料庫初始化**：首次啟動時，Spring JPA 會自動建立資料表結構（`spring.jpa.hibernate.ddl-auto=update`）
2. **管理員帳號**：系統會在啟動時自動建立管理員帳號（設定在 `application-local.properties`）
3. **CORS 設定**：開發環境已設定允許前端跨域請求
4. **Email 功能**：若未設定 Email 伺服器，訂單付款流程仍可正常運作，僅不會寄送通知信

---

## 👤 作者

**nozomi0414**

- GitHub: [@nozomi0414](https://github.com/nozomi0414)
- 本專案為個人作品集項目

---

## 📄 授權

此專案僅供學習與作品展示用途。

---

_最後更新：2026-10-28_
