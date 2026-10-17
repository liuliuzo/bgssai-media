'use strict';

const path = require('path');

function normalizeBase(base) {
  const text = String(base || '').trim().replace(/\/+$/, '');
  let url;
  try {
    url = new URL(text);
  } catch (err) {
    throw Object.assign(new Error('服务地址无效'), { code: 'BAD_BASE' });
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw Object.assign(new Error('服务地址只接受 http 或 https'), { code: 'BAD_BASE' });
  }
  if (url.username || url.password || url.search || url.hash || url.pathname !== '/') {
    throw Object.assign(new Error('服务地址只填写协议、主机和端口'), { code: 'BAD_BASE' });
  }
  return url.origin;
}

function assertApiPath(apiPath) {
  if (typeof apiPath !== 'string' || !apiPath.startsWith('/api/')) {
    throw Object.assign(new Error('路径无效'), { code: 'BAD_PATH' });
  }
  if (apiPath.includes('..') || apiPath.includes('\\') || apiPath.includes('://')) {
    throw Object.assign(new Error('路径无效'), { code: 'BAD_PATH' });
  }
  const allowed = apiPath.startsWith('/api/drama/')
    || apiPath.startsWith('/api/shorts')
    || apiPath.startsWith('/api/community/videos')
    || apiPath.startsWith('/api/creator/');
  if (!allowed) {
    throw Object.assign(new Error('路径无效'), { code: 'BAD_PATH' });
  }
}

function parseEnvelope(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw Object.assign(new Error('响应不是 JSON 对象'), { code: 'BAD_ENVELOPE' });
  }
  if (body.success !== true) {
    const code = body.code;
    const err = Object.assign(new Error(body.message || '请求失败'), { code });
    if (code === 2003 || code === '2003') err.sessionEnded = true;
    throw err;
  }
  return body.result;
}

function resolveMediaUrl(base, raw) {
  const value = raw == null ? '' : String(raw).trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  if (value.startsWith('/')) return normalizeBase(base) + value;
  return '';
}

function playableHttpUrl(base, raw) {
  const resolved = resolveMediaUrl(base, raw);
  if (!resolved) {
    throw Object.assign(new Error('没有可播放地址'), { code: 'NO_URL' });
  }
  let url;
  try {
    url = new URL(resolved);
  } catch (err) {
    throw Object.assign(new Error('没有可播放地址'), { code: 'NO_URL' });
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw Object.assign(new Error('只播放 http 或 https 地址'), { code: 'NO_URL' });
  }
  return url.href;
}

function videoMime(filePath) {
  const ext = path.extname(String(filePath || '')).toLowerCase();
  const types = {
    '.mp4': 'video/mp4',
    '.mkv': 'video/x-matroska',
    '.webm': 'video/webm',
    '.mov': 'video/quicktime',
    '.avi': 'video/x-msvideo',
    '.m3u8': 'application/vnd.apple.mpegurl',
    '.ts': 'video/mp2t',
  };
  return types[ext] || 'application/octet-stream';
}

async function requestJson(fetchImpl, options) {
  const root = normalizeBase(options.base);
  if (options.trustedPath !== true) assertApiPath(options.apiPath);
  const headers = { Accept: 'application/json' };
  if (options.token) headers.Jwttoken = options.token;
  let body;
  if (options.jsonBody !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.jsonBody);
  }
  const response = await fetchImpl(root + options.apiPath, {
    method: options.method || 'GET',
    headers,
    body,
    signal: AbortSignal.timeout(options.timeoutMs || 30000),
  });
  const text = await response.text();
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (err) {
    throw Object.assign(new Error('响应不是 JSON'), { code: 'BAD_ENVELOPE' });
  }
  return parseEnvelope(parsed);
}

async function uploadVideo(fetchImpl, options) {
  const root = normalizeBase(options.base);
  if (!options.token) {
    throw Object.assign(new Error('请先登录'), { code: 'NO_TOKEN' });
  }
  const form = new FormData();
  form.append('title', options.title || '');
  form.append('description', options.description || '');
  form.append('file', new Blob([options.bytes], { type: options.mime || 'application/octet-stream' }), options.filename || 'video.mp4');
  const response = await fetchImpl(root + '/api/creator/videos', {
    method: 'POST',
    headers: { Accept: 'application/json', Jwttoken: options.token },
    body: form,
    signal: AbortSignal.timeout(options.timeoutMs || 120000),
  });
  const text = await response.text();
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (err) {
    throw Object.assign(new Error('响应不是 JSON'), { code: 'BAD_ENVELOPE' });
  }
  return parseEnvelope(parsed);
}

module.exports = {
  normalizeBase,
  assertApiPath,
  parseEnvelope,
  resolveMediaUrl,
  playableHttpUrl,
  videoMime,
  requestJson,
  uploadVideo,
};
