-- Additive patch: in-app online CS messaging (2026-09-19)
USE bgssai_media;

CREATE TABLE IF NOT EXISTS media_support_session (
  id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  session_token    VARCHAR(64)  NOT NULL                          COMMENT '访客凭据：匿名会话读回复用；勿当管理密钥',
  user_id          BIGINT       NULL                              COMMENT '登录用户 sys_user.id；匿名可空',
  contact_name     VARCHAR(64)  NULL                              COMMENT '联系人姓名（尽量采集）',
  contact_email    VARCHAR(128) NULL                              COMMENT '联系邮箱',
  contact_phone    VARCHAR(32)  NULL                              COMMENT '联系手机',
  subject          VARCHAR(256) NULL                              COMMENT '会话主题 / 首条摘要',
  status           VARCHAR(16)  NOT NULL DEFAULT 'open'           COMMENT 'open / pending / closed',
  last_message_at  DATETIME     NULL                              COMMENT '最近一条消息时间',
  user_unread      INT          NOT NULL DEFAULT 0                COMMENT '用户未读条数',
  admin_unread     INT          NOT NULL DEFAULT 0                COMMENT '客服未读条数',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_support_session_token (session_token),
  KEY idx_media_support_session_user (user_id),
  KEY idx_media_support_session_status (status),
  KEY idx_media_support_session_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内在线客服会话';

CREATE TABLE IF NOT EXISTS media_support_message (
  id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  session_id       BIGINT       NOT NULL                          COMMENT 'media_support_session.id',
  sender_type      VARCHAR(16)  NOT NULL                          COMMENT 'user / admin / system',
  sender_user_id   BIGINT       NULL                              COMMENT '发送方用户 id（匿名用户消息可空）',
  content          TEXT         NOT NULL                          COMMENT '文本正文',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_support_msg_session (session_id, id),
  CONSTRAINT fk_media_support_msg_session FOREIGN KEY (session_id) REFERENCES media_support_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内在线客服消息';
