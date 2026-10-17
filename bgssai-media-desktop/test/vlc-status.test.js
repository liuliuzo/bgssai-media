'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const { fileURLToPath } = require('node:url');
const { VlcPlayer, ENGINE_MODE_VLC, ENGINE_MODE_BUILTIN } = require('../electron/vlc-player');
const { VlcNotFoundError } = require('../electron/vlc-locator');
const missingVlc = () => { throw new VlcNotFoundError('win32', []); };

test('built-in playback prepares actual media and uses renderer events for playing state', async (t) => {
  const player = new VlcPlayer({ locate: missingVlc });
  t.after(() => player.shutdown());
  const status = await player.openPlaylist([path.resolve(__dirname, '../fixtures/sample.mkv')]);
  assert.equal(status.mode, ENGINE_MODE_BUILTIN);
  assert.equal(status.ready, true);
  assert.equal(status.playing, false);
  assert.equal(status.desiredPlaying, true);
  assert.ok(fs.statSync(fileURLToPath(status.session.playbackUrl)).size > 100);
  assert.equal(status.audioTracks.length, 1);
  assert.deepEqual(status.subtitleTracks, []);
  const id = status.session.sessionId;
  player.reportPlayback({ sessionId: id, playing: true, positionSec: 1.2 });
  assert.equal(player.getStatus().playing, true);
  assert.equal(player.getStatus().positionSec, 1.2);
  player.reportPlayback({ sessionId: 'old-session', playing: false, positionSec: 0 });
  assert.equal(player.getStatus().positionSec, 1.2);
  await player.pause();
  assert.equal(player.getStatus().desiredPlaying, false);
  await player.seek(2);
  assert.equal(player.getStatus().session.seekSec, 2);
  await assert.rejects(() => player.selectAudioTrack(1), /超出范围/);
  await assert.rejects(() => player.selectSubtitleTrack(0), /超出范围/);
  await player.stop();
  assert.equal(player.getStatus().playing, false);
  assert.equal(player.getStatus().session, null);
});

test('located VLC stays selected', () => {
  const player = new VlcPlayer({ locate: () => ({ binary: 'vlc.exe', source: 'test' }) });
  assert.equal(player.probeEngine().mode, ENGINE_MODE_VLC);
});

test('invalid media never reports ready or playing', async (t) => {
  const player = new VlcPlayer({ locate: missingVlc });
  t.after(() => player.shutdown());
  await assert.rejects(() => player.openPlaylist([path.join(__dirname, 'nonexistent.mkv')]));
  const status = player.getStatus();
  assert.equal(status.playing, false);
  assert.equal(status.ready, false);
  assert.equal(status.preparing, false);
  assert.equal(status.session, null);
  assert.equal(status.lastError.code, 'MEDIA_PROBE_FAILED');
});

test('stop rejects late preparation and prevents stale session publication', async () => {
  let complete;
  const builtin = { prepare: () => new Promise((resolve) => { complete = resolve; }), cancel() {}, cleanup() {}, close() {} };
  const player = new VlcPlayer({ locate: missingVlc, builtin });
  const opening = player.openPlaylist(['selected.mp4']);
  await new Promise((resolve) => setImmediate(resolve));
  await player.stop();
  complete({ audioTracks: [], subtitleTracks: [], playbackUrl: 'file:///unused.mp4' });
  await assert.rejects(() => opening, { code: 'PLAYBACK_CANCELED' });
  assert.equal(player.getStatus().session, null);
  assert.equal(player.getStatus().ready, false);
});
