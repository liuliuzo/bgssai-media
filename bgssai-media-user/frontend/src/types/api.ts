export interface ApiResponse<T> {
  code: number;
  message: string;
  success: boolean;
  result: T;
}

export interface PageResult<T> {
  total: number;
  page_num: number;
  page_size: number;
  list: T[];
}

export interface LoginResult {
  token: string;
  user_id: number;
  username: string;
  role_code: string;
}

export interface MediaDrama {
  id: number;
  title: string;
  cover_url?: string;
  description?: string;
  status?: string;
  source?: string;
  external_ref?: string;
  created_at?: string;
  updated_at?: string;
}

export interface MediaEpisode {
  id: number;
  drama_id: number;
  ep_no: number;
  title: string;
  duration_sec?: number;
  media_url?: string;
  storage_key?: string;
  status?: string;
  created_at?: string;
  updated_at?: string;
}

export interface DramaDetailResult {
  drama: MediaDrama;
  episodes: MediaEpisode[];
}

export interface MediaWatchProgress {
  id: number;
  user_id: number;
  drama_id: number;
  episode_id: number;
  position_sec: number;
  updated_at?: string;
}

export interface PlaylistItem {
  title: string;
  url: string;
}
