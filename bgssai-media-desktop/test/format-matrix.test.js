'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const { hook } = require('../shared/format-hook');

const copies = [
  path.join(__dirname, '../shared/format-matrix.json'),
  path.join(__dirname, '../../bgssai-media-common/src/main/resources/format/format-matrix.json'),
  path.join(__dirname, '../../bgssai-media-user/frontend/src/data/format-matrix.json'),
];

const texts = copies.map((file) => fs.readFileSync(file, 'utf8'));
const matrix = JSON.parse(texts[0]);

test('format matrix copies stay identical', () => {
  assert.equal(texts[1], texts[0]);
  assert.equal(texts[2], texts[0]);
});

test('deployment stays undeployed with empty hosts', () => {
  assert.equal(matrix.deployment.status, 'UNDEPLOYED');
  assert.equal(matrix.deployment.media_user_host, '');
  assert.equal(matrix.deployment.media_admin_host, '');
});

test('hook matches the shared cases', () => {
  const mp4 = hook(matrix, 'https://cdn.example/a.mp4?token=1');
  assert.equal(mp4.web_decision, 'PLAY');
  assert.equal(mp4.extension, '.mp4');
  assert.equal(mp4.verification, 'NOT_VERIFIED');

  const mkv = hook(matrix, 'https://cdn.example/movie.MKV');
  assert.equal(mkv.web_decision, 'REJECT');
  assert.equal(mkv.matched_surface, 'desktop_vlc');

  const hls = hook(matrix, 'https://cdn.example/live/index.m3u8');
  assert.equal(hls.web_decision, 'PLAY');
  assert.equal(hls.row_id, 'web-hls');

  const bare = hook(matrix, 'https://cdn.example/file');
  assert.equal(bare.web_decision, 'UNKNOWN');
});

test('no row is marked verified from a sample pass', () => {
  for (const row of matrix.verified_rows) {
    assert.equal(row.verification, 'NOT_VERIFIED');
  }
});
