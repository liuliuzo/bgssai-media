-- Additive patch: AUD20-020 creator upload/transcode + AUD20-021 channel/community (2026-09-20)
-- Not a substitute for media_episode / shorts ingest. Creator originals live in media_creator_video.
USE bgssai_media;

CREATE TABLE IF NOT EXISTS media_user_channel (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL                          COMMENT 'sys_user.id，一用户一频道',
  handle        VARCHAR(64)  NOT NULL                          COMMENT '公开 handle，唯一',
  display_name  VARCHAR(128) NOT NULL,
  bio           VARCHAR(512) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_user_channel_user (user_id),
  UNIQUE KEY uk_media_user_channel_handle (handle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户频道（对标 YouTube channel）';

CREATE TABLE IF NOT EXISTS media_creator_video (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id            BIGINT        NOT NULL                          COMMENT '上传者 sys_user.id',
  channel_id         BIGINT        NOT NULL                          COMMENT 'media_user_channel.id',
  title              VARCHAR(256)  NOT NULL,
  description        TEXT          NULL,
  status             VARCHAR(16)   NOT NULL DEFAULT 'QUEUED'         COMMENT 'QUEUED / RUNNING / READY / FAILED',
  original_filename  VARCHAR(256)  NULL,
  original_path      VARCHAR(1024) NULL                              COMMENT '本地存储路径，不对其他用户暴露',
  content_type       VARCHAR(128)  NULL,
  size_bytes         BIGINT        NULL,
  cover_path         VARCHAR(1024) NULL,
  cover_url          VARCHAR(1024) NULL,
  like_count         INT           NOT NULL DEFAULT 0,
  comment_count      INT           NOT NULL DEFAULT 0,
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_media_creator_video_user (user_id),
  KEY idx_media_creator_video_channel (channel_id),
  KEY idx_media_creator_video_status (status, created_at),
  CONSTRAINT fk_media_creator_video_channel FOREIGN KEY (channel_id) REFERENCES media_user_channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者上传成片（非剧集分集）';

CREATE TABLE IF NOT EXISTS media_transcode_job (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  video_id        BIGINT       NOT NULL,
  user_id         BIGINT       NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'QUEUED'         COMMENT 'QUEUED / RUNNING / READY / FAILED',
  failure_reason  VARCHAR(1024) NULL,
  retry_count     INT          NOT NULL DEFAULT 0,
  started_at      DATETIME     NULL,
  finished_at     DATETIME     NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_media_transcode_job_video (video_id),
  KEY idx_media_transcode_job_user (user_id),
  KEY idx_media_transcode_job_status (status, id),
  CONSTRAINT fk_media_transcode_job_video FOREIGN KEY (video_id) REFERENCES media_creator_video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步转码任务；ffmpeg 缺失必须 FAILED 不得 READY';

CREATE TABLE IF NOT EXISTS media_transcode_rendition (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id         BIGINT        NOT NULL,
  video_id       BIGINT        NOT NULL,
  bitrate_label  VARCHAR(16)   NOT NULL                          COMMENT '360p / 720p / 1080p',
  height         INT           NOT NULL,
  play_url       VARCHAR(1024) NOT NULL,
  storage_path   VARCHAR(1024) NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_transcode_rendition_video (video_id),
  KEY idx_media_transcode_rendition_job (job_id),
  CONSTRAINT fk_media_transcode_rendition_job FOREIGN KEY (job_id) REFERENCES media_transcode_job(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多码率播放地址';

CREATE TABLE IF NOT EXISTS media_channel_follow (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  follower_user_id  BIGINT   NOT NULL,
  channel_id        BIGINT   NOT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_channel_follow (follower_user_id, channel_id),
  KEY idx_media_channel_follow_channel (channel_id),
  CONSTRAINT fk_media_channel_follow_channel FOREIGN KEY (channel_id) REFERENCES media_user_channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅 / 关注频道';

CREATE TABLE IF NOT EXISTS media_video_comment (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  video_id    BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  content     VARCHAR(2000) NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_video_comment_video (video_id, id),
  KEY idx_media_video_comment_user (user_id),
  CONSTRAINT fk_media_video_comment_video FOREIGN KEY (video_id) REFERENCES media_creator_video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成片评论';

CREATE TABLE IF NOT EXISTS media_video_like (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  video_id    BIGINT   NOT NULL,
  user_id     BIGINT   NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_media_video_like (video_id, user_id),
  KEY idx_media_video_like_user (user_id),
  CONSTRAINT fk_media_video_like_video FOREIGN KEY (video_id) REFERENCES media_creator_video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成片点赞';

CREATE TABLE IF NOT EXISTS media_recommend_run (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id           BIGINT   NOT NULL,
  candidate_count   INT      NOT NULL DEFAULT 0,
  kept_count        INT      NOT NULL DEFAULT 0,
  algorithm         VARCHAR(64) NOT NULL DEFAULT 'two_stage_recall_filter' COMMENT '规则召回+过滤+排序，不是 ML',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_recommend_run_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐运行快照（规则两阶段，非机器学习）';

CREATE TABLE IF NOT EXISTS media_recommend_item (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id         BIGINT       NOT NULL,
  video_id       BIGINT       NOT NULL,
  stage          VARCHAR(16)  NOT NULL                          COMMENT 'CANDIDATE / KEPT',
  recall_reason  VARCHAR(32)  NOT NULL                          COMMENT 'FOLLOWING / RECENT',
  filter_reason  VARCHAR(32)  NULL                              COMMENT 'DUPLICATE / NOT_READY；KEPT 为空',
  sort_rank      INT          NULL,
  kept           TINYINT(1)   NOT NULL DEFAULT 0,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_media_recommend_item_run (run_id, kept, sort_rank),
  CONSTRAINT fk_media_recommend_item_run FOREIGN KEY (run_id) REFERENCES media_recommend_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐候选与过滤结果';
