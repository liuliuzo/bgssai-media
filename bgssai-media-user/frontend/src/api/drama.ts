import client from './client';
import type {
  DramaDetailResult,
  MediaDrama,
  MediaEpisode,
  PageResult,
} from '@/types/api';

export async function fetchFeed(params?: {
  page_num?: number;
  page_size?: number;
  keyword?: string;
}) {
  const { data } = await client.get<PageResult<MediaDrama>>('/drama/feed', {
    params,
  });
  return data;
}

export async function fetchDramaDetail(id: number) {
  const { data } = await client.get<DramaDetailResult>('/drama/detail', {
    params: { id },
  });
  return data;
}

export async function fetchEpisode(id: number) {
  const { data } = await client.get<MediaEpisode>('/drama/episode', {
    params: { id },
  });
  return data;
}
