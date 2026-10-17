'use strict';

// The HTMLVideoElement is the audio/video output of the FFmpeg fallback.
// A successful preparation alone never marks a file as playing.
(function () {
  const video = document.getElementById('builtin-video');
  let sessionId = null;
  let commandVersion = -1;
  let seekVersion = -1;
  let generation = 0;

  function report(extra = {}) {
    if (!sessionId) return;
    window.mediaDesktop.reportPlayback({ sessionId, commandVersion, readyState: video.readyState,
      positionSec: video.currentTime || 0, volume: video.volume * 100, rate: video.playbackRate,
      playing: !video.paused && !video.ended && video.readyState >= 2, paused: video.paused, ...extra });
  }
  for (const name of ['playing', 'pause', 'timeupdate', 'seeked', 'volumechange', 'ratechange']) video.addEventListener(name, () => report());
  video.addEventListener('ended', () => report({ ended: true }));
  video.addEventListener('error', () => report({ error: '视频输出失败，请重新打开文件或尝试 VLC。' }));

  window.syncBuiltinPlayer = async (status) => {
    const session = status.mode === 'builtin-html5-ffmpeg' ? status.session : null;
    if (!session?.playbackUrl) {
      if (sessionId) {
        generation += 1;
        sessionId = null;
        video.pause();
        video.removeAttribute('src');
        video.replaceChildren();
        video.load();
      }
      video.hidden = true;
      return;
    }
    video.hidden = false;
    const changed = sessionId !== session.sessionId;
    if (changed) {
      generation += 1;
      sessionId = session.sessionId;
      video.pause();
      video.replaceChildren();
      if (session.subtitleUrl) {
        const track = document.createElement('track');
        track.kind = 'subtitles';
        track.label = '字幕';
        track.srclang = 'und';
        track.default = true;
        track.src = session.subtitleUrl;
        video.append(track);
      }
      video.src = session.playbackUrl;
      video.load();
    }
    if (!changed && commandVersion === status.commandVersion) return;
    commandVersion = status.commandVersion;
    video.volume = Math.max(0, Math.min(1, status.volume / 100));
    video.playbackRate = status.rate;
    const currentGeneration = generation;
    const apply = async () => {
      if (currentGeneration !== generation || commandVersion !== status.commandVersion) return;
      if (changed || seekVersion !== status.seekVersion) {
        const target = Math.max(0, session.seekSec || 0);
        video.currentTime = Number.isFinite(video.duration) ? Math.min(target, video.duration) : target;
        seekVersion = status.seekVersion;
      }
      try {
        if (status.desiredPlaying) await video.play();
        else video.pause();
      } catch (err) {
        if (currentGeneration === generation && err.name !== 'AbortError') report({ error: '无法开始播放，请点击视频窗口的播放按钮重试。' });
      }
    };
    if (video.readyState >= 1) await apply();
    else video.addEventListener('loadedmetadata', apply, { once: true });
  };

  document.getElementById('btn-fullscreen').addEventListener('click', () => {
    if (!video.hidden) video.requestFullscreen().catch(() => {});
    else window.mediaDesktop.fullscreen();
  });
}());
