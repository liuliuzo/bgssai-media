'use strict';

const desktop = window.mediaDesktop;

function text(el, value) {
  el.textContent = value || '';
}

function failed(result) {
  return Boolean(result && result.ok === false);
}

function clear(el) {
  el.replaceChildren();
}

function addCard(parent, title, meta, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'card';
  const strong = document.createElement('strong');
  strong.textContent = title || '未命名';
  const small = document.createElement('span');
  small.textContent = meta || '';
  button.append(strong, small);
  if (onClick) button.addEventListener('click', onClick);
  parent.append(button);
  return button;
}

function showView(name) {
  document.querySelectorAll('.view').forEach((view) => {
    view.hidden = view.id !== `view-${name}`;
  });
  document.querySelectorAll('.nav-btn').forEach((button) => {
    button.classList.toggle('is-active', button.dataset.view === name);
  });
  if (name === 'feed') loadFeed();
  if (name === 'shorts') loadShorts();
  if (name === 'creator') loadMine();
  if (name === 'community') loadCommunity();
}

async function refreshAccount() {
  const session = await desktop.session();
  const status = document.getElementById('account-status');
  const logout = document.getElementById('btn-logout');
  if (failed(session)) {
    text(status, session.message || '账号状态不可用');
    logout.hidden = true;
    return;
  }
  document.getElementById('server').value = session.base || 'http://127.0.0.1:8081';
  if (session.loggedIn) {
    text(status, `已登录 ${session.username || ''}`);
    logout.hidden = false;
  } else {
    text(status, '登录后可看正片、短视频，并在创作者中心发布作品。');
    logout.hidden = true;
  }
}

document.getElementById('account').addEventListener('submit', async (event) => {
  event.preventDefault();
  const status = document.getElementById('account-status');
  text(status, '登录中');
  const result = await desktop.login({
    base: document.getElementById('server').value,
    username: document.getElementById('username').value,
    password: document.getElementById('password').value,
  });
  document.getElementById('password').value = '';
  if (failed(result) || !result.loggedIn) {
    text(status, (result && result.message) || '登录失败');
    return;
  }
  await refreshAccount();
});

document.getElementById('btn-logout').addEventListener('click', async () => {
  await desktop.logout();
  await refreshAccount();
});

document.querySelectorAll('.nav-btn').forEach((button) => {
  button.addEventListener('click', () => showView(button.dataset.view));
});

async function playItem(rawUrl, statusEl) {
  text(statusEl, '正在准备视频');
  showView('player');
  const result = await desktop.playUrl(rawUrl);
  if (failed(result)) {
    text(statusEl, result.message || '无法播放');
    showError(result);
    return;
  }
  await refreshStatus();
}

let communityVersion = 0;
async function loadCommunity() {
  const version = ++communityVersion;
  const status = document.getElementById('community-status');
  const list = document.getElementById('community-list');
  clear(list);
  document.getElementById('community-playback').hidden = true;
  text(status, '加载中');
  const result = await desktop.get('/api/community/videos');
  if (version !== communityVersion) return;
  if (failed(result)) { text(status, result.message || '作品加载失败，请重试'); return; }
  const rows = Array.isArray(result) ? result : [];
  text(status, rows.length ? '' : '还没有已发布的作品');
  rows.forEach((video) => addCard(list, video.title, video.description || '', () => openCommunityVideo(video)));
}

async function openCommunityVideo(video) {
  const version = ++communityVersion;
  const status = document.getElementById('community-status');
  const panel = document.getElementById('community-playback');
  panel.hidden = true;
  text(status, '正在获取播放地址');
  const playback = await desktop.get(`/api/creator/videos/${encodeURIComponent(video.id)}/playback`);
  if (version !== communityVersion) return;
  if (failed(playback)) { text(status, playback.message || '作品暂不可播，请刷新重试'); return; }
  const renditions = (playback.renditions || []).filter((row) => row.play_url);
  if (!renditions.length) { text(status, '作品没有可播放的清晰度'); return; }
  text(document.getElementById('community-title'), video.title);
  text(document.getElementById('community-description'), video.description);
  const quality = document.getElementById('community-quality');
  clear(quality);
  const auto = document.createElement('option');
  auto.value = 'auto';
  auto.textContent = '自动';
  quality.append(auto);
  renditions.forEach((row, index) => {
    const option = document.createElement('option');
    option.value = String(index);
    option.textContent = row.profile || row.name || (row.height ? `${row.height}p` : `清晰度 ${index + 1}`);
    quality.append(option);
  });
  panel.hidden = false;
  text(status, '选择清晰度并播放');
  document.getElementById('btn-community-play').onclick = () => {
    let idx = quality.value === 'auto'
      ? renditions.reduce((best, row, i, arr) => {
          const h = Number(row.height) || 0;
          const bh = Number(arr[best].height) || 0;
          return h >= bh ? i : best;
        }, 0)
      : Number(quality.value);
    playItem(renditions[idx].play_url, status);
  };
}

