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
| 資料權責分工 | MySQL 存 `Post`（id, title, content, likeCount, createdAt）當 source of truth；Redis Sorted Set（`post:likes`，member=postId, score=讚數）當即時排行榜索引 | 讚數遞增用 Redis `ZINCRBY` 做原子操作，避免直接對 MySQL 欄位 `UPDATE ... SET count = count + 1` 的併發問題；MySQL 端的 `likeCount` 欄位做備援/可查詢用，同步策略（每次寫入同步 vs. 非同步批次）留到動手時再定案 |
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
