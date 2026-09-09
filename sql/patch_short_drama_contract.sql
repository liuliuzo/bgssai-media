
-- Patch existing DBs created before shared short-drama contract columns.
ALTER TABLE media_ingest_log
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(256) NULL,
  ADD COLUMN IF NOT EXISTS media_id VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS play_url VARCHAR(4096) NULL;
-- Unique index (ignore error if exists)
-- MariaDB 10.11 supports IF NOT EXISTS for columns; index separately:
