SET @session_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'current_session_id');
SET @session_ddl = IF(@session_exists = 0, 'ALTER TABLE sys_user ADD COLUMN current_session_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE session_statement FROM @session_ddl;
EXECUTE session_statement;
DEALLOCATE PREPARE session_statement;
