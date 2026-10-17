-- Additive upgrade for OPC requirements 2026-10-17 (bgssai-media-pc).
-- Non-destructive: ADD COLUMN / CREATE TABLE IF NOT EXISTS only.
USE bgssai_media;

-- Account deletion (COMMON-016)
ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL COMMENT '注销时间；status=deleted 时填充' AFTER status;

-- Channel profile + ban (PRODUCT-009)
ALTER TABLE media_user_channel
  ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1024) NULL COMMENT '频道头像' AFTER bio,
  ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active / banned' AFTER avatar_url;

-- Creator video tags / visibility / moderation (PRODUCT-011/015)
ALTER TABLE media_creator_video
  ADD COLUMN IF NOT EXISTS tags VARCHAR(512) NULL COMMENT '逗号分隔标签' AFTER description,
  ADD COLUMN IF NOT EXISTS visibility VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT 'public / private / unlisted' AFTER tags,
  ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending / approved / rejected' AFTER visibility,
  ADD COLUMN IF NOT EXISTS moderated_at DATETIME NULL COMMENT '审核完成时间' AFTER moderation_status;

-- Backfill: already READY videos stay publicly playable
UPDATE media_creator_video
SET moderation_status = 'approved', moderated_at = COALESCE(moderated_at, updated_at, created_at)
WHERE status = 'READY' AND (moderation_status IS NULL OR moderation_status = 'pending');

-- Comment reply + pin + soft delete (PRODUCT-010)
ALTER TABLE media_video_comment
  ADD COLUMN IF NOT EXISTS parent_id BIGINT NULL COMMENT '回复的父评论 id' AFTER user_id,
  ADD COLUMN IF NOT EXISTS pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '创作者置顶' AFTER content,
  ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL COMMENT '软删除时间' AFTER pinned;

-- Not interested feedback (PRODUCT-011)
CREATE TABLE IF NOT EXISTS media_not_interested (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  video_id    BIGINT       NOT NULL,
  reason      VARCHAR(64)  NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_not_interested (user_id, video_id),
  KEY idx_media_not_interested_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不感兴趣反馈';

-- Watch history + pause preference (PRODUCT-012)
CREATE TABLE IF NOT EXISTS media_user_pref (
  user_id               BIGINT PRIMARY KEY,
  pause_watch_history   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=暂停记录观看历史',
  updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好';

CREATE TABLE IF NOT EXISTS media_watch_history (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  video_id      BIGINT       NOT NULL,
  content_id    VARCHAR(64)  NULL,
  title         VARCHAR(256) NULL,
  watched_sec   INT          NOT NULL DEFAULT 0,
  playable      TINYINT(1)   NOT NULL DEFAULT 1,
  last_watched_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_watch_history (user_id, video_id),
  KEY idx_media_watch_history_user_time (user_id, last_watched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观看历史';

CREATE TABLE IF NOT EXISTS media_watch_later (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  video_id    BIGINT   NOT NULL,
  sort_order  INT      NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_watch_later (user_id, video_id),
  KEY idx_media_watch_later_user (user_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='稍后观看';

CREATE TABLE IF NOT EXISTS media_playlist (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  title       VARCHAR(128) NOT NULL,
  visibility  VARCHAR(16)  NOT NULL DEFAULT 'private' COMMENT 'private / public',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_media_playlist_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自建播放列表';

CREATE TABLE IF NOT EXISTS media_playlist_item (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  playlist_id  BIGINT   NOT NULL,
  video_id     BIGINT   NOT NULL,
  sort_order   INT      NOT NULL DEFAULT 0,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_playlist_item (playlist_id, video_id),
  KEY idx_media_playlist_item_order (playlist_id, sort_order),
  CONSTRAINT fk_media_playlist_item_playlist FOREIGN KEY (playlist_id) REFERENCES media_playlist(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表条目';

-- Creator analytics events (PRODUCT-014)
CREATE TABLE IF NOT EXISTS media_view_event (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT       NULL,
  video_id      BIGINT       NOT NULL,
  channel_id    BIGINT       NOT NULL,
  watched_sec   INT          NOT NULL DEFAULT 0,
  completed     TINYINT(1)   NOT NULL DEFAULT 0,
  event_day     DATE         NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_view_event_video_day (video_id, event_day),
  KEY idx_media_view_event_channel_day (channel_id, event_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放事件（分析口径源）';

CREATE TABLE IF NOT EXISTS media_subscribe_event (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_id    BIGINT       NOT NULL,
  follower_user_id BIGINT    NOT NULL,
  action        VARCHAR(16)  NOT NULL COMMENT 'follow / unfollow',
  event_day     DATE         NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_subscribe_event_channel_day (channel_id, event_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅增减事件';

-- Content reports + notices (PRODUCT-015)
CREATE TABLE IF NOT EXISTS media_content_report (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_user_id  BIGINT       NOT NULL,
  target_type       VARCHAR(16)  NOT NULL COMMENT 'VIDEO / COMMENT',
  target_id         BIGINT       NOT NULL,
  reason            VARCHAR(512) NOT NULL,
  status            VARCHAR(16)  NOT NULL DEFAULT 'open' COMMENT 'open / resolved / rejected',
  merge_key         VARCHAR(128) NOT NULL COMMENT '合并键 reporter+type+target',
  handler_note      VARCHAR(512) NULL,
  handled_at        DATETIME     NULL,
  notify_sent       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_content_report_merge (merge_key),
  KEY idx_media_content_report_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品/评论举报';

CREATE TABLE IF NOT EXISTS media_user_notice (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  kind        VARCHAR(32)  NOT NULL,
  title       VARCHAR(128) NOT NULL,
  body        VARCHAR(1024) NOT NULL,
  ref_type    VARCHAR(32)  NULL,
  ref_id      BIGINT       NULL,
  read_flag   TINYINT(1)   NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_user_notice_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知（举报处理等）';
