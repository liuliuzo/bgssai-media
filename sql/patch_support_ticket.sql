-- Additive patch: chat-first CS + ticket-from-chat (2026-09-19)
-- 会话面板内「提工单」；工单挂接当前会话，不替代聊天。
USE bgssai_media;

CREATE TABLE IF NOT EXISTS media_support_ticket (
  id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  session_id       BIGINT       NOT NULL                          COMMENT 'media_support_session.id',
  subject          VARCHAR(256) NULL                              COMMENT '工单主题；可默认取会话/最近消息',
  priority         VARCHAR(16)  NOT NULL DEFAULT 'normal'         COMMENT 'low / normal / high',
  category         VARCHAR(64)  NULL                              COMMENT '可选分类',
  status           VARCHAR(16)  NOT NULL DEFAULT 'open'           COMMENT 'open / pending / resolved / closed',
  description      TEXT         NULL                              COMMENT '可选说明；可默认取最近消息摘要',
  created_by_user_id BIGINT     NULL                              COMMENT '提单用户；匿名可空',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_media_support_ticket_session (session_id),
  KEY idx_media_support_ticket_status (status),
  CONSTRAINT fk_media_support_ticket_session FOREIGN KEY (session_id) REFERENCES media_support_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话挂接工单（聊中提工单）';
