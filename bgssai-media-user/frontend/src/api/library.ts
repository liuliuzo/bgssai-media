import client from './client';
import type { LibraryResult } from '@/types/api';

export async function fetchLibrary(params?: {
  q?: string;
  kind?: string;
  page?: number;
  page_size?: number;
}) {
  const { data } = await client.get<LibraryResult>('/library', { params });
  return data;
}
