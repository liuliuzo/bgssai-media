const test = require('node:test');
const assert = require('node:assert');
const { VlcPlayer } = require('../electron/vlc-player');
const { VlcNotFoundError } = require('../electron/vlc-locator');

test('missing VLC reports that decode is unavailable and browser will not hard-decode mkv', () => {
  const player = new VlcPlayer({
    locate() {
      throw new VlcNotFoundError('win32', ['vlc.exe']);
    },
  });
  const probe = player.probeEngine();
  assert.strictEqual(probe.ok, false);
  assert.strictEqual(probe.code, 'VLC_NOT_FOUND');
  const status = player.getStatus();
  assert.strictEqual(status.canDecode, false);
  assert.strictEqual(status.browserHardDecode, false);
  assert.strictEqual(status.engine, null);
  assert.match(status.decodeStatus, /无法解码/);
  assert.match(status.decodeStatus, /mkv/);
});

test('located VLC is marked decodable without a browser fallback', () => {
  const player = new VlcPlayer({
    locate() {
      return { binary: 'C:\\VideoLAN\\VLC\\vlc.exe', source: 'registry', searched: [] };
    },
  });
  const probe = player.probeEngine();
  assert.strictEqual(probe.ok, true);
  const status = player.getStatus();
  assert.strictEqual(status.canDecode, true);
  assert.strictEqual(status.browserHardDecode, false);
  assert.match(status.engine, /vlc\.exe/);
});
