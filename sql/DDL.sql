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

CREATE TABLE IF NOT EXISTS user_identity (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  provider   VARCHAR(20)  NOT NULL COMMENT 'WECHAT / DOUYIN / BAIDU / ALIPAY / CHAT',
  open_id    VARCHAR(128) NOT NULL,
  nickname   VARCHAR(64)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_identity (provider, open_id),
  KEY idx_media_identity_user (user_id)
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

-- -----------------------------------------------------------------------------
-- 登录验证码通道（与 chat-cn / chat-global 同款口径）
--
-- 为什么验证码必须落库而不是留在进程内 Map：
--   1. 内存 Map 没有过期，进程活多久验证码就有效多久；
--   2. 多实例部署时发码与校验可能落在不同实例，用户会收到「验证码不对」；
--   3. 尝试次数、重发间隔这些风控计数没有共同的存放点。
-- 演示用固定码（如 123456）在任何环境都不允许存在：能访问发码接口的人
-- 不需要持有手机或邮箱就能登录任意已有用户。
-- -----------------------------------------------------------------------------

-- 表: platform_email_config (平台 SMTP 发信配置，单例行 id=1)
-- 凭据由管理端控制台维护并落库，不写进 application-*.properties:
--   properties 随构建产物进 jar、进镜像、进 Git，改一次要重新发版；
--   落库则是运营在控制台改完即刻生效。
-- smtp_password 读路径只出掩码，绝不回显、绝不入日志。
-- enabled=0 或 host / username / password 任一为空 = 通道不可用，
-- 发信路径直接报错，不静默成功。
CREATE TABLE IF NOT EXISTS platform_email_config (
  id               BIGINT       NOT NULL                              COMMENT '固定单例 ID (恒为 1)',
  smtp_host        VARCHAR(128) DEFAULT NULL                          COMMENT 'SMTP 服务器地址',
  smtp_port        INT          NOT NULL DEFAULT 465                  COMMENT 'SMTP 端口 (默认 465 SSL)',
  smtp_username    VARCHAR(128) DEFAULT NULL                          COMMENT 'SMTP 登录账号 (通常为发件邮箱)',
  smtp_password    VARCHAR(256) DEFAULT NULL                          COMMENT 'SMTP 授权码 (敏感: 出参掩码、绝不回显、绝不入日志)',
  from_address     VARCHAR(128) DEFAULT NULL                          COMMENT '发件人地址 (留空回退 smtp_username)',
  from_name        VARCHAR(64)  DEFAULT NULL                          COMMENT '发件人显示名',
  ssl_enable       TINYINT(1)   NOT NULL DEFAULT 1                    COMMENT '是否启用 SSL',
  starttls_enable  TINYINT(1)   NOT NULL DEFAULT 0                    COMMENT '是否启用 STARTTLS',
  auth_enable      TINYINT(1)   NOT NULL DEFAULT 1                    COMMENT '是否启用 SMTP 认证',
  enabled          TINYINT(1)   NOT NULL DEFAULT 0                    COMMENT '是否启用平台发信: 1=启用 / 0=停用',
  last_test_status VARCHAR(16)  DEFAULT NULL                          COMMENT '最近一次试发结果: SUCCESS / FAILED',
  last_test_at     DATETIME     DEFAULT NULL                          COMMENT '最近一次试发时刻',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台 SMTP 发信配置 (单例 id=1，管理端维护)';

-- 表: platform_sms_config (平台短信通道配置，单例行 id=1)
-- 口径与 platform_email_config 一致。各场景模板 ID 分列存放:
-- 留空即「该场景不可发码」，发码接口返回明确错误，而不是拿别的场景模板凑合发
-- (短信签名与模板是审核制，串用会被服务商拒发，且拒发在控制台之外看不见)。
CREATE TABLE IF NOT EXISTS platform_sms_config (
  id                         BIGINT       NOT NULL                    COMMENT '固定单例 ID (恒为 1)',
  provider                   VARCHAR(32)  NOT NULL DEFAULT 'TENCENT'  COMMENT '短信服务商: TENCENT',
  secret_id                  VARCHAR(128) DEFAULT NULL                COMMENT '服务商 SecretId',
  secret_key                 VARCHAR(256) DEFAULT NULL                COMMENT '服务商 SecretKey (敏感: 出参掩码)',
  sdk_app_id                 VARCHAR(32)  DEFAULT NULL                COMMENT '短信应用 SdkAppId',
  sign_name                  VARCHAR(64)  DEFAULT NULL                COMMENT '短信签名内容 (须为审核通过的签名本身)',
  region                     VARCHAR(32)  NOT NULL DEFAULT 'ap-guangzhou' COMMENT '服务区域',
  login_template_id          VARCHAR(32)  DEFAULT NULL                COMMENT 'LOGIN 场景模板 ID；空=该场景不可发码',
  bind_phone_template_id     VARCHAR(32)  DEFAULT NULL                COMMENT 'BIND_PHONE 场景模板 ID；空=该场景不可发码',
  reset_password_template_id VARCHAR(32)  DEFAULT NULL                COMMENT 'RESET_PASSWORD 场景模板 ID；空=该场景不可发码',
  code_expire_seconds        INT          NOT NULL DEFAULT 300        COMMENT '验证码有效期 (秒)',
  enabled                    TINYINT(1)   NOT NULL DEFAULT 0          COMMENT '是否启用短信通道',
  last_test_status           VARCHAR(16)  DEFAULT NULL                COMMENT '最近一次连通性测试结果',
  last_test_at               DATETIME     DEFAULT NULL                COMMENT '最近一次连通性测试时刻',
  created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台短信通道配置 (单例 id=1，管理端维护)';

-- 表: media_verify_code (登录验证码)
-- code 落 SHA-256 摘要而不是明文：库被读到时明文码等同于一批可直接登录的凭证。
-- used / expires_at / attempts 三列合起来才构成「一次性、会过期、猜不动」;
-- 少任何一列，验证码都退化成一个长期有效的口令。
CREATE TABLE IF NOT EXISTS media_verify_code (
  id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  target       VARCHAR(128) NOT NULL                          COMMENT '手机号或邮箱',
  channel      VARCHAR(16)  NOT NULL                          COMMENT '投递通道: SMS / EMAIL',
  scene        VARCHAR(32)  NOT NULL DEFAULT 'LOGIN'          COMMENT '发码场景: LOGIN / BIND_PHONE / RESET_PASSWORD',
  code_hash    CHAR(64)     NOT NULL                          COMMENT '验证码 SHA-256 摘要 (绝不存明文)',
  attempts     INT          NOT NULL DEFAULT 0                COMMENT '已尝试校验次数；超过上限即作废',
  used         TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否已消费: 1=已用 (一次性)',
  expires_at   DATETIME     NOT NULL                          COMMENT '过期时刻',
  sent_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时刻 (重发间隔与频控依据)',
  used_at      DATETIME     DEFAULT NULL                      COMMENT '消费时刻',
  KEY idx_mvc_lookup (target, scene, used, expires_at),
  KEY idx_mvc_sent (target, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录验证码 (一次性、带过期与尝试上限)';

-- -----------------------------------------------------------------------------
-- MCP 个人访问令牌（对照 blog：用户自助创建，仅哈希落库，明文只在创建时回显一次）
-- 供 Claude / Codex / Cursor / Grok Bot / bgssai-bot 等经 Authorization: Bearer 调用 /api/mcp
-- -----------------------------------------------------------------------------
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
