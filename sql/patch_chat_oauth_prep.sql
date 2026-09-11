-- Additive comment only. No DML secrets. Chat identity is reserved for the user app.
USE bgssai_media;
ALTER TABLE user_identity
  MODIFY COLUMN provider VARCHAR(20) NOT NULL COMMENT 'WECHAT / DOUYIN / BAIDU / ALIPAY / CHAT';
