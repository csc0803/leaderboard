# 專案進度 (PROGRESS)

最後更新：2026-10-05

## 目前狀態
Phase 0～2 已完成。backend 骨架（Spring Boot + Maven，含 Data JPA / Spring Data Redis 依賴）已建立，`db/01-init.sql` 定義好 `leaderboarddb` / `post` table，`docker-compose.yml` 可用 `docker compose up -d` 啟動 MySQL + Redis 容器（皆 healthy）。`Post` entity 與 `PostRepository` 已建立，`local`/`docker` 兩種 profile 都實測啟動成功，Hibernate `validate` 確認 entity 與 `post` table schema 一致。接下來進入 Phase 3（後端 API 層，含 Redis 整合，本專案重點）。

2026-10-02：Phase 3 進行中。`PostService`/`PostController` 的 MySQL 版本已完成並以 curl 手動測試通過（建立/查詢/按讚/排行榜、400/404 驗證）；讚數同步策略定案為「同步寫入」（細節見 PROMPT.md 資料權責分工）。下一步：Redis 整合。

2026-10-05：Phase 3 完成。Redis 整合（建立貼文 `ZADD`、按讚同步 `ZINCRBY`、排行榜改讀 `ZREVRANGE`、啟動時 zset 為空則從 MySQL rebuild）、`GlobalExceptionHandler`、CORS、Swagger 皆完成，並跑過完整手動測試（含 Redis 停機、容器重建後 rebuild）。測試中發現 Redis 停機時 request 會卡住，已加 Redis timeout 修正。下一步：Phase 4 後端測試。

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

## Phase 3：後端 API 層（含 Redis 整合，本專案重點）（已完成）
- [x] 建立 `PostService`：`getAllPosts` / `getPostById` / `createPost` / `likePost` / `getLeaderboard`（MySQL 版本完成；按讚方法目前命名為 `updateLike`）
- [x] Redis 整合：直接用 Spring Boot 自動設定的 `StringRedisTemplate`（不另寫 `RedisConfig`），zset key 為 `post:likes`。Sorted Set 操作目前直接寫在 `PostServiceImpl`，未另外封裝成類別
- [x] 建立 `PostController`：
  - [x] `GET /api/posts`（查全部貼文）
  - [x] `GET /api/posts/{id}`（查單筆，不存在回 404）
  - [x] `POST /api/posts`（新增貼文，用 `CreatePostRequest` DTO + `@Valid`，回 201）
  - [x] `POST /api/posts/{id}/like`（按讚，同步寫入：同一 `@Transactional` 內先 `PostRepository.incrementLikeCount`（JPQL `UPDATE ... + 1`，更新 0 筆 → 404），再 Redis `ZINCRBY`；回 204）
  - [x] `GET /api/posts/leaderboard?limit=10`（`ZREVRANGE` 取前 N 名 id → `findAllById` 一次撈詳情 → 依 Redis 順序重組，Redis 有、MySQL 沒有的 id 略過；`limit` 限制 1～100。同分時 Redis 依 member 字串字典序排，例如 "9" 排在 "10" 前，暫不處理）
