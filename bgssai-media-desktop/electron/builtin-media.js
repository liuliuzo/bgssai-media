'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { execFile, spawn } = require('node:child_process');

function binary(name) {
  const override = process.env[`BGSSAI_${name.toUpperCase()}_PATH`];
  if (override) return override;
  try {
    const bundled = name === 'ffmpeg' ? require('ffmpeg-static') : require('ffprobe-static').path;
    return bundled.replace(/app\.asar([\\/])/, 'app.asar.unpacked$1');
  } catch (_) {
    return name;
  }
}

function error(code, message) {
  return Object.assign(new Error(message), { code });
}

class BuiltinMedia {
  constructor(options = {}) {
    this.ffmpeg = options.ffmpeg || binary('ffmpeg');
    this.ffprobe = options.ffprobe || binary('ffprobe');
    this.processes = new Set();
    this.directories = new Set();
    this.generation = 0;
  }

  async probe(input) {
    input = String(input).replace(/\\/g, '/');
    const output = await new Promise((resolve, reject) => {
      const proc = execFile(this.ffprobe, ['-v', 'error', '-show_streams', '-show_format', '-of', 'json', input],
        { windowsHide: true, timeout: 30000, maxBuffer: 4 * 1024 * 1024 }, (err, stdout) => {
          this.processes.delete(proc);
          if (err) reject(error('MEDIA_PROBE_FAILED', err.code === 'ENOENT'
            ? '未找到 FFprobe，请重新安装完整桌面程序或安装 FFmpeg。'
            : '无法读取媒体，请检查文件或平台地址是否有效。'));
          else resolve(stdout);
        });
      this.processes.add(proc);
    });
    const data = JSON.parse(output);
    const streams = Array.isArray(data.streams) ? data.streams : [];
    const track = (s, index) => ({ index, streamIndex: s.index, codec: s.codec_name,
      lang: s.tags?.language || 'und', label: s.tags?.title || `${s.codec_name || '音轨'} (${s.tags?.language || 'und'})` });
    const audioTracks = streams.filter((s) => s.codec_type === 'audio').map(track);
    const subtitleTracks = streams.filter((s) => s.codec_type === 'subtitle').map(track)
      .filter((s) => ['subrip', 'ass', 'ssa', 'webvtt', 'mov_text', 'text'].includes(s.codec));
    if (!audioTracks.length && !streams.some((s) => s.codec_type === 'video')) {
      throw error('NO_MEDIA_STREAM', '文件没有可播放的音频或视频流。');
    }
    return { audioTracks, subtitleTracks, durationSec: Number(data.format?.duration) || 0 };
  }

  run(args) {
    return new Promise((resolve, reject) => {
      const proc = spawn(this.ffmpeg, args, { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
      this.processes.add(proc);
      let detail = '';
      proc.stderr.on('data', (chunk) => { detail = (detail + chunk.toString()).slice(-1500); });
      proc.on('error', (err) => {
        this.processes.delete(proc);
        reject(error('MEDIA_ENGINE_MISSING', err.code === 'ENOENT'
          ? '未找到 FFmpeg，请重新安装完整桌面程序或安装 FFmpeg。' : '无法启动媒体解码进程。'));
      });
      proc.on('close', (code) => {
        this.processes.delete(proc);
        if (code === 0) resolve();
        else reject(error('MEDIA_DECODE_FAILED', `视频解码失败，请尝试其它文件或 VLC。${detail ? ' ' + detail : ''}`));
      });
    });
  }

  async prepare(input, options = {}) {
    input = String(input).replace(/\\/g, '/');
    this.cancel();
    const generation = this.generation;
    const check = () => {
      if (generation !== this.generation) throw error('PLAYBACK_CANCELED', '播放准备已取消。');
    };
    const info = await this.probe(input);
    check();
    const audio = options.audioTrack ?? 0;
    const subtitle = options.subtitleTrack ?? -1;
    const selectedAudio = info.audioTracks.find((t) => t.index === audio);
    const selectedSubtitle = info.subtitleTracks.find((t) => t.index === subtitle);
    if (info.audioTracks.length && !selectedAudio) throw error('INVALID_AUDIO_TRACK', '无效的音轨索引。');
    if (subtitle >= 0 && !selectedSubtitle) throw error('INVALID_SUBTITLE_TRACK', '内置路径不支持这条字幕。');
    const directory = await fs.promises.mkdtemp(path.join(os.tmpdir(), 'bgssai-media-'));
    this.directories.add(directory);
    const output = path.join(directory, 'playback.mp4');
    try {
      check();
      const args = ['-hide_banner', '-loglevel', 'error', '-nostdin', '-y', '-i', input, '-map', '0:v:0?'];
      if (selectedAudio) args.push('-map', `0:${selectedAudio.streamIndex}`);
      args.push('-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '23', '-pix_fmt', 'yuv420p',
        '-vf', 'scale=trunc(iw/2)*2:trunc(ih/2)*2', '-c:a', 'aac', '-b:a', '192k', '-sn', '-movflags', '+faststart', output);
      await this.run(args);
      check();
      if ((await fs.promises.stat(output)).size === 0) throw error('MEDIA_DECODE_FAILED', '解码没有生成视频。');
      let subtitleUrl = null;
      if (selectedSubtitle) {
        const vtt = path.join(directory, 'subtitle.vtt');
        await this.run(['-hide_banner', '-loglevel', 'error', '-nostdin', '-y', '-i', input,
          '-map', `0:${selectedSubtitle.streamIndex}`, '-c:s', 'webvtt', vtt]);
        check();
        subtitleUrl = pathToFileURL(vtt).href;
      }
      check();
      await this.cleanup(directory);
      check();
      return { ...info, inputPath: input, container: path.extname(input.split('?')[0]).slice(1),
        strategy: 'ffmpeg-transcode', needsTranscode: true,
        playbackUrl: pathToFileURL(output).href, subtitleUrl, directory };
    } catch (err) {
      await this.remove(directory);
      throw err;
    }
  }

  cancel() {
    this.generation += 1;
    return Promise.all([...this.processes].map((proc) => new Promise((resolve) => {
      if (proc.exitCode !== null || proc.signalCode !== null) { resolve(); return; }
      proc.once('close', resolve);
      proc.kill();
    })));
  }

  async remove(directory) {
    try {
      await fs.promises.rm(directory, { recursive: true, force: true });
      this.directories.delete(directory);
    } catch (_) { /* A renderer may still be releasing the previous file on Windows. Retry on next session/exit. */ }
  }

  async cleanup(keep) {
    await Promise.all([...this.directories].filter((directory) => directory !== keep).map((directory) => this.remove(directory)));
  }

  async close() {
    await this.cancel();
    await this.cleanup();
  }
}

module.exports = { BuiltinMedia };
