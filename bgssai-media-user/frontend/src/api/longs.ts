import client from './client';
import type { ShortPlayItem } from '@/types/api';

export async function fetchLongDetail(mediaId: string) {
  const { data } = await client.get<ShortPlayItem>(`/longs/${encodeURIComponent(mediaId)}`);
  return data;
}
