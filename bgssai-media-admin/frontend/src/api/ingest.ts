import request from './request';
import type { MediaIngestLog, PageResult } from '../types';

export interface IngestLogsParams {
  page_num: number;
  page_size: number;
}

export function fetchIngestLogs(params: IngestLogsParams) {
  return request.get<never, PageResult<MediaIngestLog>>('/ingest/logs', {
    params,
  });
}
