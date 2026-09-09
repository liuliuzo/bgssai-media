import client from './client';
import type { LoginResult } from '@/types/api';

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

export async function sendPhoneOtp(phone: string) {
  const { data } = await client.post<Record<string, unknown>>(
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
