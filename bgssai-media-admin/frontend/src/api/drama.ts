import request from './request';
import type {
  DramaDetailResult,
  MediaDrama,
  MediaEpisode,
  PageResult,
} from '../types';

export interface DramaPageParams {
  page_num: number;
  page_size: number;
  status?: string;
  keyword?: string;
}

export function fetchDramaPage(params: DramaPageParams) {
  return request.get<never, PageResult<MediaDrama>>('/drama/page', { params });
}

export function fetchDramaDetail(id: number) {
  return request.get<never, DramaDetailResult>('/drama/detail', {
    params: { id },
  });
}

export function createDrama(data: MediaDrama) {
  return request.post<never, MediaDrama>('/drama/create', data);
}

export function updateDrama(data: MediaDrama) {
  return request.post<never, MediaDrama>('/drama/update', data);
}

export function publishDrama(id: number, published: boolean) {
  return request.post<never, MediaDrama>('/drama/publish', { id, published });
}

export function deleteDrama(id: number) {
  return request.post<never, void>('/drama/delete', { id });
}

export function createEpisode(data: MediaEpisode) {
  return request.post<never, MediaEpisode>('/drama/episode/create', data);
}

export function updateEpisode(data: MediaEpisode) {
  return request.post<never, MediaEpisode>('/drama/episode/update', data);
}

export function deleteEpisode(id: number) {
  return request.post<never, void>('/drama/episode/delete', { id });
}
