'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const os = require('node:os');
const { fileURLToPath } = require('node:url');
const { BuiltinMedia } = require('../electron/builtin-media');

const fixtures = path.resolve(__dirname, '../fixtures');
test('real bundled FFmpeg decodes each available matrix fixture', { timeout: 120000 }, async (t) => {
  const media = new BuiltinMedia();
  t.after(() => media.close());
  const files = fs.readdirSync(fixtures).filter((name) => !name.endsWith('.md') && fs.statSync(path.join(fixtures, name)).isFile());
  for (const name of files) {
    await t.test(name, async () => {
      const output = await media.prepare(path.join(fixtures, name));
      assert.ok(fs.statSync(fileURLToPath(output.playbackUrl)).size > 100);
      const info = await media.probe(fileURLToPath(output.playbackUrl));
      assert.ok(info.durationSec > 0);
      assert.ok(info.audioTracks.every((track) => track.codec === 'aac'));
    });
  }
});

test('platform HTTP video produces real playable output and temporary files are removed', async () => {
  const bytes = fs.readFileSync(path.join(fixtures, 'sample.mp4'));
  const server = http.createServer((_req, res) => {
    res.writeHead(200, { 'Content-Type': 'video/mp4', 'Content-Length': bytes.length });
    res.end(bytes);
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const media = new BuiltinMedia();
  try {
    const output = await media.prepare(`http://127.0.0.1:${server.address().port}/video.mp4`);
    const file = fileURLToPath(output.playbackUrl);
    assert.ok(fs.statSync(file).size > 100);
    await media.close();
    assert.equal(fs.existsSync(file), false);
  } finally {
    await media.close();
    server.closeAllConnections();
    await new Promise((resolve) => server.close(resolve));
  }
});

test('missing FFmpeg is an explicit failure instead of successful playback', async () => {
  const media = new BuiltinMedia({ ffmpeg: path.join(fixtures, 'missing-ffmpeg.exe') });
  try {
    await assert.rejects(() => media.prepare(path.join(fixtures, 'sample.mp4')), { code: 'MEDIA_ENGINE_MISSING' });
  } finally { await media.close(); }
});

test('real alternate audio and embedded text subtitles are selected from probed streams', async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'bgssai-track-fixture-'));
  const media = new BuiltinMedia();
  try {
    const subtitle = path.join(directory, 'caption.srt');
    fs.writeFileSync(subtitle, '1\n00:00:00,000 --> 00:00:02,000\nActual embedded caption\n');
    const input = path.join(directory, 'tracks.mkv');
    await media.run(['-hide_banner', '-loglevel', 'error', '-y', '-i', path.join(fixtures, 'sample.mp4'),
      '-i', path.join(fixtures, 'sample.wav'), '-i', subtitle, '-map', '0:v:0', '-map', '0:a:0',
      '-map', '1:a:0', '-map', '2:0', '-c:v', 'copy', '-c:a', 'aac', '-c:s', 'srt', input]);
    const output = await media.prepare(input, { audioTrack: 1, subtitleTrack: 0 });
    assert.equal(output.audioTracks.length, 2);
    assert.equal(output.subtitleTracks.length, 1);
    assert.match(fs.readFileSync(fileURLToPath(output.subtitleUrl), 'utf8'), /WEBVTT[\s\S]*Actual embedded caption/);
    assert.equal((await media.probe(fileURLToPath(output.playbackUrl))).audioTracks.length, 1);
    await assert.rejects(() => media.prepare(input, { audioTrack: 7 }), { code: 'INVALID_AUDIO_TRACK' });
  } finally {
    await media.close();
    fs.rmSync(directory, { recursive: true, force: true });
  }
});

test('a canceled preparation cannot remove the next media output after a delayed stat', async () => {
  const media = new BuiltinMedia();
  const originalStat = fs.promises.stat;
  let release;
  let reached;
  const gate = new Promise((resolve) => { release = resolve; });
  const ready = new Promise((resolve) => { reached = resolve; });
  let delayed = false;
  fs.promises.stat = async (...args) => {
    const result = await originalStat(...args);
    if (!delayed && String(args[0]).endsWith('playback.mp4')) {
      delayed = true;
      reached();
      await gate;
    }
    return result;
  };
  try {
    const first = media.prepare(path.join(fixtures, 'sample.mkv'));
    await ready;
    const second = await media.prepare(path.join(fixtures, 'sample.mp4'));
    release();
    await assert.rejects(() => first, { code: 'PLAYBACK_CANCELED' });
    assert.ok(fs.existsSync(fileURLToPath(second.playbackUrl)), 'stale cleanup must preserve the current output');
  } finally {
    release();
    fs.promises.stat = originalStat;
    await media.close();
  }
});

test('closing waits for actual FFmpeg exit before final cleanup', async () => {
  const media = new BuiltinMedia();
  const running = media.run(['-hide_banner', '-loglevel', 'error', '-re', '-i', path.join(fixtures, 'sample.mp4'), '-f', 'null', '-']).catch((err) => err);
  assert.ok(media.processes.size > 0);
  await media.close();
  await running;
  assert.equal(media.processes.size, 0);
});
