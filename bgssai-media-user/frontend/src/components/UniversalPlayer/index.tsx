import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, List, Typography } from 'antd';
import videojs from 'video.js';
import type Player from 'video.js/dist/types/player';
import Hls from 'hls.js';
import 'video.js/dist/video-js.css';
import type { PlaylistItem } from '@/types/api';
import './UniversalPlayer.css';

const { Text } = Typography;

const UNSUPPORTED_EXTENSIONS = [
  '.mkv', '.avi', '.wmv', '.flv', '.rmvb', '.mov', '.mpg', '.mpeg', '.3gp', '.ts',
  '.mp3', '.m4a', '.aac', '.ogg', '.opus', '.flac', '.wav', '.ape', '.alac', '.wma',
];

const FORMAT_MATRIX = [
  { format: 'Web: MP4 / WebM / HLS', support: '浏览器短剧端（本页）' },
  { format: 'Desktop: mkv/avi/mov/… + 全音频', support: 'bgssai-media-desktop（libVLC）' },
];

function getExtension(url: string): string {
  const clean = url.split('?')[0].split('#')[0];
  const idx = clean.lastIndexOf('.');
  if (idx < 0) return '';
  return clean.slice(idx).toLowerCase();
}

function isHls(url: string): boolean {
  const ext = getExtension(url);
  return ext === '.m3u8' || url.includes('.m3u8');
}

function isUnsupported(url: string): boolean {
  const ext = getExtension(url);
  return UNSUPPORTED_EXTENSIONS.includes(ext);
}

function guessMimeType(url: string): string {
  const ext = getExtension(url);
  switch (ext) {
    case '.mp4':
      return 'video/mp4';
    case '.webm':
      return 'video/webm';
    case '.m3u8':
      return 'application/x-mpegURL';
    case '.mov':
      return 'video/mp4';
    default:
      return 'video/mp4';
  }
}

export interface UniversalPlayerProps {
  mode?: 'short' | 'file';
  src?: string;
  title?: string;
  playlist?: PlaylistItem[];
  autoPlay?: boolean;
  onEnded?: () => void;
}

export default function UniversalPlayer({
  mode = 'file',
  src,
  title,
  playlist = [],
  autoPlay = false,
  onEnded,
}: UniversalPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const playerRef = useRef<Player | null>(null);
  const hlsRef = useRef<Hls | null>(null);
  const onEndedRef = useRef(onEnded);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [unsupportedUrl, setUnsupportedUrl] = useState<string | null>(null);

  onEndedRef.current = onEnded;

  const queue: PlaylistItem[] =
    playlist.length > 0
      ? playlist
      : src
        ? [{ title: title || '当前视频', url: src }]
        : [];

  const queueRef = useRef(queue);
  queueRef.current = queue;

  const currentIndexRef = useRef(currentIndex);
  currentIndexRef.current = currentIndex;

  const currentItem = queue[currentIndex] ?? null;
  const currentUrl = currentItem?.url ?? '';

  const destroyHls = useCallback(() => {
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }
  }, []);

  const loadSource = useCallback(
    (player: Player, url: string) => {
      destroyHls();

      if (!url) return;

      if (isUnsupported(url)) {
        setUnsupportedUrl(url);
        player.pause();
        return;
      }

      setUnsupportedUrl(null);

      const videoEl = player.el().querySelector('video') as HTMLVideoElement;

      if (isHls(url) && Hls.isSupported()) {
        const hls = new Hls({ enableWorker: true });
        hls.loadSource(url);
        hls.attachMedia(videoEl);
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          if (autoPlay) player.play()?.catch(() => undefined);
        });
        hlsRef.current = hls;
        return;
      }

      if (isHls(url) && videoEl.canPlayType('application/vnd.apple.mpegurl')) {
        player.src({ src: url, type: 'application/x-mpegURL' });
      } else {
        player.src({ src: url, type: guessMimeType(url) });
      }

      if (autoPlay) {
        player.play()?.catch(() => undefined);
      }
    },
    [autoPlay, destroyHls],
  );

  useEffect(() => {
    if (!videoRef.current || playerRef.current) return;

    const player = videojs(videoRef.current, {
      controls: true,
      fluid: true,
      responsive: true,
      playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2],
      controlBar: {
        volumePanel: { inline: false },
        pictureInPictureToggle: false,
      },
    });

    playerRef.current = player;

    player.on('ended', () => {
      const idx = currentIndexRef.current;
      const list = queueRef.current;
      if (idx < list.length - 1) {
        setCurrentIndex(idx + 1);
      } else {
        onEndedRef.current?.();
      }
    });

    return () => {
      destroyHls();
      if (playerRef.current && !playerRef.current.isDisposed()) {
        playerRef.current.dispose();
        playerRef.current = null;
      }
    };
  }, [destroyHls]);

  useEffect(() => {
    const player = playerRef.current;
    if (!player || !currentUrl) return;
    loadSource(player, currentUrl);
  }, [currentUrl, loadSource]);

  const handleSelectItem = (index: number) => {
    setCurrentIndex(index);
  };

  const rootClass =
    mode === 'short'
      ? 'universal-player universal-player--short'
      : 'universal-player universal-player--file';

  return (
    <div className={rootClass}>
      <div className="universal-player__video-wrap">
        {unsupportedUrl ? (
          <div className="universal-player__unsupported">
            <div>
              <Text style={{ color: '#fff', fontSize: 16 }}>
                当前格式无法在浏览器中播放
              </Text>
              <br />
              <Text type="secondary" style={{ color: '#aaa' }}>
                {getExtension(unsupportedUrl)} 文件（如 MKV、AVI）不被 Web
                播放器支持，请使用 MP4 或 HLS 格式。
              </Text>
            </div>
          </div>
        ) : (
          <div data-vjs-player>
            <video
              ref={videoRef}
              className="video-js vjs-big-play-centered vjs-fluid"
              playsInline
            />
          </div>
        )}
      </div>

      {currentItem && (
        <div style={{ marginTop: 8 }}>
          <Text strong>{currentItem.title}</Text>
        </div>
      )}

      <Alert
        className="universal-player__matrix"
        type="info"
        showIcon
        message="格式支持说明"
        description={
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {FORMAT_MATRIX.map((row) => (
              <li key={row.format}>
                <Text strong>{row.format}</Text>
                {' — '}
                {row.support}
              </li>
            ))}
          </ul>
        }
      />

      {queue.length > 1 && (
        <List
          className="universal-player__playlist"
          size="small"
          bordered
          header={<Text strong>播放列表</Text>}
          dataSource={queue}
          renderItem={(item, index) => (
            <List.Item
              className={
                index === currentIndex
                  ? 'universal-player__playlist-item universal-player__playlist-item--active'
                  : 'universal-player__playlist-item'
              }
              onClick={() => handleSelectItem(index)}
            >
              {index + 1}. {item.title}
            </List.Item>
          )}
        />
      )}
    </div>
  );
}
