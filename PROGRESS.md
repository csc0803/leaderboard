# 專案進度 (PROGRESS)

最後更新：2026-09-11

## 目前狀態
Phase 0～2 已完成。backend 骨架（Spring Boot + Maven，含 Data JPA / Spring Data Redis 依賴）已建立，`db/01-init.sql` 定義好 `leaderboarddb` / `post` table，`docker-compose.yml` 可用 `docker compose up -d` 啟動 MySQL + Redis 容器（皆 healthy）。`Post` entity 與 `PostRepository` 已建立，`local`/`docker` 兩種 profile 都實測啟動成功，Hibernate `validate` 確認 entity 與 `post` table schema 一致。接下來進入 Phase 3（後端 API 層，含 Redis 整合，本專案重點）。

## 專案結構（規劃）
```
leaderboard/
├── PROMPT.md
├── PROGRESS.md
├── docker-compose.yml       # MySQL + Redis
├── backend/                  # Spring Boot (Maven)
└── frontend/                 # React (Vite)
```

---

## Phase 0：專案初始化與環境準備
- [ ] 確認技術棧：Spring Boot + React + MySQL + Redis
- [ ] 確認不需要登入功能（單人/無帳號使用）
- [ ] 確認後端建構工具使用 Maven
- [ ] 確認 MySQL / Redis 以 Docker Compose 執行
- [ ] 檢查本機開發環境（Java/Node/npm/Docker 版本、Redis 是否需額外安裝或全用 Docker）
- [ ] 確認 Spring Boot 版本
- [ ] 初始化 git repository
- [ ] 補上根目錄 `.gitignore`
- [ ] 建立第一個 git commit

## Phase 1：後端骨架建立（已完成）
- [x] 用 Spring Initializr 產生 backend/ 專案（依賴：Web、Data JPA、Validation、MySQL Driver、Spring Data Redis、Lombok）
- [x] 確認專案可用 Maven（或 wrapper）成功編譯（`.\mvnw.cmd clean compile` → BUILD SUCCESS）
- [x] 建立 DB script（`db/01-init.sql`）：`leaderboarddb` 資料庫、`post` table（id, title, content, like_count, created_at, updated_at），`like_count` 有 `DEFAULT 0 NOT NULL`
- [x] 設定 `application.yml`：MySQL 連線資訊（比照 Todo List 的 `local`/`docker` profile 隔離機密設定作法）。過程中修正一個 profile 沒套用導致的 bug：直接跑 `mvnw spring-boot:run` 沒帶 `-Dspring-boot.run.profiles=local`（或 IntelliJ Run Configuration 沒設 Active profiles）時，會漏掉 `application-local.yaml` 的密碼，噴 `Access denied for user 'leaderboard'@'localhost' (using password: NO)`
- [x] 設定 Redis 連線（host/port，先不設密碼）：`application.yaml`（base/local）用 `localhost:6379`；`application-docker.yaml` 原本誤寫成 `host: redis`（docker compose 內部網路才能解析的 service 名稱），但目前 backend 是跑在主機上（`mvnw` 直接執行、尚未容器化），需透過 `docker-compose.yml` 的 port mapping `"6379:6379"` 走 `localhost` 才能連到容器，已修正為 `host: localhost`。待 Phase 11 backend 也容器化、跟 Redis 同在 compose 網路內時，才會改回用 service 名稱 `redis`
- [x] 建立 `docker-compose.yml`（MySQL + Redis 服務）。過程中修正：一開始誤把 Phase 11 才要做的 `backend`/`frontend` 服務也寫進去（`backend/` 當時沒有 Dockerfile、`frontend/` 目錄根本不存在，會導致 `docker compose up` 失敗），已改回只留 `mysql` + `redis` 兩個服務
- [x] 本地端連線測試：`docker compose up -d` 啟動 `leaderboard-mysql`（healthy）、`leaderboard-redis` 兩個容器，backend 用 `docker` profile 啟動成功，port 8080 正常監聽，MySQL/Redis 皆連線無誤

## Phase 2：後端資料層（已完成）
- [x] 建立 `Post` entity（id, title, content, likeCount, createdAt, updatedAt）：`com.chun.backend.entity.Post`，風格比照 Todo List 的 `Todo` entity（Lombok `@Getter @Setter`、`@PrePersist`/`@PreUpdate` 補預設值）。`@PrePersist` 目前無條件把 `likeCount` 設為 0（不像 Todo 的 `completed` 會先檢查是否已有值），視為刻意設計：新貼文的讚數一律從 0 開始，不開放呼叫端指定
- [x] 建立 `PostRepository`（extends JpaRepository）：`JpaRepository<Post, Long>`，泛型順序正確
- [x] 設定 `spring.jpa.hibernate.ddl-auto=validate`（Phase 1 已設定，沿用）
- [x] 驗證資料表對應正確：`docker` profile 實際啟動一次（`mvn spring-boot:run`），`Found 1 JPA repository interface`，Hibernate 初始化無 `SchemaManagementException`，確認 `Post` entity 與既有 `post` table 完全對應

