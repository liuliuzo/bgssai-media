import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { webPlayback, type MatrixDocument } from './matrix.ts';

const here = dirname(fileURLToPath(import.meta.url));
const copies = [
  resolve(here, '../data/format-matrix.json'),
  resolve(here, '../../../../bgssai-media-common/src/main/resources/format/format-matrix.json'),
  resolve(here, '../../../../bgssai-media-desktop/shared/format-matrix.json'),
];

const documents = copies.map((path) => readFileSync(path, 'utf8'));
const matrix = JSON.parse(documents[0]) as MatrixDocument;

test('format matrix copies stay identical', () => {
  assert.equal(documents[1], documents[0]);
  assert.equal(documents[2], documents[0]);
});

test('deployment stays undeployed with empty hosts', () => {
  assert.equal(matrix.deployment?.status, 'UNDEPLOYED');
  assert.equal(matrix.deployment?.media_user_host, '');
  assert.equal(matrix.deployment?.media_admin_host, '');
});

test('web hook matches the shared cases', () => {
  const mp4 = webPlayback('https://cdn.example/a.mp4?token=1', matrix);
  assert.equal(mp4.web_decision, 'PLAY');
  assert.equal(mp4.extension, '.mp4');
  assert.equal(mp4.verification, 'NOT_VERIFIED');

  const mkv = webPlayback('https://cdn.example/movie.MKV', matrix);
  assert.equal(mkv.web_decision, 'REJECT');
  assert.equal(mkv.matched_surface, 'desktop_vlc');

  const hls = webPlayback('https://cdn.example/live/index.m3u8', matrix);
  assert.equal(hls.web_decision, 'PLAY');
  assert.equal(hls.row_id, 'web-hls');

  const bare = webPlayback('https://cdn.example/file', matrix);
  assert.equal(bare.web_decision, 'UNKNOWN');
  assert.equal(bare.extension, '');
});

test('no verified_row claims a local sample pass', () => {
  const rows = matrix.verified_rows || [];
  assert.ok(rows.length > 0);
  for (const row of rows) {
    assert.notEqual(row.verification, 'VERIFIED_LOCAL');
    assert.equal(row.verification, 'NOT_VERIFIED');
  }
});
