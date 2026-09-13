
-- Patch existing DBs created before shared short-drama contract columns.
USE bgssai_media;

ALTER TABLE media_ingest_log
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(256) NULL,
  ADD COLUMN IF NOT EXISTS media_id VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS play_url VARCHAR(4096) NULL;

-- UNIQUE upsert key for short → media publish (short PR #54).
-- MySQL UNIQUE allows multiple NULLs (legacy admin logs without a key).
-- Re-run safe: skip when uk_ingest_idempotency already exists.
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'media_ingest_log'
    AND index_name = 'uk_ingest_idempotency'
);
SET @sqlstmt := IF(@idx_exists = 0,
  'ALTER TABLE media_ingest_log ADD UNIQUE KEY uk_ingest_idempotency (idempotency_key)',
  'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
