function showError(result) {
  const box = document.getElementById('engine-error');
  if (!result || result.ok !== false) {
    box.hidden = true;
    box.innerHTML = '';
    return;
  }
  box.hidden = false;
  const steps = Array.isArray(result.steps) ? result.steps : [];
  box.innerHTML = '';
  const title = document.createElement('strong');
  title.textContent = `[${result.code}] ${result.message.split('\n')[0]}`;
  box.appendChild(title);
  if (steps.length) {
    const ol = document.createElement('ol');
    steps.forEach((s) => {
      const li = document.createElement('li');
      li.textContent = s;
      ol.appendChild(li);
    });
    box.appendChild(ol);
  }
  if (result.download) {
    const a = document.createElement('a');
    a.href = result.download;
    a.target = '_blank';
    a.textContent = result.download;
    box.appendChild(a);
  }
}

async function checkEngine() {
  const probe = await window.mediaDesktop.engine();
  showError(probe);
  return probe;
}

function renderTrackOptions(status) {
  const audioSelect = document.getElementById('audio-track');
  const subSelect = document.getElementById('subtitle-track');
  if (audioSelect && Array.isArray(status.audioTracks)) {
    audioSelect.innerHTML = '';
    status.audioTracks.forEach((t) => {
      const opt = document.createElement('option');
      opt.value = String(t.index);
      opt.textContent = t.label || `Audio ${t.index}`;
      if (t.index === status.selectedAudioTrack) opt.selected = true;
      audioSelect.appendChild(opt);
    });
  }
  if (subSelect && Array.isArray(status.subtitleTracks)) {
    subSelect.innerHTML = '<option value="-1">关闭字幕</option>';
    status.subtitleTracks.forEach((t) => {
      const opt = document.createElement('option');
      opt.value = String(t.index);
      opt.textContent = t.label || `Subtitle ${t.index}`;
      if (t.index === status.selectedSubtitleTrack) opt.selected = true;
      subSelect.appendChild(opt);
    });
  }
}

async function refreshStatus() {
  const status = await window.mediaDesktop.status();
  await window.syncBuiltinPlayer(status);
  const el = document.getElementById('status');
  const list = document.getElementById('playlist');
  if (status.preparing) {
    el.textContent = '正在准备视频，可点击停止取消。';
  } else if (status.canDecode === false) {
    el.textContent = status.decodeStatus || '引擎不可用';
  } else {
    const name = status.current ? status.current.split(/[\\/]/).pop().split('?')[0] : '';
    const state = status.playing ? '正在播放' : status.paused ? '已暂停' : status.ready ? '准备就绪' : '未播放';
    el.textContent = name ? `${name} · ${state} · ${Math.floor(status.positionSec || 0)} 秒` : '选择本地文件或平台作品开始播放';
  }
  renderTrackOptions(status);
  showError(status.lastError);
  list.innerHTML = '';
  (status.playlist || []).forEach((p, i) => {
    const li = document.createElement('li');
    li.textContent = p;
    if (i === status.index) li.className = 'active';
    list.appendChild(li);
  });
}

async function loadMatrix() {
  const matrix = await window.mediaDesktop.matrix();
  const el = document.getElementById('matrix');
  const rows = Array.isArray(matrix.verified_rows) ? matrix.verified_rows : [];
  const deploy = matrix.deployment || {};
  const lines = rows.map((row) => {
    const ext = Array.isArray(row.extensions) && row.extensions.length
      ? row.extensions.join(' ')
      : row.kind;
    return `${row.surface}\t${ext}\t${row.declared}\t${row.verification}\t${row.web_decision || ''}\t${row.id}`;
  });
  const host = deploy.media_user_host ? deploy.media_user_host : '(empty)';
  el.innerHTML = `<pre>${[
    `deployment=${deploy.status || 'UNKNOWN'} MEDIA_USER_HOST=${host}`,
    'surface\textensions\tdeclared\tverification\tweb_decision\tid',
    ...lines,
    '',
    JSON.stringify(matrix, null, 2),
  ].join('\n')}</pre>`;
}

document.getElementById('btn-open').onclick = async () => {
  const r = await window.mediaDesktop.openFiles();
  showError(r);
  await refreshStatus();
};
for (const action of ['play', 'pause', 'stop']) {
  document.getElementById(`btn-${action}`).onclick = async () => {
    showError(await window.mediaDesktop[action]());
    await refreshStatus();
  };
}
document.getElementById('btn-next').onclick = async () => {
  await window.mediaDesktop.next();
  await refreshStatus();
};
document.getElementById('btn-prev').onclick = async () => {
  await window.mediaDesktop.prev();
  await refreshStatus();
};
document.getElementById('btn-seek').onclick = async () => {
  const s = Number(document.getElementById('seek').value || 0);
  await window.mediaDesktop.seek(s);
  await refreshStatus();
};
document.getElementById('volume').oninput = (e) => {
  window.mediaDesktop.setVolume(Number(e.target.value));
};
document.getElementById('rate').onchange = (e) => {
  window.mediaDesktop.setRate(Number(e.target.value));
};
const audioSelectEl = document.getElementById('audio-track');
if (audioSelectEl) {
  audioSelectEl.onchange = async (e) => {
    await window.mediaDesktop.selectAudioTrack(Number(e.target.value));
    await refreshStatus();
  };
}
const subSelectEl = document.getElementById('subtitle-track');
if (subSelectEl) {
  subSelectEl.onchange = async (e) => {
    await window.mediaDesktop.selectSubtitleTrack(Number(e.target.value));
    await refreshStatus();
  };
}

const subDelayEl = document.getElementById('sub-delay');
if (subDelayEl) {
  subDelayEl.onchange = async (e) => {
    await window.mediaDesktop.setSubtitleDelay(Number(e.target.value));
  };
}
const audioDelayEl = document.getElementById('audio-delay');
if (audioDelayEl) {
  audioDelayEl.onchange = async (e) => {
    await window.mediaDesktop.setAudioDelay(Number(e.target.value));
  };
}
const shotBtn = document.getElementById('btn-screenshot');
if (shotBtn) {
  shotBtn.onclick = async () => {
    const video = document.getElementById('builtin-video');
    if (video && !video.hidden && video.videoWidth) {
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      canvas.getContext('2d').drawImage(video, 0, 0);
      const a = document.createElement('a');
      a.href = canvas.toDataURL('image/png');
      a.download = `bgssai-shot-${Date.now()}.png`;
      a.click();
      setStatus('已保存截图');
      return;
    }
    const r = await window.mediaDesktop.screenshot();
    setStatus(r && r.ok ? '已请求截图' : '截图失败');
  };
}
const openUrlBtn = document.getElementById('btn-open-url');
if (openUrlBtn) {
  openUrlBtn.onclick = async () => {
    const url = window.prompt('网络流地址 (http / https / .m3u8)');
    if (!url) return;
    try {
      await window.mediaDesktop.openNetwork(url.trim());
      setStatus('已打开网络流');
      await refreshStatus();
    } catch (err) {
      setStatus(err.message || '无法打开网络流');
    }
  };
}
const extSubBtn = document.getElementById('btn-ext-sub');
if (extSubBtn) {
  extSubBtn.onclick = async () => {
    const path = window.prompt('外挂字幕文件路径 (srt/vtt/ass)');
    if (!path) return;
    try {
      await window.mediaDesktop.openExternalSubtitle(path.trim());
      setStatus('已加载外挂字幕');
    } catch (err) {
      setStatus(err.message || '字幕加载失败');
    }
  };
}

loadMatrix();
checkEngine();
refreshStatus();
setInterval(refreshStatus, 2000);
