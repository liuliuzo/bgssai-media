import { useState } from 'react';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { Navigate, useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import LegalLinks from '../legal/LegalLinks';

const { Title, Text } = Typography;

export default function Login() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const token = useAuthStore((s) => s.token);

  if (token) {
    return <Navigate to="/dramas" replace />;
  }

  const onFinish = async (values: { password: string }) => {
    setLoading(true);
    try {
      const result = await login(values.password);
      setAuth(result.token, result.username);
      message.success('登录成功');
      navigate('/dramas');
    } catch {
      // error handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 400, boxShadow: '0 2px 8px rgba(0,0,0,0.08)' }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
          BGSSAI 媒体管理后台
        </Title>
        <Text
          type="secondary"
          style={{ display: 'block', textAlign: 'center', marginBottom: 32 }}
        >
          管理员登录
        </Text>
        <Form onFinish={onFinish} size="large">
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入管理员密码"
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登录
            </Button>
          </Form.Item>
        </Form>
        <Text type="secondary" style={{ fontSize: 12 }}>
          提示：账号 admin / 密码 admin123。管理员不接入 Chat 或用户第三方登录。
        </Text>
        <LegalLinks variant="admin-outbound" />
      </Card>
    </div>
  );
}
