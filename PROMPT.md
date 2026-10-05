# 專案需求 (PROMPT)

## 目標
建立一個「文章按讚排行榜」網頁應用程式，作為 Todo List 練習的延伸，本次重點練習 **Redis**。

## 技術棧
- 後端：Spring Boot (Java)
- 前端：React + TypeScript
- 資料庫：MySQL（貼文資料的 source of truth）
- 快取 / 即時排行：Redis（Sorted Set 做即時排行榜、原子計數）

## 已確認的決策
| 項目 | 決策 |
|---|---|
| 專案定位 | Todo List 練習的延伸，沿用同一套技術棧，新增 Redis |
| 情境 | 文章 / 貼文按讚排行榜 |
| 使用者登入功能 | 不需要（沿用 Todo List 假設，任何人皆可對貼文按讚，無帳號/註冊/登入） |
| 後端建構工具 | Maven（沿用） |
| 前端語言 | TypeScript（沿用） |
| MySQL / Redis 執行方式 | Docker Compose（沿用） |

## 預設假設（尚未經使用者明確確認，如有不同想法可隨時調整）
| 項目 | 預設決策 | 說明 |
|---|---|---|
| 資料權責分工 | MySQL 存 `Post`（id, title, content, likeCount, createdAt）當 source of truth；Redis Sorted Set（`post:likes`，member=postId, score=讚數）當即時排行榜索引 | **已定案（2026-10-02）：同步寫入**。按讚在同一個 `@Transactional` 內先 `UPDATE post SET like_count = like_count + 1`（更新 0 筆 → 404），再 `ZINCRBY post:likes 1 <id>`；Redis 失敗則整筆 rollback（代價：Redis 掛掉時無法按讚）。新增貼文時 `ZADD post:likes 0 <id>`。排行榜用 `ZREVRANGE` 取 id 後 `findAllById` 補詳情並依 Redis 順序排列；同分時依 member 字典序，暫不處理。用 Redis 的理由是排行榜讀取不必每次 `ORDER BY`（註：原本寫的「避免 `count = count + 1` 併發問題」不成立——單一 `UPDATE ... + 1` 在 MySQL 本身就是原子的，會出問題的是讀出來、Java 加 1、再存回去）。非同步批次寫回列為之後的比較練習 |
| 重複按讚防護 | 第一版不做（無登入，無法可靠識別使用者，同一人可重複按讚）；列為 Phase 12 stretch goal |
| 排行榜查詢 | `GET /api/posts/leaderboard?limit=10`，用 Redis `ZREVRANGE` / `ZREVRANGEBYSCORE` 取前 N 名 |
| Redis 資料持久化 | 開發環境先不特別設定；列一條 stretch/備註：正式環境需開 Redis AOF，或提供「從 MySQL rebuild zset」的還原機制 |
| Spring Boot 版本 | 使用 Spring Initializr 當前預設最新穩定版 |
| 部署方式 | 僅本機開發環境（Docker Compose 跑 MySQL + Redis，後端/前端本機執行），全部 Docker 化列為 stretch goal |

完整分階段任務規劃請見 [PROGRESS.md](./PROGRESS.md)。

## 本機環境資訊
- Java: 21.0.11
- Node: v24.13.1
- npm: 11.8.0
- Docker: 29.4.2
- Maven: 未安裝於 PATH，但可用 IntelliJ 內建 Maven，或改用 Maven Wrapper (mvnw)
- Redis: 未安裝，計畫透過 Docker Compose 執行
