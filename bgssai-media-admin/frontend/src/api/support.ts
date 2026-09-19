import request from './request';
import type { PageResult } from '../types';

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

export interface SupportSessionRow {
  session: SupportSession;
  ticket?: SupportTicket | null;
}

export function fetchSupportPage(params: {
  page_num: number;
  page_size: number;
  status?: string;
  keyword?: string;
}) {
  return request.get<never, PageResult<SupportSessionRow>>('/support/sessions/page', { params });
}

export function fetchSupportDetail(sessionId: number) {
  return request.get<never, SupportDetail>('/support/sessions/detail', {
    params: { session_id: sessionId },
  });
}

export function replySupport(sessionId: number, content: string) {
  return request.post<never, SupportDetail>('/support/sessions/reply', {
    session_id: sessionId,
    content,
  });
}

export function closeSupport(sessionId: number) {
  return request.post<never, SupportDetail>('/support/sessions/close', {
    session_id: sessionId,
  });
}

export function markSupportRead(sessionId: number) {
  return request.post<never, SupportDetail>('/support/sessions/read', {
    session_id: sessionId,
  });
}

export function updateSupportTicketStatus(sessionId: number, ticketId: number, status: string) {
  return request.post<never, SupportDetail>('/support/sessions/ticket/status', {
    session_id: sessionId,
    ticket_id: ticketId,
    status,
  });
}
