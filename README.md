<p align="center">
  <img src="frontend/public/wordmark.png" alt="拾頁" width="280">
</p>

<p align="center">
  練習用的線上書店。瀏覽書籍、加入購物車、下單，並以模擬付款走完訂單。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-f89820" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.1-6db33f" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/Vue-3-42b883" alt="Vue 3">
  <img src="https://img.shields.io/badge/PostgreSQL-bookstore-336791" alt="PostgreSQL">
</p>

## 功能

- 書架可依分類篩選、搜尋書名或作者，點書名開啟可分享的書籍頁。
- 未登入可先把書放進購物車；登入後會併入該帳號的購物車。
- 結帳只包含有勾選的品項。未勾選的會留在購物車。
- 送出訂單後為待付款，庫存只檢查、不扣除。付款為模擬，完成後貨態為已抵達並扣除庫存。
- 確認已收貨後，訂單改為已收貨，並從進行中的訂單移到歷史訂單。待付款的訂單可以取消。
- 管理員可上架與編輯書籍。封面使用圖片網址。
- 付款後可寄訂單信。未設定郵件伺服器時，付款仍會完成。

## 環境

- Java 21
- Node.js 22.18 以上，或 24.12 以上
- PostgreSQL，資料庫名稱 `bookstore`，連接埠 `5432`，使用者 `postgres`

## 啟動

先建立資料庫，再在 `backend/src/main/resources/` 新增 `application-local.properties`。此檔已列入忽略清單，不要提交。

```properties
spring.datasource.password=資料庫密碼
jwt.secret=至少三十二個字元的隨機字串

app.admin.email=admin@example.com
app.admin.password=請自行設定
app.admin.name=管理員
```

後端第一次啟動時，若這個電子郵件尚未存在，會建立管理員帳號。資料表由 JPA 依實體建立與更新。

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

開啟 [http://localhost:5173](http://localhost:5173)。前端會把 `/api` 轉到 `http://localhost:8080`。

若要在付款後寄信，於同一個設定檔加入郵件主機。未設定 `spring.mail.host` 時不會寄信。

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=你的信箱
spring.mail.password=應用程式密碼
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 頁面

| 路徑 | 說明 |
| --- | --- |
| `/` | 書架 |
| `/books/:id` | 書籍頁 |
| `/cart` | 購物車 |
| `/login`、`/register` | 登入、註冊 |
| `/orders` | 進行中的訂單 |
| `/orders/history` | 歷史訂單（已收貨、已取消） |
| `/account` | 電話與地址 |
| `/books/new`、`/books/:id/edit` | 上架、編輯，僅管理員 |

## API

需要登入的請求帶 `Authorization: Bearer <token>`。註冊的帳號為一般使用者；書籍的新增與修改會在伺服器確認管理員身分。

| 方法 | 路徑 | 說明 |
| --- | --- | --- |
| POST | `/api/users/register` | 註冊 |
| POST | `/api/users/login` | 登入 |
| GET | `/api/users/me` | 讀取個人資料 |
| PUT | `/api/users/me` | 更新個人資料 |
| GET | `/api/books` | 書籍列表 |
| GET | `/api/books/{id}` | 單本書 |
| POST | `/api/books` | 上架，管理員 |
| PUT | `/api/books/{id}` | 編輯，管理員 |
| GET | `/api/cart` | 購物車 |
| POST | `/api/cart` | 加入 |
| PUT | `/api/cart/{bookId}` | 修改數量 |
| DELETE | `/api/cart/{bookId}` | 移除一項 |
| DELETE | `/api/cart` | 清空 |
| GET | `/api/orders` | 訂單列表 |
| POST | `/api/orders` | 建立訂單 |
| POST | `/api/orders/{id}/pay` | 模擬付款 |
| POST | `/api/orders/{id}/cancel` | 取消 |
| POST | `/api/orders/{id}/receive` | 確認已收貨 |

## 目錄

```
bookstore/
├── backend/     Spring Boot、JPA、PostgreSQL
└── frontend/    Vue 3、Vue Router、Vite
```
