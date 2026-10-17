'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const api = require('../electron/platform-api');

test('normalizeBase keeps origin only', () => {
  assert.equal(api.normalizeBase('http://127.0.0.1:8081/'), 'http://127.0.0.1:8081');
  assert.throws(() => api.normalizeBase('http://127.0.0.1:8081/api'), /协议、主机和端口/);
  assert.throws(() => api.normalizeBase('file:///tmp'), /http 或 https/);
});

test('parseEnvelope returns result and marks kicked sessions', () => {
  assert.deepEqual(api.parseEnvelope({ code: 0, message: 'ok', success: true, result: { token: 'abc' } }), { token: 'abc' });
  assert.throws(() => api.parseEnvelope({ code: 2003, message: '登录已失效', success: false, result: null }), (err) => {
    assert.equal(err.sessionEnded, true);
    assert.equal(err.message, '登录已失效');
    return true;
  });
});

test('playableHttpUrl resolves relative media paths', () => {
  assert.equal(
    api.playableHttpUrl('http://127.0.0.1:8081', '/api/creator/files/1/play.m3u8'),
    'http://127.0.0.1:8081/api/creator/files/1/play.m3u8'
  );
  assert.equal(api.playableHttpUrl('http://127.0.0.1:8081', 'https://cdn.example/a.mp4'), 'https://cdn.example/a.mp4');
  assert.throws(() => api.playableHttpUrl('http://127.0.0.1:8081', ''), /没有可播放地址/);
});

test('requestJson sends Jwttoken and rejects unknown paths', async () => {
  let seen;
  const fetchImpl = async (url, init) => {
    seen = { url, init };
    return { text: async () => JSON.stringify({ success: true, result: { list: [] } }) };
  };
  const result = await api.requestJson(fetchImpl, {
    base: 'http://127.0.0.1:8081',
    token: 'jwt-1',
    apiPath: '/api/drama/feed?page_num=1&page_size=12',
  });
  assert.deepEqual(result, { list: [] });
  assert.equal(seen.url, 'http://127.0.0.1:8081/api/drama/feed?page_num=1&page_size=12');
  assert.equal(seen.init.headers.Jwttoken, 'jwt-1');
  await assert.rejects(() => api.requestJson(fetchImpl, {
    base: 'http://127.0.0.1:8081',
    apiPath: '/api/auth/login',
  }), /路径无效/);
});
