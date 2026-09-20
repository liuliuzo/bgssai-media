'use strict';

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

/**
 * Locates a VLC / libVLC command-line binary on the current platform.
 *
 * Search order (first hit wins):
 *   1. env BGSSAI_VLC_PATH (explicit override, must point at an executable file)
 *   2. bundled engine next to the packaged app: <resourcesPath>/vlc/<binary>
 *   3. platform lookup
 *      - win32:  where.exe vlc.exe  ->  HKLM\SOFTWARE\VideoLAN\VLC InstallDir  ->  %ProgramFiles%\VideoLAN\VLC
 *      - darwin: /Applications/VLC.app  ->  ~/Applications/VLC.app  ->  `which vlc`
 *      - linux:  cvlc / vlc on PATH  ->  /usr/bin, /snap/bin, /var/lib/flatpak exports
 *
 * Nothing here is tied to the Linux-only `cvlc` wrapper: on every platform the plain
 * `vlc` binary is acceptable because the RC interface is selected with `--intf rc`.
 */

const ERROR_CODE = 'VLC_NOT_FOUND';

const INSTALL_HINTS = {
  win32: {
    steps: [
      '下载并安装 VLC（https://www.videolan.org/vlc/download-windows.html），或在 PowerShell 执行：winget install VideoLAN.VLC',
      '安装完成后重新打开本播放器；无需手动修改 PATH，程序会从注册表和 Program Files 自动找到 vlc.exe',
      '若 VLC 安装在自定义目录，设置环境变量 BGSSAI_VLC_PATH 指向 vlc.exe',
    ],
    download: 'https://www.videolan.org/vlc/download-windows.html',
  },
  darwin: {
    steps: [
      '下载并安装 VLC（https://www.videolan.org/vlc/download-macosx.html），或执行：brew install --cask vlc',
      '安装到 /Applications/VLC.app 后重新打开本播放器',
      '若安装在其它位置，设置环境变量 BGSSAI_VLC_PATH 指向 VLC.app/Contents/MacOS/VLC',
    ],
    download: 'https://www.videolan.org/vlc/download-macosx.html',
  },
  linux: {
    steps: [
      '执行：sudo apt install -y vlc（Debian/Ubuntu）或 sudo dnf install vlc（Fedora），或 snap install vlc',
      '安装后重新打开本播放器；程序会在 PATH、/usr/bin、/snap/bin、flatpak 导出目录中查找 vlc/cvlc',
      '若使用自编译 VLC，设置环境变量 BGSSAI_VLC_PATH 指向 vlc 可执行文件',
    ],
    download: 'https://www.videolan.org/vlc/#download',
  },
};

class VlcNotFoundError extends Error {
  constructor(platform, searched) {
    const hint = INSTALL_HINTS[platform] || INSTALL_HINTS.linux;
    super(`未找到 VLC 解码引擎（平台 ${platform}）。请按以下步骤安装后重试：\n- ${hint.steps.join('\n- ')}`);
    this.name = 'VlcNotFoundError';
    this.code = ERROR_CODE;
    this.platform = platform;
    this.searched = searched;
    this.steps = hint.steps;
    this.download = hint.download;
  }

  toJSON() {
    return {
      ok: false,
      code: this.code,
      message: this.message,
      platform: this.platform,
      steps: this.steps,
      download: this.download,
      searched: this.searched,
    };
  }
}

function isExecutableFile(p, io) {
  if (!p) return false;
  try {
    const st = io.statSync(p);
    return st.isFile();
  } catch (_) {
    return false;
  }
}

function fromPath(binary, io) {
  const lookup = process.platform === 'win32' ? 'where.exe' : 'which';
  try {
    const r = io.spawnSync(lookup, [binary], { encoding: 'utf8', timeout: 5000 });
    if (r.status !== 0 || !r.stdout) return null;
    const first = r.stdout.split(/\r?\n/).map((s) => s.trim()).find(Boolean);
    return first && isExecutableFile(first, io) ? first : null;
  } catch (_) {
    return null;
  }
}

function fromWindowsRegistry(io) {
  const keys = [
    'HKLM\\SOFTWARE\\VideoLAN\\VLC',
    'HKLM\\SOFTWARE\\WOW6432Node\\VideoLAN\\VLC',
    'HKCU\\SOFTWARE\\VideoLAN\\VLC',
  ];
  for (const key of keys) {
    try {
      const r = io.spawnSync('reg.exe', ['query', key, '/v', 'InstallDir'], { encoding: 'utf8', timeout: 5000 });
      if (r.status !== 0 || !r.stdout) continue;
      const m = r.stdout.match(/InstallDir\s+REG_SZ\s+(.+)/i);
      if (!m) continue;
      const candidate = path.join(m[1].trim(), 'vlc.exe');
      if (isExecutableFile(candidate, io)) return candidate;
    } catch (_) {
      // registry not readable; keep going
    }
  }
  return null;
}

