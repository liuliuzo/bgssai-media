import client from './client';

export type SupportStatus = 'open' | 'pending' | 'closed';
export type TicketStatus = 'open' | 'pending' | 'resolved' | 'closed';
export type TicketPriority = 'low' | 'normal' | 'high';

export interface SupportSession {
  id: number;
  session_token: string;
  user_id?: number | null;
  contact_name?: string | null;
  contact_email?: string | null;
  contact_phone?: string | null;
  subject?: string | null;
  status: SupportStatus;
  last_message_at?: string | null;
  user_unread?: number;
  admin_unread?: number;
  created_at?: string;
  updated_at?: string;
}

export interface SupportMessage {
  id: number;
  session_id: number;
  sender_type: 'user' | 'admin' | 'system';
  sender_user_id?: number | null;
  content: string;
  created_at?: string;
}

export interface SupportTicket {
  id: number;
  session_id: number;
  subject?: string | null;
  priority?: TicketPriority | string;
  category?: string | null;
  status: TicketStatus | string;
  description?: string | null;
  created_by_user_id?: number | null;
  created_at?: string;
  updated_at?: string;
}

export interface SupportDetail {
  session: SupportSession;
  messages: SupportMessage[];
  ticket?: SupportTicket | null;
}

const STORAGE_KEY = 'bgssai_media_support_session';

export function loadStoredSession(): { session_id: number; session_token: string } | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { session_id?: number; session_token?: string };
    if (!parsed.session_id || !parsed.session_token) return null;
    return { session_id: parsed.session_id, session_token: parsed.session_token };
  } catch {
    return null;
  }
}

export function storeSession(session: SupportSession) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({ session_id: session.id, session_token: session.session_token }),
  );
}

export function clearStoredSession() {
  localStorage.removeItem(STORAGE_KEY);
}

export async function createSupportSession(payload: {
  content: string;
  subject?: string;
  contact_name?: string;
  contact_email?: string;
  contact_phone?: string;
}): Promise<SupportDetail> {
  const { data } = await client.post<SupportDetail>('/support/sessions', payload);
  return data;
}

export async function getSupportDetail(params: {
  session_id?: number;
  session_token?: string;
}): Promise<SupportDetail> {
  const { data } = await client.get<SupportDetail>('/support/sessions/detail', {
    params,
  });
  return data;
}

export async function sendSupportMessage(payload: {
  session_id?: number;
  session_token?: string;
  content: string;
}): Promise<SupportDetail> {
  const { data } = await client.post<SupportDetail>('/support/sessions/messages', payload);
  return data;
}

export async function markSupportRead(payload: {
  session_id?: number;
  session_token?: string;
}): Promise<SupportDetail> {
  const { data } = await client.post<SupportDetail>('/support/sessions/read', payload);
  return data;
}

export async function raiseSupportTicket(payload: {
  session_id?: number;
  session_token?: string;
  subject?: string;
  priority?: TicketPriority | string;
  category?: string;
  description?: string;
}): Promise<SupportDetail> {
  const { data } = await client.post<SupportDetail>('/support/sessions/ticket', payload);
  return data;
}

export async function listMySupportSessions(): Promise<SupportSession[]> {
  const { data } = await client.get<SupportSession[]>('/support/sessions/mine');
  return data;
}