document.getElementById('btn-community-refresh').addEventListener('click', loadCommunity);

async function loadFeed() {
  const status = document.getElementById('feed-status');
  const list = document.getElementById('feed-list');
  const episodes = document.getElementById('episode-list');
  clear(list);
  clear(episodes);
  text(status, '加载中');
  const page = await desktop.get('/api/drama/feed?page_num=1&page_size=20');
  if (failed(page)) {
    text(status, page.message || '加载失败');
    return;
  }
  const rows = Array.isArray(page.list) ? page.list : [];
  text(status, rows.length ? '' : '暂无已发布剧集');
  rows.forEach((drama) => {
    addCard(list, drama.title, drama.description || '', () => openDrama(drama.id));
  });
}

async function openDrama(id) {
  const status = document.getElementById('feed-status');
  const episodes = document.getElementById('episode-list');
  clear(episodes);
  const detail = await desktop.get(`/api/drama/detail?id=${encodeURIComponent(id)}`);
  if (failed(detail)) {
    text(status, detail.message || '剧集加载失败');
    return;
  }
  const rows = Array.isArray(detail.episodes) ? detail.episodes : [];
  text(status, rows.length ? '选择一集播放' : '这部剧还没有分集');
  rows.forEach((episode) => {
    const label = episode.title || `第 ${episode.ep_no || ''} 集`;
    addCard(episodes, label, episode.media_url ? '播放' : '没有播放地址', () => {
      playItem(episode.media_url, status);
    });
  });
}

async function loadShorts() {
  const status = document.getElementById('shorts-status');
  const list = document.getElementById('shorts-list');
  clear(list);
  text(status, '加载中');
  const page = await desktop.get('/api/shorts?page=1&page_size=20');
  if (failed(page)) {
    text(status, page.message || '加载失败');
    return;
  }
  const rows = Array.isArray(page.list) ? page.list : [];
  text(status, rows.length ? '' : '暂无短视频');
  rows.forEach((item) => {
    const url = item.play_url || item.video_url || '';
    const meta = item.playable === false ? (item.message || '暂不可播') : '播放';
    addCard(list, item.title, meta, () => {
      if (!url) {
        text(status, item.message || '没有播放地址');
        return;
      }
      playItem(url, status);
    });
  });
}

async function loadMine() {
  const status = document.getElementById('creator-status');
  const list = document.getElementById('mine-list');
  clear(list);
  const rows = await desktop.get('/api/creator/videos/mine');
  if (failed(rows)) {
    text(status, rows.message || '我的作品加载失败');
    return;
  }
  const videos = Array.isArray(rows) ? rows : [];
  if (!videos.length) addCard(list, '还没有作品', '发布后会出现在这里');
  videos.forEach((video) => {
    addCard(list, video.title, video.status || '', async () => {
      const playback = await desktop.get(`/api/creator/videos/${encodeURIComponent(video.id)}/playback`);
      if (failed(playback)) {
        text(status, playback.message || '还不能播放');
        return;
      }
      const rendition = (playback.renditions || []).find((row) => row.play_url);
      if (!rendition) {
        text(status, '转码完成后才能播放');
        return;
      }
      playItem(rendition.play_url, status);
    });
  });
}

document.getElementById('creator-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const status = document.getElementById('creator-status');
  const uploadButton = document.getElementById('btn-upload');
  if (uploadButton.disabled) return;
  uploadButton.disabled = true;
  text(status, '选择文件并上传');
  let result;
  try { result = await desktop.upload({
    title: document.getElementById('work-title').value,
    description: document.getElementById('work-description').value,
  }); } finally { uploadButton.disabled = false; }
  if (result && result.canceled) {
    text(status, '已取消选择');
    return;
  }
  if (failed(result)) {
    text(status, result.message || '发布失败');
    return;
  }
  text(status, '已提交，转码完成后可以播放');
  document.getElementById('work-title').value = '';
  document.getElementById('work-description').value = '';
  await loadMine();
});

refreshAccount();
