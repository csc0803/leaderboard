-- 本機開發用初始化腳本，僅供 localhost / docker-compose 開發環境使用
-- 密碼為開發用預留值，正式環境請改用環境變數或密鑰管理，勿沿用此值
CREATE DATABASE IF NOT EXISTS leaderboarddb DEFAULT CHARSET=utf8mb4;
CREATE USER IF NOT EXISTS 'leaderboard'@'%' IDENTIFIED BY 'leaderboard_local_dev_only';
ALTER USER 'leaderboard'@'%' IDENTIFIED BY 'leaderboard_local_dev_only';
GRANT SELECT, INSERT, UPDATE, DELETE ON leaderboarddb.* TO 'leaderboard'@'%';
FLUSH PRIVILEGES;
USE leaderboarddb;

CREATE TABLE IF NOT EXISTS post(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	title VARCHAR(100) NOT NULL,
	content TEXT,
    like_count BIGINT DEFAULT 0 NOT NULL,
	created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
	updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);