function candidatesFor(platform, env, resourcesPath) {
  const out = [];
  if (resourcesPath) {
    const bundled = platform === 'win32'
      ? path.join(resourcesPath, 'vlc', 'vlc.exe')
      : platform === 'darwin'
        ? path.join(resourcesPath, 'vlc', 'VLC.app', 'Contents', 'MacOS', 'VLC')
        : path.join(resourcesPath, 'vlc', 'vlc');
    out.push({ kind: 'bundled', file: bundled });
  }
  if (platform === 'win32') {
    const roots = [env.ProgramFiles, env['ProgramFiles(x86)'], env.ProgramW6432, env.LOCALAPPDATA && path.join(env.LOCALAPPDATA, 'Programs')]
      .filter(Boolean);
    for (const root of roots) {
      out.push({ kind: 'program-files', file: path.join(root, 'VideoLAN', 'VLC', 'vlc.exe') });
    }
  } else if (platform === 'darwin') {
    out.push({ kind: 'applications', file: '/Applications/VLC.app/Contents/MacOS/VLC' });
    if (env.HOME) out.push({ kind: 'applications', file: path.join(env.HOME, 'Applications', 'VLC.app', 'Contents', 'MacOS', 'VLC') });
  } else {
    for (const f of ['/usr/bin/cvlc', '/usr/bin/vlc', '/usr/local/bin/cvlc', '/usr/local/bin/vlc', '/snap/bin/vlc',
      '/var/lib/flatpak/exports/bin/org.videolan.VLC']) {
      out.push({ kind: 'fixed', file: f });
    }
  }
  return out;
}

/**
 * @param {object} [opts]
 * @param {string} [opts.platform]      defaults to process.platform
 * @param {object} [opts.env]           defaults to process.env
 * @param {string} [opts.resourcesPath] Electron process.resourcesPath when packaged
 * @param {object} [opts.io]            { statSync, spawnSync } injection for tests
 * @returns {{ binary: string, source: string, searched: string[] }}
 * @throws {VlcNotFoundError}
 */
function locateVlc(opts = {}) {
  const platform = opts.platform || process.platform;
  const env = opts.env || process.env;
  const io = { statSync: fs.statSync, spawnSync, ...(opts.io || {}) };
  const searched = [];

  const override = env.BGSSAI_VLC_PATH;
  if (override) {
    searched.push(`env BGSSAI_VLC_PATH=${override}`);
    if (isExecutableFile(override, io)) return { binary: override, source: 'env', searched };
  }

  for (const c of candidatesFor(platform, env, opts.resourcesPath)) {
    if (c.kind !== 'bundled') continue;
    searched.push(`${c.kind}:${c.file}`);
    if (isExecutableFile(c.file, io)) return { binary: c.file, source: c.kind, searched };
  }

  const pathNames = platform === 'win32' ? ['vlc.exe', 'vlc'] : platform === 'darwin' ? ['vlc'] : ['cvlc', 'vlc'];
  for (const name of pathNames) {
    searched.push(`PATH:${name}`);
    const hit = fromPath(name, io);
    if (hit) return { binary: hit, source: 'PATH', searched };
  }

  if (platform === 'win32') {
    searched.push('registry:HKLM/HKCU SOFTWARE\\VideoLAN\\VLC InstallDir');
    const reg = fromWindowsRegistry(io);
    if (reg) return { binary: reg, source: 'registry', searched };
  }

  for (const c of candidatesFor(platform, env, opts.resourcesPath)) {
    if (c.kind === 'bundled') continue;
    searched.push(`${c.kind}:${c.file}`);
    if (isExecutableFile(c.file, io)) return { binary: c.file, source: c.kind, searched };
  }

  throw new VlcNotFoundError(platform, searched);
}

/** Extra args so the RC interface stays headless on every platform (cvlc semantics without cvlc). */
function headlessArgs(platform) {
  const args = ['--intf', 'rc', '--no-one-instance', '--quiet'];
  if (platform === 'win32') args.push('--rc-quiet');
  return args;
}

module.exports = { locateVlc, headlessArgs, VlcNotFoundError, ERROR_CODE, INSTALL_HINTS };
