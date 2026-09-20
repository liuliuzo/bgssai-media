import client from './client';
import type { ChatOauthPrepare, LoginResult } from '@/types/api';

export async function loginByPassword(username: string, password: string) {
  const { data } = await client.post<LoginResult>('/auth/login', {
    username,
    password,
  });
  return data;
}

export async function sendEmailOtp(email: string) {
  const { data } = await client.post<Record<string, unknown>>(
    '/auth/otp/email/send',
    { email },
  );
  return data;
}

export async function loginByEmailOtp(email: string, code: string) {
  const { data } = await client.post<LoginResult>('/auth/otp/email/login', {
    email,
    code,
  });
  return data;
}

export type SendPhoneOtpResult = {
  sent: boolean;
  product: string;
  seq: number;
};

export async function sendPhoneOtp(phone: string) {
  const { data } = await client.post<SendPhoneOtpResult>(
    '/auth/otp/phone/send',
    { phone },
  );
  return data;
}

export async function loginByPhoneOtp(phone: string, code: string) {
  const { data } = await client.post<LoginResult>('/auth/otp/phone/login', {
    phone,
    code,
  });
  return data;
}

export async function loginByOauth(provider: string) {
  const { data } = await client.post<{ authorize_url: string; state: string }>('/auth/oauth/authorize', {
    provider,
  });
  return data;
}

export async function completeOauth(provider: string, code: string, state: string) {
  const { data } = await client.post<LoginResult>('/auth/oauth/callback', {
    provider,
    code,
    state,
  });
  return data;
}

// GET /auth/oauth/channels -> string[]：凭证已配齐的第三方登录渠道（WECHAT / DOUYIN / BAIDU / ALIPAY / CHAT）
export async function loginChannels() {
  const { data } = await client.get<string[]>('/auth/oauth/channels');
  return data;
}

export async function prepareChatLogin() {
  const { data } = await client.get<ChatOauthPrepare>('/auth/chat/prepare');
  return data;
}

export async function completeChatLogin(code: string, state: string) {
  const { data } = await client.post<LoginResult>('/auth/chat/callback', {
    code,
    state,
  });
  return data;
}
