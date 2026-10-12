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

export interface ShortPlayItem {
  media_id: string;
  title?: string;
  cover_url?: string;
  play_url?: string | null;
  video_url?: string | null;
  status?: string;
  playable?: boolean;
  source_system?: string;
  duration_sec?: number;
  message?: string;
  idempotency_key?: string;
}

export interface ShortCatalogResult {
  list: ShortPlayItem[];
  total: number;
  page: number;
  page_size: number;
  q?: string;
}

export interface FormatHook {
  extension: string;
  web_decision: 'PLAY' | 'REJECT' | 'UNKNOWN' | string;
  verification: string;
  declared: string;
  matched_surface?: string | null;
  row_id?: string | null;
  label?: string | null;
}

export interface LibraryItem extends ShortPlayItem {
  catalog?: 'short' | 'long' | string;
  play_path?: string;
  format_hook?: FormatHook;
}

export interface LibraryResult {
  list: LibraryItem[];
  total: number;
  page: number;
  page_size: number;
  q?: string | null;
  kind?: string;
  capped?: boolean;
  recommend?: LibraryItem[];
  recommend_basis?: string;
  source_total?: number;
}

export interface ChatOauthPrepare {
  provider: string;
  admin_supported: boolean;
  enabled: boolean;
  status: 'PREP' | 'READY' | string;
  authorize_url?: string | null;
  authorize_url_template?: string | null;
  redirect_uri?: string | null;
  message?: string;
}
