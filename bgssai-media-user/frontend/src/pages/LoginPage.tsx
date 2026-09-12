import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Tabs, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  loginByPassword,
  sendEmailOtp,
  loginByEmailOtp,
  sendPhoneOtp,
  loginByPhoneOtp,
  loginByOauth,
  prepareChatLogin,
} from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';
import type { LoginResult } from '@/types/api';
import { useShellMode } from '@/shell/useShellMode';
import LegalLinks from '@/legal/LegalLinks';

export default function LoginPage() {
  const { inShell } = useShellMode();
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [loading, setLoading] = useState(false);
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [phoneCodeSent, setPhoneCodeSent] = useState(false);
  const [emailForm] = Form.useForm();
  const [phoneForm] = Form.useForm();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleLoginSuccess = (result: LoginResult) => {
    setAuth(result.token, result.user_id, result.username);
    message.success('登录成功');
    navigate(from, { replace: true });
  };

  const onPasswordLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const result = await loginByPassword(values.username, values.password);
      handleLoginSuccess(result);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const onSendEmailOtp = async () => {
    const email = emailForm.getFieldValue('email');
    if (!email) {
      message.warning('请输入邮箱');
      return;
    }
    setLoading(true);
    try {
      await sendEmailOtp(email);
      setEmailCodeSent(true);
      message.success('验证码已发送');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    } finally {
      setLoading(false);
    }
  };

  const onEmailLogin = async (values: { email: string; code: string }) => {
    setLoading(true);
    try {
      const result = await loginByEmailOtp(values.email, values.code);
      handleLoginSuccess(result);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const onSendPhoneOtp = async () => {
    const phone = phoneForm.getFieldValue('phone');
    if (!phone) {
      message.warning('请输入手机号');
      return;
    }
    setLoading(true);
    try {
      await sendPhoneOtp(phone);
      setPhoneCodeSent(true);
      message.success('验证码已发送');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    } finally {
      setLoading(false);
    }
  };

  const onPhoneLogin = async (values: { phone: string; code: string }) => {
    setLoading(true);
    try {
      const result = await loginByPhoneOtp(values.phone, values.code);
      handleLoginSuccess(result);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const onOauth = async (provider: string) => {
    setLoading(true);
    try {
      const result = await loginByOauth(provider);
      handleLoginSuccess(result);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const onChat = async () => {
    setLoading(true);
    try {
      const prep = await prepareChatLogin();
      if (prep.status === 'READY' && prep.authorize_url) {
        window.location.assign(prep.authorize_url);
        return;
      }
      message.info(prep.message || 'Chat 第三方登录已预留，尚未开通真实授权');
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Chat 登录不可用');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="shell-login"
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: inShell ? 'flex-start' : 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
        padding: inShell ? 12 : 16,
      }}
    >
      <Card title="用户登录" style={{ width: inShell ? '100%' : 420, maxWidth: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="演示账号"
          description="用户名 demo，密码 user123"
          style={{ marginBottom: 16 }}
        />
        <Tabs
          items={[
            {
              key: 'password',
              label: '密码登录',
              children: (
                <Form layout="vertical" onFinish={onPasswordLogin}>
                  <Form.Item
                    name="username"
                    label="用户名"
                    rules={[{ required: true, message: '请输入用户名' }]}
                  >
                    <Input prefix={<UserOutlined />} placeholder="用户名" />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[{ required: true, message: '请输入密码' }]}
                  >
                    <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'email',
              label: '邮箱验证码',
              children: (
                <Form form={emailForm} layout="vertical" onFinish={onEmailLogin}>
                  <Form.Item
                    name="email"
                    label="邮箱"
                    rules={[
                      { required: true, message: '请输入邮箱' },
                      { type: 'email', message: '邮箱格式不正确' },
                    ]}
                  >
                    <Input prefix={<MailOutlined />} placeholder="email@example.com" />
                  </Form.Item>
                  <Form.Item
                    name="code"
                    label="验证码"
                    rules={[{ required: true, message: '请输入验证码' }]}
                  >
                    <Input.Search
                      placeholder="验证码"
                      enterButton="发送验证码"
                      onSearch={onSendEmailOtp}
                      loading={loading}
                    />
                  </Form.Item>
                  {emailCodeSent && (
                    <Alert type="success" message="验证码已发送至邮箱" style={{ marginBottom: 12 }} />
                  )}
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'phone',
              label: '手机验证码',
              children: (
                <Form form={phoneForm} layout="vertical" onFinish={onPhoneLogin}>
                  <Form.Item
                    name="phone"
                    label="手机号"
                    rules={[{ required: true, message: '请输入手机号' }]}
                  >
                    <Input prefix={<PhoneOutlined />} placeholder="手机号" />
                  </Form.Item>
                  <Form.Item
                    name="code"
                    label="验证码"
                    rules={[{ required: true, message: '请输入验证码' }]}
                  >
                    <Input.Search
                      placeholder="验证码"
                      enterButton="发送验证码"
                      onSearch={onSendPhoneOtp}
                      loading={loading}
                    />
                  </Form.Item>
                  {phoneCodeSent && (
                    <Alert type="success" message="验证码已发送至手机" style={{ marginBottom: 12 }} />
                  )}
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录
                  </Button>
                </Form>
              ),
            },
          ]}
        />
        <div style={{ marginTop: 16, textAlign: 'center', color: '#8c8c8c', fontSize: 12 }}>
          或使用第三方账号
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginTop: 8 }}>
          {[
            ['WECHAT', '微信'],
            ['DOUYIN', '抖音'],
            ['BAIDU', '百度'],
            ['ALIPAY', '支付宝'],
          ].map(([key, label]) => (
            <Button key={key} onClick={() => onOauth(key)} disabled={loading}>
              {label}
            </Button>
          ))}
        </div>
        <Button block style={{ marginTop: 8 }} onClick={onChat} disabled={loading}>
          Chat 第三方登录（预留）
        </Button>
        <LegalLinks variant="user-consent" />
      </Card>
    </div>
  );
}
