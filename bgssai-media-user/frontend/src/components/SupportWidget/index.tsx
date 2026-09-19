import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Input, Spin, Typography, message } from 'antd';
import { CustomerServiceOutlined, CloseOutlined } from '@ant-design/icons';
import {
  clearStoredSession,
  createSupportSession,
  getSupportDetail,
  loadStoredSession,
  markSupportRead,
  sendSupportMessage,
  storeSession,
  type SupportDetail,
  type SupportMessage,
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

export default function SupportWidget() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [detail, setDetail] = useState<SupportDetail | null>(null);
  const [draft, setDraft] = useState('');
  const [contactName, setContactName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const listRef = useRef<HTMLDivElement>(null);

  const applyDetail = useCallback((next: SupportDetail) => {
    setDetail(next);
    storeSession(next.session);
    if (next.session.status === 'closed') {
      // keep closed history visible; new message starts a new session
    }
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
        next = await createSupportSession({
          content,
          contact_name: contactName.trim() || undefined,
          contact_email: contactEmail.trim() || undefined,
          contact_phone: contactPhone.trim() || undefined,
        });
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

  const unread = detail?.session.user_unread || 0;
  const messages: SupportMessage[] = detail?.messages || [];
  const closed = detail?.session.status === 'closed';

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
                  : detail
                    ? `状态：${detail.session.status}`
                    : '留言后客服会尽快回复'}
              </Text>
            </div>
            <Button
              type="text"
              icon={<CloseOutlined />}
              aria-label="关闭"
              onClick={() => setOpen(false)}
            />
          </div>

          {!detail || closed ? (
            <div className="support-panel__contacts">
              <Input
                placeholder="称呼（可选）"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
                maxLength={64}
              />
              <Input
                placeholder="邮箱（可选）"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
                maxLength={128}
              />
              <Input
                placeholder="手机（可选）"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
                maxLength={32}
              />
            </div>
          ) : null}

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
    </div>
  );
}
