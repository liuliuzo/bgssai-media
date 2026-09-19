'use strict';

/**
 * Headless smoke: open each fixture with the located VLC binary (--play-and-exit, 1s).
 * Proves system libVLC can demux/decode matrix samples on this machine.
 * Exit codes: 0 all fixtures ok; 1 some fixture failed; 2 VLC not found (install steps printed).
 */
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { locateVlc, VlcNotFoundError } = require('../electron/vlc-locator');

let vlc;
try {
  vlc = locateVlc();
} catch (err) {
  if (err instanceof VlcNotFoundError) {
    console.error(`VLC_NOT_FOUND: ${err.message}`);
    console.error(`searched: ${err.searched.join(' | ')}`);
    process.exit(2);
  }
  throw err;
}
console.log(`engine: ${vlc.binary} (${vlc.source})`);

const fixturesDir = path.join(__dirname, '..', 'fixtures');

const MEDIA_EXT = new Set([
  '.mp4', '.mkv', '.webm', '.mov', '.avi', '.flv', '.ts', '.m3u8', '.mpg', '.mpeg', '.wmv', '.3gp',
  '.mp3', '.m4a', '.aac', '.ogg', '.opus', '.flac', '.wav', '.ape', '.alac', '.wma',
]);

function collect(dir) {
  const out = [];
  for (const name of fs.readdirSync(dir)) {
    if (name.startsWith('.')) continue;
    const full = path.join(dir, name);
    const st = fs.statSync(full);
    if (st.isDirectory()) out.push(...collect(full));
    else if (MEDIA_EXT.has(path.extname(name).toLowerCase())) out.push(full);
  }
  return out;
}

const files = collect(fixturesDir);
if (files.length === 0) {
  console.error('no fixtures');
  process.exit(1);
}

let failed = 0;
for (const full of files) {
  const rel = path.relative(fixturesDir, full);
  const r = spawnSync(
    vlc.binary,
    ['--play-and-exit', '--run-time=1', '--intf', 'dummy', full],
    { encoding: 'utf8', timeout: 20000 }
  );
  const ok = r.status === 0;
  console.log(`${ok ? 'OK' : 'FAIL'} ${rel} (exit=${r.status})`);
  if (!ok) failed += 1;
}

if (failed > 0) {
  console.error(`smoke failed: ${failed}/${files.length}`);
  process.exit(1);
}
console.log(`smoke passed: ${files.length} fixtures via ${vlc.binary}`);
