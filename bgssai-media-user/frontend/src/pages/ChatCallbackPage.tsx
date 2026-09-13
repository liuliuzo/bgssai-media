import { useEffect, useState } from 'react';
import { Alert, Button, Card, Spin, Typography } from 'antd';
import { Link, useSearchParams } from 'react-router-dom';
import { completeChatLogin } from '@/api/auth';
import { useShellMode } from '@/shell/useShellMode';

export default function ChatCallbackPage() {
  const { inShell } = useShellMode();
  const [params] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const code = params.get('code') || '';
    const state = params.get('state') || '';
    completeChatLogin(code, state)
      .then(() => {
        setError('Chat 回调不应签发会话：当前为 PREP，未开通真实 token 交换。');
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Chat 登录未开通');
      })
      .finally(() => setLoading(false));
  }, [params]);

  return (
    <div
      className="shell-callback"
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: inShell ? 'flex-start' : 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
        padding: inShell ? 12 : 16,
      }}
    >
      <Card title="Chat 第三方登录" style={{ width: inShell ? '100%' : 480, maxWidth: '100%' }}>
        {loading ? (
          <Spin />
        ) : (
          <>
            <Alert
              type="warning"
              showIcon
              message="用户端预留，尚未签发本地会话"
              description={error || 'Chat token 交换未上线。'}
              style={{ marginBottom: 16 }}
            />
            <Typography.Paragraph type="secondary">
              管理员后台不接入 Chat。请使用账号密码或邮箱/手机验证码登录用户端。
            </Typography.Paragraph>
            <Link to="/login">
              <Button type="primary">返回登录</Button>
            </Link>
          </>
        )}
      </Card>
    </div>
  );
}