## Phase 3：後端 API 層（含 Redis 整合，本專案重點）
- [ ] 建立 `PostService`：`getAllPosts` / `getPostById` / `createPost` / `likePost` / `getLeaderboard`
- [ ] Redis 整合：設定 `RedisTemplate`（或 Lettuce），封裝 Sorted Set 操作（`ZINCRBY`、`ZREVRANGE`、`ZSCORE`）
- [ ] 建立 `PostController`：
  - [ ] `GET /api/posts`（查全部貼文）
  - [ ] `GET /api/posts/{id}`（查單筆）
  - [ ] `POST /api/posts`（新增貼文）
  - [ ] `POST /api/posts/{id}/like`（按讚：Redis `ZINCRBY` 遞增 + 同步/非同步更新 MySQL `like_count`）
  - [ ] `GET /api/posts/leaderboard?limit=10`（讀 Redis Sorted Set 取前 N 名，組合 MySQL 貼文詳情回傳）
- [ ] 決定並實作「Redis 遺失資料時的還原機制」：例如啟動時若 zset 為空，從 MySQL `like_count` 欄位 rebuild
- [ ] 加入輸入驗證與統一錯誤處理（比照 Todo List 的 `GlobalExceptionHandler`）
- [ ] 設定 CORS
- [ ] 加入 Swagger
- [ ] 手動測試所有 API 端點（含按讚後排行榜即時更新、重啟 Redis 後 rebuild 是否正確）

## Phase 4：後端測試
- [ ] Repository 層測試（`@DataJpaTest` + H2）
- [ ] Redis 相關邏輯測試（視情況用 embedded Redis 或針對 Service 邏輯 mock `RedisTemplate`）
- [ ] Controller 層測試（`@WebMvcTest` + MockMvc）
- [ ] 確認 `mvn test` 全數通過

## Phase 5：前端骨架建立
- [ ] 用 Vite 建立 frontend/ 專案（React + TypeScript）
- [ ] 確認 `npm install` / `npm run dev` 可正常啟動
- [ ] 建立基本目錄結構：`src/api`、`src/components`、`src/hooks`
- [ ] 設定 `.env` / `.env.example`（`VITE_API_BASE_URL`）

## Phase 6：前端 UI 元件
- [ ] `PostList`：顯示所有貼文（含目前讚數）
- [ ] `PostItem`：單筆貼文（標題/內容、按讚按鈕）
- [ ] `AddPostForm`：新增貼文表單
- [ ] `Leaderboard`：即時排行榜（依讚數排序前 N 名，可考慮輪詢或按讚後重新拉取）
- [ ] 空狀態、基本樣式

## Phase 7：前端 API 串接
- [ ] 建立 API service 模組
- [ ] 串接 `GET /api/posts`、`POST /api/posts`
- [ ] 串接 `POST /api/posts/{id}/like`：按讚後更新畫面（該筆讚數 + 排行榜）
- [ ] 串接 `GET /api/posts/leaderboard`
- [ ] Loading / Error 狀態處理

## Phase 8：前端測試
- [ ] 設定測試工具（Vitest + React Testing Library，比照 Todo List）
- [ ] 針對關鍵元件撰寫基本測試（按讚觸發回呼、排行榜排序顯示等）

## Phase 9：整合與端對端驗證
- [ ] 同時啟動 MySQL、Redis、後端、前端
- [ ] 用 curl 驗證按讚 → 排行榜即時反映
- [ ] 驗證 Redis 重啟後 rebuild 機制是否正確還原排行榜
- [ ] 用 Claude in Chrome 跑過前端主要流程（新增貼文 → 按讚 → 排行榜更新）
- [ ] 檢查瀏覽器 console 無錯誤

## Phase 10：文件
- [ ] 撰寫根目錄 `README.md`（專案簡介、技術棧、啟動步驟、Redis 資料模型說明）
- [ ] 記錄環境變數需求
- [ ] 記錄 API 規格

## Phase 11（Stretch）：容器化與一鍵啟動
- [ ] `backend/Dockerfile`、`frontend/Dockerfile`
- [ ] 擴充 `docker-compose.yml` 納入 backend/frontend 服務
- [ ] 驗證 `docker compose up -d --build` 一鍵啟動

## Phase 12（Stretch）：進階功能
- [ ] 防止重複按讚（localStorage 或 IP 限制）
- [ ] Redis 持久化設定（AOF）與還原流程驗證
- [ ] 依時間區間排行（例如「本週熱門」，可用多組 zset 或 time-decay 分數）
- [ ] 分頁 / 無限捲動
- [ ] 取消讚（unlike）

---

## 下一步（建議立即執行）
- [ ] 進入 Phase 3：`PostService`（`likePost` 等）與 Redis `RedisTemplate` 整合，`PostController` API 端點

## 備註
- 讚數的權威來源與同步策略（Redis vs MySQL）在 Phase 3 動手實作時需明確定案，並記錄取捨原因。
