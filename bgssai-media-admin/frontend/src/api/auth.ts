import request from './request';
import type { LoginResult } from '../types';

export function login(password: string) {
  return request.post<never, LoginResult>('/auth/login', {
    username: 'admin',
    password,
  });
}
