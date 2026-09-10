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

-- -----------------------------------------------------------------------------
-- 登录通道配置：空种子，enabled=0，运营在管理端补齐凭据后再启用。
-- ON DUPLICATE KEY UPDATE 只做空值回填：重复执行 DML 不会覆盖控制台已填的真实凭据。
-- enabled 同理——只有在凭据仍为空时才允许被种子改回 0，避免把线上已启用的通道关掉。
-- 真实密钥一律不入库种子、不进 Git。
-- -----------------------------------------------------------------------------
INSERT INTO platform_email_config
  (id, smtp_host, smtp_port, smtp_username, smtp_password, from_address, from_name,
   ssl_enable, starttls_enable, auth_enable, enabled)
VALUES (1, '', 465, '', '', '', 'BGSSAI Media', 1, 0, 1, 0)
ON DUPLICATE KEY UPDATE
  smtp_host = IF(smtp_host IS NULL OR smtp_host = '', VALUES(smtp_host), smtp_host),
  smtp_username = IF(smtp_username IS NULL OR smtp_username = '', VALUES(smtp_username), smtp_username),
  smtp_password = IF(smtp_password IS NULL OR smtp_password = '', VALUES(smtp_password), smtp_password),
  from_address = IF(from_address IS NULL OR from_address = '', VALUES(from_address), from_address),
  from_name = IF(from_name IS NULL OR from_name = '', VALUES(from_name), from_name),
  enabled = IF(
    (smtp_host IS NULL OR smtp_host = '')
      AND (smtp_username IS NULL OR smtp_username = '')
      AND (smtp_password IS NULL OR smtp_password = ''),
    VALUES(enabled), enabled);

-- 对齐 BGSSAI 共享腾讯云控制台：SignName=昆山兵贵神速智能科技、Login OTP=2677885
-- (禁止待审 2728085)。SecretId / SecretKey / SdkAppId 空种子，Admin 运行期填齐后启用。
INSERT INTO platform_sms_config
  (id, provider, secret_id, secret_key, sdk_app_id, sign_name, region,
   login_template_id, bind_phone_template_id, reset_password_template_id,
   code_expire_seconds, enabled)
VALUES
  (1, 'TENCENT', '', '', '', '昆山兵贵神速智能科技', 'ap-guangzhou',
   '2677885', '', '', 300, 0)
ON DUPLICATE KEY UPDATE
  secret_id = IF(secret_id IS NULL OR secret_id = '', VALUES(secret_id), secret_id),
  secret_key = IF(secret_key IS NULL OR secret_key = '', VALUES(secret_key), secret_key),
  sdk_app_id = IF(sdk_app_id IS NULL OR sdk_app_id = '', VALUES(sdk_app_id), sdk_app_id),
  sign_name = IF(sign_name IS NULL OR sign_name = '', VALUES(sign_name), sign_name),
  login_template_id = IF(login_template_id IS NULL OR login_template_id = '', VALUES(login_template_id), login_template_id),
  bind_phone_template_id = IF(bind_phone_template_id IS NULL OR bind_phone_template_id = '', VALUES(bind_phone_template_id), bind_phone_template_id),
  reset_password_template_id = IF(reset_password_template_id IS NULL OR reset_password_template_id = '', VALUES(reset_password_template_id), reset_password_template_id),
  enabled = IF(
    (secret_id IS NULL OR secret_id = '')
      AND (secret_key IS NULL OR secret_key = '')
      AND (sdk_app_id IS NULL OR sdk_app_id = ''),
    VALUES(enabled), enabled);
