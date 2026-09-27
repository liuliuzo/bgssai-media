import BotDownloadLayout from './../components/BotDownloadLayout'
import ProductMotion from '../components/ProductMotion'
import { useEffect, useRef, useState } from 'react';
import { Button, Card, Form, Input, Tabs, message } from 'antd';
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

// 用户端登录页（Standards §14 / §15）：品牌行 → 装饰动效 → 标题 → 一排三个登录方式（密码 / 邮箱验证码 / 手机验证码，
// 默认手机验证码）→ 表单 + 主按钮 → 第三方账号登录（只画已开通渠道）→ 一行法律同意。BGSSAI BOT 下载在页面顶部导航。
// 字段名只给读屏（aria-label），可见文字只有方式名与占位符，避免同一个词在一屏上写三遍。
const RESEND_SECONDS = 60;

export default function LoginPage() {
  const { inShell } = useShellMode();
  const navigate = useNavigate();
  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('reason') === 'session_kicked') message.error('账号已在其他设备登录，请重新登录');
  }, []);
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [loading, setLoading] = useState(false);
  // 邮箱与手机各自一个重发倒计时（后端按渠道 + 目标分别限频），切换方式不互相影响。
  const [countdowns, setCountdowns] = useState<{ email: number; phone: number }>({ email: 0, phone: 0 });
  const ticking = countdowns.email > 0 || countdowns.phone > 0;
  useEffect(() => {
    if (!ticking) return undefined;
    const timer = window.setTimeout(() => {
      setCountdowns((prev) => ({ email: Math.max(0, prev.email - 1), phone: Math.max(0, prev.phone - 1) }));
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [countdowns, ticking]);
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
      message.warning('请输入邮箱地址');
      return;
    }
    setLoading(true);
    try {
      await sendEmailOtp(email);
      setCountdowns((prev) => ({ ...prev, email: RESEND_SECONDS }));
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
      await sendPhoneOtp(composeApiPhone(dialCode, phone));
      setCountdowns((prev) => ({ ...prev, phone: RESEND_SECONDS }));
      // 短信正文里的 product#seq 是排障编号，不往用户界面上贴（Standards §14.2）
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
      // 后端给运维看的说明不上界面（Standards §14.1）
      message.info('该登录方式暂不可用，请换一种方式登录');
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Chat 登录不可用');
    } finally {
      setLoading(false);
    }
  };

  const codeButton = (channel: 'email' | 'phone') =>
    countdowns[channel] > 0 ? `${countdowns[channel]} 秒后重发` : '获取验证码';

  return (
    <BotDownloadLayout>
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
      <Card className="login-card" style={{ width: inShell ? '100%' : 420, maxWidth: '100%' }}>
        <div className="login-brand">
          <img className="login-brand-mark" src="/brand/bgss-mark.png" alt="" width={28} height={28} />
          <span>BGSSAI MEDIA</span>
        </div>
        <ProductMotion />
        <h1 className="login-title">欢迎回来</h1>
        <Tabs
          defaultActiveKey="phone"
          items={[
            {
              key: 'password',
              label: '密码',
              children: (
                <Form onFinish={onPasswordLogin}>
                  <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                    <Input
                      prefix={<UserOutlined />}
                      aria-label="用户名"
                      placeholder="请输入用户名"
                      autoComplete="username"
                    />
                  </Form.Item>
                  <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                    <Input.Password
                      prefix={<LockOutlined />}
                      aria-label="密码"
                      placeholder="请输入密码"
                      autoComplete="current-password"
                    />
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
                <Form form={emailForm} onFinish={onEmailLogin}>
                  <Form.Item
                    name="email"
                    rules={[
                      { required: true, message: '请输入邮箱地址' },
                      { type: 'email', message: '邮箱格式不正确' },
                    ]}
                  >
                    <Input
                      prefix={<MailOutlined />}
                      aria-label="邮箱"
                      placeholder="请输入邮箱地址"
                      autoComplete="email"
                    />
                  </Form.Item>
                  <Form.Item name="code" rules={[{ required: true, message: '请输入验证码' }]}>
                    <Input.Search
                      aria-label="邮箱验证码"
                      placeholder="6 位验证码"
                      inputMode="numeric"
                      maxLength={6}
                      enterButton={codeButton('email')}
                      onSearch={() => {
                        if (countdowns.email === 0) void onSendEmailOtp();
                      }}
                      loading={loading}
                    />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录 / 注册
                  </Button>
                </Form>
              ),
            },
            {
              key: 'phone',
              label: '手机验证码',
              children: (
                <Form form={phoneForm} onFinish={onPhoneLogin}>
                  <Form.Item
                    name="phone"
                    rules={[{
                      validator: async (_, value) => {
                        const err = validatePhoneParts(dialCode, value);
                        if (err) throw new Error(err);
                      },
                    }]}
                  >
                    <MediaPhoneInput dialCode={dialCode} onDialCodeChange={setDialCode} />
                  </Form.Item>
                  <Form.Item name="code" rules={[{ required: true, message: '请输入验证码' }]}>
                    <Input.Search
                      aria-label="短信验证码"
                      placeholder="6 位验证码"
                      inputMode="numeric"
                      maxLength={6}
                      enterButton={codeButton('phone')}
                      onSearch={() => {
                        if (countdowns.phone === 0) void onSendPhoneOtp();
                      }}
                      loading={loading}
                    />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录 / 注册
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
          <div className="login-oauth-grid">
            {visibleProviders.map(([key, label, icon]) => (
              <Button
                key={key}
                onClick={() => startOauth(key)}
                disabled={loading}
                icon={<img className="login-oauth-icon" src={icon} alt="" width={18} height={18} />}
              >
                {label}
              </Button>
            ))}
          </div>
        ) : null}
        {chatReady ? (
          <Button
            block
            style={{ marginTop: 8 }}
            onClick={onChat}
            disabled={loading}
            icon={<img className="login-oauth-icon" src="/brand/oauth/bgssai.svg" alt="" width={18} height={18} />}
          >
            用 Chat 登录
          </Button>
        ) : null}
        <LegalConsent />
      </Card>
    </div>
    </BotDownloadLayout>
  );
}
