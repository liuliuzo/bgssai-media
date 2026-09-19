const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
const { locateVlc, VlcNotFoundError } = require('../electron/vlc-locator');

test('locateVlc', async (t) => {
  await t.test('Returns env override when BGSSAI_VLC_PATH is set and file exists', () => {
    const env = { BGSSAI_VLC_PATH: '/custom/vlc' };
    const io = {
      statSync: (p) => {
        if (p === '/custom/vlc') return { isFile: () => true };
        throw new Error('enoent');
      },
      spawnSync: () => { throw new Error('should not be called'); }
    };
    const result = locateVlc({ env, io, platform: 'linux' });
    assert.strictEqual(result.binary, '/custom/vlc');
    assert.strictEqual(result.source, 'env');
  });

  await t.test('Returns bundled path when resourcesPath provided and file exists', () => {
    const resourcesPath = '/app/resources';
    const bundledPath = path.join(resourcesPath, 'vlc', 'vlc');
    const io = {
      statSync: (p) => {
        if (p === bundledPath) return { isFile: () => true };
        throw new Error('enoent');
      },
      spawnSync: () => { throw new Error('should not be called'); }
    };
    const result = locateVlc({ env: {}, io, platform: 'linux', resourcesPath });
    assert.strictEqual(result.binary, bundledPath);
    assert.strictEqual(result.source, 'bundled');
  });

  await t.test('Returns PATH lookup when vlc found on PATH', () => {
    const io = {
      statSync: (p) => {
        if (p === '/usr/bin/vlc') return { isFile: () => true };
        throw new Error('enoent');
      },
      spawnSync: (cmd, args) => {
        const expectedCmd = process.platform === 'win32' ? 'where.exe' : 'which';
        if (cmd === expectedCmd && args[0] === 'cvlc') {
          return { status: 0, stdout: '/usr/bin/vlc\n' };
        }
        return { status: 1 };
      }
    };
    const result = locateVlc({ env: {}, io, platform: 'linux' });
    assert.strictEqual(result.binary, '/usr/bin/vlc');
    assert.strictEqual(result.source, 'PATH');
  });

  await t.test('Returns registry on win32 when found', () => {
    const expectedPath = path.join('C:\\Program Files\\VideoLAN\\VLC', 'vlc.exe');
    const io = {
      statSync: (p) => {
        if (p === expectedPath) return { isFile: () => true };
        throw new Error('enoent');
      },
      spawnSync: (cmd, args) => {
        if (cmd === 'where.exe') {
          return { status: 1 };
        }
        if (cmd === 'reg.exe' && args[1] === 'HKLM\\SOFTWARE\\VideoLAN\\VLC') {
          return { status: 0, stdout: '    InstallDir    REG_SZ    C:\\Program Files\\VideoLAN\\VLC\r\n' };
        }
        return { status: 1 };
      }
    };
    const result = locateVlc({ env: {}, io, platform: 'win32' });
    assert.strictEqual(result.binary, expectedPath);
    assert.strictEqual(result.source, 'registry');
  });

  await t.test('Throws VlcNotFoundError when nothing found', () => {
    const io = {
      statSync: () => { throw new Error('enoent'); },
      spawnSync: () => ({ status: 1 })
    };
    assert.throws(() => {
      locateVlc({ env: {}, io, platform: 'linux' });
    }, (err) => {
      assert(err instanceof VlcNotFoundError);
      assert.strictEqual(err.code, 'VLC_NOT_FOUND');
      return true;
    });
  });

  await t.test('VlcNotFoundError.toJSON() returns correct structure', () => {
    const err = new VlcNotFoundError('linux', ['test1', 'test2']);
    const json = err.toJSON();
    assert.strictEqual(json.ok, false);
    assert.strictEqual(json.code, 'VLC_NOT_FOUND');
    assert.strictEqual(json.platform, 'linux');
    assert.deepStrictEqual(json.searched, ['test1', 'test2']);
    assert(Array.isArray(json.steps));
    assert(typeof json.download === 'string');
    assert.strictEqual(json.message, err.message);
  });
});
