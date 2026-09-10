-- Patch: MCP personal access tokens (blog paradigm)
USE bgssai_media;

CREATE TABLE IF NOT EXISTS mcp_token (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL                          COMMENT '所属用户 sys_user.id',
  name            VARCHAR(64)  NOT NULL                          COMMENT '令牌备注名（如 Claude Desktop）',
  token_prefix    VARCHAR(16)  NOT NULL                          COMMENT '明文前缀，用于列表展示',
  token_suffix    VARCHAR(8)   NOT NULL                          COMMENT '明文后缀，用于列表展示',
  token_hash      CHAR(64)     NOT NULL                          COMMENT '令牌 SHA-256 摘要（绝不存明文）',
  status          VARCHAR(16)  NOT NULL DEFAULT 'active'         COMMENT 'active / revoked',
  last_used_at    DATETIME     DEFAULT NULL                      COMMENT '最近一次成功鉴权时刻',
  revoked_at      DATETIME     DEFAULT NULL                      COMMENT '吊销时刻',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mcp_token_hash (token_hash),
  KEY idx_mcp_token_user (user_id, status),
  CONSTRAINT fk_mcp_token_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 个人访问令牌';
