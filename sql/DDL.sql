-- bgssai_media schema (MySQL 8)
CREATE DATABASE IF NOT EXISTS bgssai_media DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bgssai_media;

CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  email         VARCHAR(128) NULL,
  phone         VARCHAR(32)  NULL,
  role_code     VARCHAR(32)  NOT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'active',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_drama (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  title         VARCHAR(256) NOT NULL,
  cover_url     VARCHAR(1024) NULL,
  description   TEXT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'draft',
  source        VARCHAR(32)  NOT NULL DEFAULT 'manual',
  external_ref  VARCHAR(128) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_drama_external_ref (external_ref),
  KEY idx_media_drama_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_episode (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  drama_id      BIGINT       NOT NULL,
  ep_no         INT          NOT NULL,
  title         VARCHAR(256) NOT NULL,
  duration_sec  INT          NULL,
  media_url     VARCHAR(2048) NULL,
  storage_key   VARCHAR(512) NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'draft',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_episode_drama_ep (drama_id, ep_no),
  KEY idx_media_episode_drama (drama_id),
  CONSTRAINT fk_media_episode_drama FOREIGN KEY (drama_id) REFERENCES media_drama(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_watch_progress (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL,
  drama_id        BIGINT       NOT NULL,
  episode_id      BIGINT       NOT NULL,
  position_sec    INT          NOT NULL DEFAULT 0,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_watch_user_drama (user_id, drama_id),
  KEY idx_watch_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS media_ingest_log (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_ref     VARCHAR(128) NOT NULL,
  drama_id         BIGINT       NULL,
  payload_json     MEDIUMTEXT   NULL,
  status           VARCHAR(32)  NOT NULL,
  message          VARCHAR(1024) NULL,
  idempotency_key  VARCHAR(256) NULL,
  media_id         VARCHAR(64)  NULL,
  play_url         VARCHAR(4096) NULL,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ingest_idempotency (idempotency_key),
  KEY idx_ingest_external_ref (external_ref),
  KEY idx_ingest_media_id (media_id),
  KEY idx_ingest_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Additive catalog views used by shared short-drama contract (maps onto drama/episode).
-- media_id is a stable public id stored on media_ingest_log and derived from episode id.
