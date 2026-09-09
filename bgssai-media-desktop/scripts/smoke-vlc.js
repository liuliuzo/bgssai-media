'use strict';

/**
 * Headless smoke: open each fixture with cvlc --play-and-exit for 1s.
 * Proves system libVLC can demux/decode the matrix samples on this machine.
 */
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const fixturesDir = path.join(__dirname, '..', 'fixtures');
const files = fs.readdirSync(fixturesDir).filter((f) => !f.startsWith('.'));
if (files.length === 0) {
  console.error('no fixtures');
  process.exit(1);
}

let failed = 0;
for (const file of files) {
  const full = path.join(fixturesDir, file);
  const r = spawnSync(
    'cvlc',
    ['--play-and-exit', '--run-time=1', '--intf', 'dummy', full],
    { encoding: 'utf8', timeout: 15000 }
  );
  const ok = r.status === 0;
  console.log(`${ok ? 'OK' : 'FAIL'} ${file} (exit=${r.status})`);
  if (!ok) failed += 1;
}

if (failed > 0) {
  console.error(`smoke failed: ${failed}/${files.length}`);
  process.exit(1);
}
console.log(`smoke passed: ${files.length} fixtures via libVLC/cvlc`);
