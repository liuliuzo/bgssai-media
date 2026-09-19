import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Input,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import {
  closeSupport,
  fetchSupportDetail,
  markSupportRead,
  replySupport,
  type SupportDetail,
  type SupportMessage,
} from '../api/support';

const { TextArea } = Input;
const { Text } = Typography;

function statusTag(status?: string) {
  if (status === 'pending') return <Tag color="gold">待处理</Tag>;
  if (status === 'open') return <Tag color="blue">进行中</Tag>;
  if (status === 'closed') return <Tag>已关闭</Tag>;
  return <Tag>{status || '-'}</Tag>;
}

export default function SupportSessionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const sessionId = Number(id);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [detail, setDetail] = useState<SupportDetail | null>(null);
  const [draft, setDraft] = useState('');
  const listRef = useRef<HTMLDivElement>(null);

  const load = useCallback(async () => {
    if (!Number.isFinite(sessionId) || sessionId <= 0) return;
    setLoading(true);
    try {
      const next = await fetchSupportDetail(sessionId);
      setDetail(next);
      if ((next.session.admin_unread || 0) > 0) {
        const cleared = await markSupportRead(sessionId);
        setDetail(cleared);
      }
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [detail?.messages?.length]);

  const handleReply = async () => {
    const content = draft.trim();
    if (!content) {
      message.warning('请输入回复内容');
      return;
    }
    setSending(true);
    try {
      const next = await replySupport(sessionId, content);
      setDetail(next);
      setDraft('');
      message.success('已回复');
    } catch {
      // handled
    } finally {
      setSending(false);
    }
  };

  const handleClose = async () => {
    setSending(true);
    try {
      const next = await closeSupport(sessionId);
      setDetail(next);
      message.success('会话已关闭');
    } catch {
      // handled
    } finally {
      setSending(false);
    }
  };

  const session = detail?.session;
  const messages: SupportMessage[] = detail?.messages || [];
  const closed = session?.status === 'closed';

  return (
    <Card
      loading={loading && !detail}
      title={
        <Space>
          <Button type="link" onClick={() => navigate('/support')}>
            返回
          </Button>
          <span>会话 #{sessionId}</span>
          {statusTag(session?.status)}
        </Space>
      }
      extra={
        !closed ? (
          <Button danger loading={sending} onClick={() => void handleClose()}>
            关闭会话
          </Button>
        ) : null
      }
    >
      {session ? (
        <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="主题">{session.subject || '-'}</Descriptions.Item>
          <Descriptions.Item label="用户">
            {session.user_id ? `#${session.user_id}` : '匿名'}
          </Descriptions.Item>
          <Descriptions.Item label="联系人">{session.contact_name || '-'}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{session.contact_email || '-'}</Descriptions.Item>
          <Descriptions.Item label="手机">{session.contact_phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {session.updated_at ? new Date(session.updated_at).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
        </Descriptions>
      ) : null}

      <div
        ref={listRef}
        style={{
          maxHeight: 420,
          overflowY: 'auto',
          padding: 12,
          marginBottom: 16,
          background: '#f8fafc',
          border: '1px solid #e2e8f0',
          borderRadius: 8,
        }}
      >
        {messages.map((msg) => (
          <div
            key={msg.id}
            style={{
              marginBottom: 12,
              textAlign:
                msg.sender_type === 'admin'
                  ? 'right'
                  : msg.sender_type === 'system'
                    ? 'center'
                    : 'left',
            }}
          >
            <Text type="secondary" style={{ fontSize: 12 }}>
              {msg.sender_type === 'user'
                ? '用户'
                : msg.sender_type === 'admin'
                  ? '客服'
                  : '系统'}{' '}
              {msg.created_at ? new Date(msg.created_at).toLocaleString('zh-CN') : ''}
            </Text>
            <div
              style={{
                display: 'inline-block',
                marginTop: 4,
                padding: '8px 10px',
                borderRadius: 8,
                background:
                  msg.sender_type === 'admin'
                    ? '#dbeafe'
                    : msg.sender_type === 'system'
                      ? 'transparent'
                      : '#fff',
                border: msg.sender_type === 'system' ? 'none' : '1px solid #e2e8f0',
                whiteSpace: 'pre-wrap',
                maxWidth: '80%',
                textAlign: 'left',
              }}
            >
              {msg.content}
            </div>
          </div>
        ))}
      </div>

      {!closed ? (
        <Space.Compact style={{ width: '100%' }}>
          <TextArea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="输入回复…"
            autoSize={{ minRows: 2, maxRows: 5 }}
            maxLength={4000}
          />
          <Button type="primary" loading={sending} onClick={() => void handleReply()}>
            回复
          </Button>
        </Space.Compact>
      ) : (
        <Text type="secondary">会话已关闭。用户可在用户端新开会话。</Text>
      )}
    </Card>
  );
}
