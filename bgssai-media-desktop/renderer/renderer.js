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
  title.textContent = `[${result.code}] ${result.message.split('
')[0]}`;
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

async function refreshStatus() {
  const status = await window.mediaDesktop.status();
  const el = document.getElementById('status');
  const list = document.getElementById('playlist');
  el.textContent = status.current
    ? `引擎 ${status.engine} · 当前: ${status.current}`
    : `引擎 ${status.engine || 'libVLC'} · 未选择文件`;
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
  el.innerHTML = `<pre>${JSON.stringify(matrix, null, 2)}</pre>`;
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
document.getElementById('btn-seek').onclick = () => {
  const s = Number(document.getElementById('seek').value || 0);
  window.mediaDesktop.seek(s);
};
document.getElementById('volume').oninput = (e) => {
  window.mediaDesktop.setVolume(Number(e.target.value));
};
document.getElementById('rate').onchange = (e) => {
  window.mediaDesktop.setRate(Number(e.target.value));
};

loadMatrix();
checkEngine();
refreshStatus();
setInterval(refreshStatus, 2000);
