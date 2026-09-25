import ProductMotion from '../components/ProductMotion'
import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Card, Form, Input, Tabs, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  loginByPassword,
  sendEmailOtp,
  loginByEmailOtp,
  sendPhoneOtp,
  loginByPhoneOtp,
  loginByOauth,
  completeOauth,
  prepareChatLogin,
  loginChannels,
} from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';
import type { LoginResult } from '@/types/api';
import { useShellMode } from '@/shell/useShellMode';
import LegalConsent from '@/legal/LegalConsent';
import BotPromoCard from '@/components/BotPromoCard';
import { PhoneDialField } from '@/components/PhoneDialField';
import { DEFAULT_DIAL_CODE, composeApiPhone, validatePhoneParts } from '@/lib/phoneDial';

function MediaPhoneInput({
  value,
  onChange,
  dialCode,
  onDialCodeChange,
}: {
  value?: string;
  onChange?: (value: string) => void;
  dialCode: string;
  onDialCodeChange: (value: string) => void;
}) {
  return (
    <PhoneDialField
      id="login-phone"
      dialCode={dialCode}
      nationalNumber={value || ''}
      onDialCodeChange={onDialCodeChange}
      onNationalNumberChange={(next) => onChange?.(next)}
      wrapperClassName="phone-dial-field"
    />
  );
}

export default function LoginPage() {
  const { inShell } = useShellMode();
  const navigate = useNavigate();
  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('reason') === 'session_kicked') message.error('账号已在其他设备登录，请重新登录');
  }, []);
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [loading, setLoading] = useState(false);
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [phoneCodeSent, setPhoneCodeSent] = useState(false);
  const [openChannels, setOpenChannels] = useState<string[]>([]);

  // 只画后端说凭证已配齐的渠道：先前四个境内入口和 Chat 入口无条件常驻，点下去才报「未配置」。
  useEffect(() => {
    let alive = true;
    loginChannels()
      .then((list) => {
        if (alive) setOpenChannels(Array.isArray(list) ? list : []);
      })
      .catch(() => {
        if (alive) setOpenChannels([]);
      });
    return () => {
      alive = false;
    };
  }, []);
  const [emailForm] = Form.useForm();
  const [phoneForm] = Form.useForm();
  const [dialCode, setDialCode] = useState<string>(DEFAULT_DIAL_CODE);
  const finishing = useRef(false);

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const code = params.get('code');
    const state = params.get('state');
    if (!code || !state || finishing.current) return;
    finishing.current = true;
    let provider = params.get('provider') || '';
    try {
      const saved = JSON.parse(sessionStorage.getItem('bgssai-media-oauth') || '{}') as {
        provider?: string;
      };
      if (saved.provider) provider = saved.provider;
    } catch {
      /* ignore */
    }
    if (!provider) provider = 'CHAT';
    setLoading(true);
    completeOauth(provider, code, state)
      .then((result) => {
        try {
          sessionStorage.removeItem('bgssai-media-oauth');
        } catch {
          /* ignore */
        }
        handleLoginSuccess(result);
      })
      .catch((err) => {
        finishing.current = false;
        message.error(err instanceof Error ? err.message : '授权失败');
      })
      .finally(() => setLoading(false));
  }, [location.search]);

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
    const phoneError = validatePhoneParts(dialCode, phone);
    if (phoneError) {
      message.warning(phoneError);
      return;
    }
    setLoading(true);
    try {
      const sent = await sendPhoneOtp(composeApiPhone(dialCode, phone));
      setPhoneCodeSent(true);
      message.success(
        // 短信正文里的 product#seq 是排障编号，不往用户界面上贴
        sent ? '验证码已发送' : '验证码已发送',
      );
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    } finally {
      setLoading(false);
    }
  };

  const onPhoneLogin = async (values: { phone: string; code: string }) => {
    setLoading(true);
    try {
      const result = await loginByPhoneOtp(composeApiPhone(dialCode, values.phone), values.code);
      handleLoginSuccess(result);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  // 全线统一的方形矢量品牌标，见 public/brand/oauth/
  const OAUTH_PROVIDERS: Array<[string, string, string]> = [
    ['WECHAT', '微信', '/brand/oauth/wechat.svg'],
    ['DOUYIN', '抖音', '/brand/oauth/douyin.svg'],
    ['BAIDU', '百度', '/brand/oauth/baidu.svg'],
    ['ALIPAY', '支付宝', '/brand/oauth/alipay.svg'],
  ];
  const visibleProviders = OAUTH_PROVIDERS.filter(([key]) => openChannels.includes(key));
  const chatReady = openChannels.includes('CHAT');

  const startOauth = async (provider: string) => {
    setLoading(true);
    try {
      const data = await loginByOauth(provider);
      try {
        sessionStorage.setItem('bgssai-media-oauth', JSON.stringify({ provider, state: data.state }));
      } catch {
        /* ignore */
      }
      window.location.href = data.authorize_url;
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
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
        display: 'flex',
        alignItems: inShell ? 'flex-start' : 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
        padding: inShell ? 12 : 16,
        boxSizing: 'border-box',
      }}
    >
      <BotPromoCard />
      <Card title="用户登录" style={{ width: inShell ? '100%' : 420, maxWidth: '100%' }}>
        <ProductMotion />
        <Alert
          type="info"
          showIcon
          message="演示账号"
          description="用户名 demo，密码 user123"
          style={{ marginBottom: 16 }}
        />
        <Tabs
            defaultActiveKey="phone"
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
                    rules={[{
                      validator: async (_, value) => {
                        const err = validatePhoneParts(dialCode, value);
                        if (err) throw new Error(err);
                      },
                    }]}
                  >
                    <MediaPhoneInput dialCode={dialCode} onDialCodeChange={setDialCode} />
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
        {visibleProviders.length > 0 || chatReady ? (
          <div className="login-oauth-divider">第三方账号登录</div>
        ) : null}
        {visibleProviders.length > 0 ? (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            {visibleProviders.map(([key, label, icon]) => (
              <Button key={key} onClick={() => startOauth(key)} disabled={loading} title={label}
                icon={<img className="login-oauth-icon" src={icon} alt="" width={18} height={18} />}>
                {label}
              </Button>
            ))}
          </div>
        ) : null}
        {chatReady ? (
          <Button block style={{ marginTop: 8 }} onClick={onChat} disabled={loading}
            icon={<img className="login-oauth-icon" src="/brand/oauth/bgssai.svg" alt="" width={18} height={18} />}>
            用 Chat 登录
          </Button>
        ) : null}
        <LegalConsent />
      </Card>
    </div>
  );
}
