async function refreshStatus() {
  const status = await window.mediaDesktop.status();
  const el = document.getElementById('status');
  const list = document.getElementById('playlist');
  el.textContent = status.current
    ? `引擎 ${status.engine} · 当前: ${status.current}`
    : `引擎 ${status.engine || 'libVLC'} · 未选择文件`;
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
  await window.mediaDesktop.openFiles();
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
refreshStatus();
setInterval(refreshStatus, 2000);
