import client from './client';
import type { MediaWatchProgress } from '@/types/api';

export async function fetchContinueWatching() {
  const { data } = await client.get<MediaWatchProgress[]>('/watch/continue');
  return data;
}

export async function saveWatchProgress(
  dramaId: number,
  episodeId: number,
  positionSec: number,
) {
  const { data } = await client.post<MediaWatchProgress>('/watch/progress', {
    drama_id: dramaId,
    episode_id: episodeId,
    position_sec: positionSec,
  });
  return data;
}
