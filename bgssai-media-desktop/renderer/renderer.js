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
  const el = document.getElementById('status');
  const list = document.getElementById('playlist');
  if (status.canDecode === false) {
    el.textContent = status.decodeStatus || '引擎不可用';
  } else {
    el.textContent = status.current
      ? `引擎 ${status.engine} (${status.mode}) - 当前: ${status.current} (位置: ${status.positionSec || 0}s)`
      : `引擎 ${status.engine || '未定位'} (${status.mode || 'builtin-html5-ffmpeg'}) - 未选择文件`;
  }
  renderTrackOptions(status);
  if (status.lastError) showError(status.lastError);
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
document.getElementById('btn-play').onclick = () => window.mediaDesktop.play();
document.getElementById('btn-pause').onclick = () => window.mediaDesktop.pause();
document.getElementById('btn-stop').onclick = () => window.mediaDesktop.stop();
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

loadMatrix();
checkEngine();
refreshStatus();
setInterval(refreshStatus, 2000);