- [x] Redis 遺失資料時的還原機制：`config/LeaderboardInitializer`（`ApplicationRunner`）啟動時 `ZCARD`，為 0 則呼叫 `PostService.rebuildLeaderboard()`，從 MySQL `findAll` 組成 `TypedTuple` 後一次 `ZADD` 寫回。過程中修正過兩個 bug：key 打成 `posts:likes`、判斷條件寫反（`size == null && size == 0`，會 NPE 且永遠 rebuild）。已知限制：只在啟動時檢查，backend 執行中 Redis 資料遺失則排行榜為空，直到重啟（「讀取時補救」列為選配，未做）
- [x] 輸入驗證與統一錯誤處理：`exception/PostNotFoundException` 取代 Service 裡的 `ResponseStatusException`；`GlobalExceptionHandler` 處理 404（`PostNotFoundException`）、400（`MethodArgumentNotValidException` / `HandlerMethodValidationException` / `MethodArgumentTypeMismatchException`），回應含 `message`。驗證訊息語系跟隨 JVM（目前為中文）。未處理：Redis 不可用時目前回預設 500（可加 503 handler，選配）
- [x] 設定 CORS：`config/CorsConfig`，`/api/**` 只允許 `http://localhost:5173`、方法只開 `GET`/`POST`；curl 驗證 preflight 200、其他 origin / 未允許方法 403
- [x] 加入 Swagger：`springdoc-openapi-starter-webmvc-ui` 3.0.3（比照 Todo List），`/swagger-ui.html`、`/v3/api-docs` 正常，`@Min`/`@Max`/`@NotBlank`/`@Size` 有自動反映到文件。錯誤回應（400/404）未出現在文件中，暫不處理
- [x] 手動測試所有 API 端點：建立/按讚/排行榜即時更新、MySQL 與 Redis 讚數一致、`docker restart` Redis（RDB 快照，資料保留、Lettuce 自動重連）、刪除重建 Redis 容器後重啟 backend 會 rebuild 且與 MySQL 一致
- [x] 修正：Redis 停機時 request 卡住。Lettuce 斷線時預設會暫存指令、等重連後才送出，加上 Spring Boot 未設定時指令逾時為 60 秒，導致按讚 request 卡住約 34 秒，期間一直佔著 MySQL row lock、Tomcat 執行緒與 DB 連線。已在 `application.yaml` 加 `spring.data.redis.timeout: 2s`、`connect-timeout: 2s`；複測：Redis 停機時按讚/建立貼文/排行榜約 2 秒回 500，MySQL 正確 rollback，Redis 恢復後不會補送逾時的指令
- [x] 順手設定 `spring.jpa.open-in-view: false`（消除啟動 WARN）

## Phase 4：後端測試（已完成）
- [x] Repository 層測試（`@DataJpaTest` + H2，`application-test.yaml` 用 `MODE=MySQL` + `ddl-auto: create-drop`）：`@PrePersist` 預設值、`incrementLikeCount` 回傳筆數/累加/只影響目標貼文
- [x] Redis 相關邏輯測試：`PostServiceImplTest` 以 Mockito mock `StringRedisTemplate` / `ZSetOperations`，涵蓋按讚不存在時不碰 Redis、排行榜維持 Redis 順序並略過 MySQL 已不存在的 id、rebuild 寫入的 score
- [x] Controller 層測試（`@WebMvcTest` + MockMvc）：各端點 200/201/204、404、400（驗證失敗、型別錯誤、`limit` 邊界 1/100 與超出範圍 0/-1/101）、`/posts/leaderboard` 不會被當成 `/posts/{id}`、CORS preflight（允許 origin 200、其他 origin / 未允許方法 403）
- [x] 確認 `mvn test` 全數通過（43 tests，0 failures）。`BackendApplicationTests` 以 `@MockitoBean LeaderboardInitializer` 避免啟動時連 Redis

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
- [ ] 進入 Phase 4：後端測試（Repository / Redis 相關 Service 邏輯 / Controller）
- [ ] （選配）Redis 不可用時回 503、`getLeaderboard` 讀取時補救 rebuild、`application-docker.yaml` 刪除與 base 重複的設定

## 備註
- 讚數的權威來源與同步策略：已定案（2026-10-02）MySQL 為 source of truth、同步寫入，取捨記錄於 PROMPT.md。
- Redis 逾時：未設定 `spring.data.redis.timeout` 時 Lettuce 斷線會暫存指令，request 卡到重連或 60 秒逾時；同步寫入策略下必須設定逾時，Redis 失敗才會快速 rollback（2026-10-05 發現並修正）。
- 已知小問題：`created_at`/`updated_at` 為 `DATETIME`（秒精度），建立貼文的回應時間含奈秒、之後查詢被進位到秒，兩者不一致。
