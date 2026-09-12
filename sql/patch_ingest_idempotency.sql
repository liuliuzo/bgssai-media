-- Additive patch: UNIQUE(idempotency_key) for short → media idempotent publish.
-- Apply after sql/patch_short_drama_contract.sql on DBs that already have the columns.
-- Fresh installs already have uk_ingest_idempotency in sql/DDL.sql.
-- Online execution status: NOT_VERIFIED.
USE bgssai_media;

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
