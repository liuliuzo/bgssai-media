-- seed data for bgssai_media
USE bgssai_media;

INSERT INTO sys_user (id, username, password_hash, email, phone, role_code, status)
VALUES (1, 'admin', '$2a$10$hplq9FoOL88xHqu6yb6R8OSgxSkvHSWkI3Jz7RskAWfpph7ZapA3m', 'admin@bgssai.local', NULL, 'PLATFORM_ADMIN', 'active')
ON DUPLICATE KEY UPDATE
  password_hash = IF(password_hash IS NULL OR password_hash = '', VALUES(password_hash), password_hash),
  email = IF(email IS NULL OR email = '', VALUES(email), email),
  role_code = IF(role_code IS NULL OR role_code = '', VALUES(role_code), role_code);

INSERT INTO sys_user (id, username, password_hash, email, phone, role_code, status)
VALUES (2, 'demo', '$2a$10$48i4qKHTPoBiCNUq6zUhkuLe93c72rN7fvjRFhjKY/VJXXGtCA7qi', 'demo@bgssai.local', '13800000000', 'USER', 'active')
ON DUPLICATE KEY UPDATE
  password_hash = IF(password_hash IS NULL OR password_hash = '', VALUES(password_hash), password_hash),
  email = IF(email IS NULL OR email = '', VALUES(email), email),
  phone = IF(phone IS NULL OR phone = '', VALUES(phone), phone),
  role_code = IF(role_code IS NULL OR role_code = '', VALUES(role_code), role_code);

INSERT INTO media_drama (id, title, cover_url, description, status, source, external_ref)
VALUES (
  1,
  '示例短剧：开场',
  'https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11234',
  'MVP 样例短剧。第1集为公开 MP4，第2集为公开 HLS，便于评审验证播放器。',
  'published',
  'manual',
  NULL
)
ON DUPLICATE KEY UPDATE
  title = IF(title IS NULL OR title = '', VALUES(title), title),
  status = IF(status IS NULL OR status = '', VALUES(status), status);

INSERT INTO media_episode (id, drama_id, ep_no, title, duration_sec, media_url, storage_key, status)
VALUES (
  1, 1, 1, '第1集 MP4', 60,
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  NULL, 'published'
)
ON DUPLICATE KEY UPDATE
  media_url = IF(media_url IS NULL OR media_url = '', VALUES(media_url), media_url),
  status = IF(status IS NULL OR status = '', VALUES(status), status);

INSERT INTO media_episode (id, drama_id, ep_no, title, duration_sec, media_url, storage_key, status)
VALUES (
  2, 1, 2, '第2集 HLS', 10,
  'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
  NULL, 'published'
)
ON DUPLICATE KEY UPDATE
  media_url = IF(media_url IS NULL OR media_url = '', VALUES(media_url), media_url),
  status = IF(status IS NULL OR status = '', VALUES(status), status);

INSERT INTO media_drama (id, title, cover_url, description, status, source, external_ref)
VALUES (
  2,
  '来自 short 的样例',
  'https://peach.blender.org/wp-content/uploads/bbb-splash.png',
  '模拟 short_publish 来源的已发布短剧。',
  'published',
  'short_publish',
  'short-demo-ref-001'
)
ON DUPLICATE KEY UPDATE
  external_ref = IF(external_ref IS NULL OR external_ref = '', VALUES(external_ref), external_ref);

INSERT INTO media_episode (id, drama_id, ep_no, title, duration_sec, media_url, storage_key, status)
VALUES (
  3, 2, 1, '第1集', 15,
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
  NULL, 'published'
)
ON DUPLICATE KEY UPDATE
  media_url = IF(media_url IS NULL OR media_url = '', VALUES(media_url), media_url);

INSERT INTO media_watch_progress (id, user_id, drama_id, episode_id, position_sec)
VALUES (1, 2, 1, 1, 12)
ON DUPLICATE KEY UPDATE
  position_sec = IF(position_sec IS NULL, VALUES(position_sec), position_sec);
