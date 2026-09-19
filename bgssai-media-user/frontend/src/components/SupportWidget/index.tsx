import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Input, Modal, Select, Spin, Typography, message } from 'antd';
import { CustomerServiceOutlined, CloseOutlined, FormOutlined } from '@ant-design/icons';
import {
  clearStoredSession,
  createSupportSession,
  getSupportDetail,
  loadStoredSession,
  markSupportRead,
  raiseSupportTicket,
  sendSupportMessage,
  storeSession,
  type SupportDetail,
  type SupportMessage,
  type TicketPriority,
} from '@/api/support';
import './SupportWidget.css';

const { TextArea } = Input;
const { Text } = Typography;

function formatTime(value?: string) {
  if (!value) return '';
  try {
    return new Date(value).toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
}

function ticketStatusLabel(status?: string) {
  if (status === 'open') return '工单进行中';
  if (status === 'pending') return '工单待处理';
  if (status === 'resolved') return '工单已解决';
  if (status === 'closed') return '工单已关闭';
  return status ? `工单：${status}` : '';
}

function isActiveTicket(status?: string) {
  return status === 'open' || status === 'pending';
}

export default function SupportWidget() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [detail, setDetail] = useState<SupportDetail | null>(null);
  const [draft, setDraft] = useState('');
  const [ticketOpen, setTicketOpen] = useState(false);
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketPriority, setTicketPriority] = useState<TicketPriority>('normal');
  const [ticketSubmitting, setTicketSubmitting] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const applyDetail = useCallback((next: SupportDetail) => {
    setDetail(next);
    storeSession(next.session);
  }, []);

  const refresh = useCallback(async () => {
    const stored = loadStoredSession();
    if (!stored) return;
    setLoading(true);
    try {
      const next = await getSupportDetail(stored);
      applyDetail(next);
      if ((next.session.user_unread || 0) > 0) {
        await markSupportRead(stored);
        const cleared = await getSupportDetail(stored);
        applyDetail(cleared);
      }
    } catch {
      clearStoredSession();
      setDetail(null);
    } finally {
      setLoading(false);
    }
  }, [applyDetail]);

  useEffect(() => {
    if (open) {
      void refresh();
    }
  }, [open, refresh]);

  useEffect(() => {
    if (!open) return;
    const timer = window.setInterval(() => {
      void refresh();
    }, 15000);
    return () => window.clearInterval(timer);
  }, [open, refresh]);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [detail?.messages?.length, open]);

  const handleSend = async () => {
    const content = draft.trim();
    if (!content) {
      message.warning('请输入留言内容');
      return;
    }
    setSending(true);
    try {
      let next: SupportDetail;
      const closed = detail?.session.status === 'closed';
      if (!detail || closed) {
        if (closed) clearStoredSession();
        next = await createSupportSession({ content });
      } else {
        next = await sendSupportMessage({
          session_id: detail.session.id,
          session_token: detail.session.session_token,
          content,
        });
      }
      applyDetail(next);
      setDraft('');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    } finally {
      setSending(false);
    }
  };

  const openTicketModal = () => {
    if (!detail || detail.session.status === 'closed') {
      message.info('请先发送一条留言开始会话');
      return;
    }
    if (detail.ticket && isActiveTicket(detail.ticket.status)) {
      message.info(`已有工单 #${detail.ticket.id}（${ticketStatusLabel(detail.ticket.status)}）`);
      return;
    }
    setTicketSubject(detail.session.subject || '');
    setTicketPriority('normal');
    setTicketOpen(true);
  };

  const handleRaiseTicket = async () => {
    if (!detail || detail.session.status === 'closed') return;
    setTicketSubmitting(true);
    try {
      const next = await raiseSupportTicket({
        session_id: detail.session.id,
        session_token: detail.session.session_token,
        subject: ticketSubject.trim() || undefined,
        priority: ticketPriority,
      });
      applyDetail(next);
      setTicketOpen(false);
      message.success(next.ticket ? `已提工单 #${next.ticket.id}` : '已提工单');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '提工单失败');
    } finally {
      setTicketSubmitting(false);
    }
  };

  const unread = detail?.session.user_unread || 0;
  const messages: SupportMessage[] = detail?.messages || [];
  const closed = detail?.session.status === 'closed';
  const ticket = detail?.ticket;
  const canRaiseTicket = Boolean(detail && !closed && (!ticket || !isActiveTicket(ticket.status)));

  return (
    <div className="support-widget">
      {open ? (
        <div className="support-panel" role="dialog" aria-label="在线客服">
          <div className="support-panel__header">
            <div>
              <div className="support-panel__title">在线客服</div>
              <Text type="secondary" className="support-panel__sub">
                {closed
                  ? '会话已关闭，发送将开启新会话'
                  : ticket
                    ? ticketStatusLabel(ticket.status)
                    : detail
                      ? `状态：${detail.session.status}`
                      : '直接留言，客服会尽快回复'}
              </Text>
            </div>
            <Button
              type="text"
              icon={<CloseOutlined />}
              aria-label="关闭"
              onClick={() => setOpen(false)}
            />
          </div>

          <div className="support-panel__toolbar">
            <Button
              size="small"
              icon={<FormOutlined />}
              disabled={!canRaiseTicket}
              onClick={openTicketModal}
            >
              提工单
            </Button>
            {ticket ? (
              <Text type="secondary" className="support-panel__ticket-hint">
                #{ticket.id} · {ticket.status}
              </Text>
            ) : (
              <Text type="secondary" className="support-panel__ticket-hint">
                需要正式跟进时可提工单
              </Text>
            )}
          </div>

          <div className="support-panel__body" ref={listRef}>
            {loading && !detail ? (
              <div className="support-panel__empty">
                <Spin />
              </div>
            ) : messages.length === 0 ? (
              <div className="support-panel__empty">
                <Text type="secondary">发送一条留言开始会话</Text>
              </div>
            ) : (
              messages.map((msg) => (
                <div
                  key={msg.id}
                  className={
                    msg.sender_type === 'user'
                      ? 'support-bubble support-bubble--user'
                      : msg.sender_type === 'admin'
                        ? 'support-bubble support-bubble--admin'
                        : 'support-bubble support-bubble--system'
                  }
                >
                  <div className="support-bubble__meta">
                    {msg.sender_type === 'user'
                      ? '我'
                      : msg.sender_type === 'admin'
                        ? '客服'
                        : '系统'}
                    <span>{formatTime(msg.created_at)}</span>
                  </div>
                  <div className="support-bubble__content">{msg.content}</div>
                </div>
              ))
            )}
          </div>

          <div className="support-panel__composer">
            <TextArea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={closed ? '输入内容开启新会话…' : '输入留言…'}
              autoSize={{ minRows: 2, maxRows: 4 }}
              maxLength={4000}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault();
                  void handleSend();
                }
              }}
            />
            <Button type="primary" loading={sending} onClick={() => void handleSend()}>
              发送
            </Button>
          </div>
        </div>
      ) : null}

      <button
        type="button"
        className="support-fab"
        aria-label="在线客服"
        onClick={() => setOpen((v) => !v)}
      >
        <CustomerServiceOutlined />
        <span>在线客服</span>
        {unread > 0 ? <i className="support-fab__badge">{unread > 9 ? '9+' : unread}</i> : null}
      </button>

      <Modal
        title="提工单"
        open={ticketOpen}
        onCancel={() => setTicketOpen(false)}
        onOk={() => void handleRaiseTicket()}
        confirmLoading={ticketSubmitting}
        okText="提交工单"
        cancelText="取消"
        destroyOnClose
      >
        <div className="support-ticket-form">
          <Text type="secondary">工单挂接当前会话，提交后仍可继续聊天。</Text>
          <Input
            placeholder="主题（可选，默认取最近留言）"
            value={ticketSubject}
            onChange={(e) => setTicketSubject(e.target.value)}
            maxLength={256}
          />
          <Select
            value={ticketPriority}
            onChange={(v) => setTicketPriority(v)}
            options={[
              { value: 'low', label: '低' },
              { value: 'normal', label: '普通' },
              { value: 'high', label: '高' },
            ]}
          />
        </div>
      </Modal>
    </div>
  );
}
