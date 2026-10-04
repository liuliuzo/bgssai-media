import client from './client';

export interface CreatorJob {
  id: number;
  video_id: number;
  status: string;
  failure_reason?: string | null;
  retry_count?: number;
  renditions?: CreatorRendition[];
}

export interface CreatorRendition {
  bitrate_label: string;
  height: number;
  play_url: string;
}

export interface CreatorVideo {
  id: number;
  user_id: number;
  channel_id: number;
  title: string;
  description?: string | null;
  status: string;
  cover_url?: string | null;
  like_count?: number;
  comment_count?: number;
  job?: CreatorJob;
  renditions?: CreatorRendition[];
}

export interface CreatorChannel {
  id: number;
  user_id?: number;
  handle?: string | null;
  display_name?: string | null;
  bio?: string | null;
}

export interface CreatorComment {
  id: number;
  video_id: number;
  user_id: number;
  content: string;
  created_at?: string;
}

export interface CreatorPlayback {
  video_id: number;
  cover_url?: string | null;
  renditions: CreatorRendition[];
}

export async function fetchMyChannel() {
  const { data } = await client.get<CreatorChannel>('/community/channel/mine');
  return data;
}

export async function saveMyChannel(body: {
  display_name?: string;
  bio?: string;
  handle?: string;
}) {
  const { data } = await client.post<CreatorChannel>('/community/channel', body);
  return data;
}

export async function fetchMyVideos() {
  const { data } = await client.get<CreatorVideo[]>('/creator/videos/mine');
  return data;
}

export async function uploadCreatorVideo(input: {
  file: File;
  title?: string;
  description?: string;
}) {
  const form = new FormData();
  form.append('file', input.file);
  if (input.title) form.append('title', input.title);
  if (input.description) form.append('description', input.description);
  const { data } = await client.post<CreatorVideo>('/creator/videos', form);
  return data;
}

export async function fetchCreatorJob(jobId: number) {
  const { data } = await client.get<CreatorJob>(`/creator/jobs/${jobId}`);
  return data;
}

export async function retryCreatorJob(jobId: number) {
  const { data } = await client.post<CreatorJob>(`/creator/jobs/${jobId}/retry`);
  return data;
}

export async function fetchCreatorPlayback(videoId: number) {
  const { data } = await client.get<CreatorPlayback>(`/creator/videos/${videoId}/playback`);
  return data;
}

export async function fetchVideoComments(videoId: number) {
  const { data } = await client.get<CreatorComment[]>(`/community/videos/${videoId}/comments`);
  return data;
}

export async function addVideoComment(videoId: number, content: string) {
  const { data } = await client.post<CreatorComment>(`/community/videos/${videoId}/comments`, {
    content,
  });
  return data;
}
