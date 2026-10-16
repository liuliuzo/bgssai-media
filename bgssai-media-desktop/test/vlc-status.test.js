'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const {
  VlcPlayer,
  ENGINE_MODE_VLC,
  ENGINE_MODE_BUILTIN,
} = require('../electron/vlc-player');
const { VlcNotFoundError } = require('../electron/vlc-locator');

test('missing host VLC falls back automatically to builtin-html5-ffmpeg self-contained engine', () => {
  const player = new VlcPlayer({
    locate() {
      throw new VlcNotFoundError('win32', ['vlc.exe']);
    },
  });
  const probe = player.probeEngine();
  assert.equal(probe.ok, true);
  assert.equal(probe.code, 'BUILTIN_ENGINE_READY');
  assert.equal(probe.mode, ENGINE_MODE_BUILTIN);
  assert.equal(probe.vlcAvailable, false);

  const status = player.getStatus();
  assert.equal(status.canDecode, true);
  assert.equal(status.vlcAvailable, false);
  assert.equal(status.mode, ENGINE_MODE_BUILTIN);
  assert.equal(status.browserHardDecode, false);
  assert.match(status.engine, /builtin-html5-ffmpeg/);
  assert.match(status.decodeStatus, /builtin-html5-ffmpeg/);
});

test('located host VLC uses libvlc-rc mode and marks vlcAvailable true', () => {
  const player = new VlcPlayer({
    locate() {
      return { binary: 'C:\\VideoLAN\\VLC\\vlc.exe', source: 'registry', searched: [] };
    },
  });
  const probe = player.probeEngine();
  assert.equal(probe.ok, true);
  assert.equal(probe.code, 'VLC_FOUND');
  assert.equal(probe.mode, ENGINE_MODE_VLC);
  assert.equal(probe.vlcAvailable, true);

  const status = player.getStatus();
  assert.equal(status.canDecode, true);
  assert.equal(status.vlcAvailable, true);
  assert.equal(status.mode, ENGINE_MODE_VLC);
  assert.equal(status.browserHardDecode, false);
  assert.match(status.engine, /vlc\.exe/);
});

test('builtin engine creates playback session, seeks, switches audio/subtitle tracks, and controls playback without host VLC', async () => {
  const player = new VlcPlayer({
    locate() {
      throw new VlcNotFoundError('linux', ['cvlc', 'vlc']);
    },
  });

  const sampleMkv = path.join(__dirname, '..', 'fixtures', 'sample.mkv');
  const sampleMp4 = path.join(__dirname, '..', 'fixtures', 'sample.mp4');

  const openStatus = await player.openPlaylist([sampleMkv, sampleMp4]);
  assert.equal(openStatus.vlcAvailable, false);
  assert.equal(openStatus.mode, ENGINE_MODE_BUILTIN);
  assert.equal(openStatus.ready, true);
  assert.equal(openStatus.playing, true);
  assert.equal(openStatus.paused, false);
  assert.equal(openStatus.current, path.resolve(sampleMkv));
  assert.ok(openStatus.session);
  assert.equal(openStatus.session.container, 'mkv');
  assert.equal(openStatus.session.needsTranscode, true);
  assert.ok(openStatus.session.transcodeArgs.includes('-movflags'));
  assert.ok(openStatus.session.transcodeArgs.includes('frag_keyframe+empty_moov+default_base_moof'));

  // Seek updates positionSec and injects -ss before -i in transcodeArgs
  const seekRes = await player.seek(45);
  assert.equal(seekRes.ok, true);
  assert.equal(seekRes.positionSec, 45);
  const ssIdx = seekRes.session.transcodeArgs.indexOf('-ss');
  const iIdx = seekRes.session.transcodeArgs.indexOf('-i');
  assert.ok(ssIdx >= 0 && ssIdx < iIdx);
  assert.equal(seekRes.session.transcodeArgs[ssIdx + 1], '45');

  // Switch audio track
  const audioRes = await player.selectAudioTrack(1);
  assert.equal(audioRes.ok, true);
  assert.equal(audioRes.selectedAudioTrack, 1);
  assert.ok(audioRes.session.transcodeArgs.includes('0:a:1?'));
  assert.equal(audioRes.session.seekSec, 45);

  // Switch subtitle track and verify WebVTT extraction args
  const subRes = await player.selectSubtitleTrack(1);
  assert.equal(subRes.ok, true);
  assert.equal(subRes.selectedSubtitleTrack, 1);
  assert.ok(Array.isArray(subRes.session.subtitleExtractArgs));
  assert.ok(subRes.session.subtitleExtractArgs.includes('0:s:1'));
  assert.ok(subRes.session.subtitleExtractArgs.includes('webvtt'));

  // Disable subtitle track
  const subOff = await player.selectSubtitleTrack(-1);
  assert.equal(subOff.selectedSubtitleTrack, -1);
  assert.equal(subOff.session.subtitleExtractArgs, null);

  // Pause, resume, next, prev, stop
  const pausedStatus = await player.pause();
  assert.equal(pausedStatus.paused, true);
  assert.equal(pausedStatus.playing, false);

  const resumedStatus = await player.resume();
  assert.equal(resumedStatus.paused, false);
  assert.equal(resumedStatus.playing, true);

  const nextStatus = await player.next();
  assert.equal(nextStatus.index, 1);
  assert.equal(nextStatus.current, path.resolve(sampleMp4));
  assert.equal(nextStatus.positionSec, 0);

  const prevStatus = await player.prev();
  assert.equal(prevStatus.index, 0);
  assert.equal(prevStatus.current, path.resolve(sampleMkv));

  await player.setVolume(80);
  await player.setRate(1.5);
  const afterControls = player.getStatus();
  assert.equal(afterControls.volume, 80);
  assert.equal(afterControls.rate, 1.5);

  const stoppedStatus = await player.stop();
  assert.equal(stoppedStatus.playing, false);
  assert.equal(stoppedStatus.positionSec, 0);

  await player.shutdown();
});

test('selectAudioTrack and selectSubtitleTrack reject out-of-range track indices', async () => {
  const player = new VlcPlayer({
    locate() {
      throw new VlcNotFoundError('win32', ['vlc.exe']);
    },
  });
  await player.openPlaylist([path.join(__dirname, '..', 'fixtures', 'sample.mkv')]);

  await assert.rejects(() => player.selectAudioTrack(99), /INVALID_AUDIO_TRACK|超出范围/);
  await assert.rejects(() => player.selectSubtitleTrack(99), /INVALID_SUBTITLE_TRACK|超出范围/);
});
