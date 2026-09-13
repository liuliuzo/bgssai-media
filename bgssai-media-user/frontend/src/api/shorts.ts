import client from './client';
import type { ShortCatalogResult, ShortPlayItem } from '@/types/api';

export async function fetchShorts(params?: {
  q?: string;
  page?: number;
  page_size?: number;
}) {
  const { data } = await client.get<ShortCatalogResult>('/shorts', { params });
  return data;
}

export async function fetchShortDetail(mediaId: string) {
  const { data } = await client.get<ShortPlayItem>(`/shorts/${encodeURIComponent(mediaId)}`);
  return data;
}
