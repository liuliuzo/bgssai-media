import axios from 'axios';
import type { ApiResponse } from '@/types/api';
import { useAuthStore } from '@/stores/authStore';

const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Jwttoken = token;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse<unknown>;
    if (String(data?.code) === '2003') {
      useAuthStore.getState().logout();
      window.location.href = '/login?reason=session_kicked';
      return Promise.reject(new Error('账号已在其他设备登录'));
    }
    if (data && typeof data.success === 'boolean') {
      if (!data.success) {
        return Promise.reject(new Error(data.message || '请求失败'));
      }
      return { ...response, data: data.result };
    }
    return response;
  },
  (error) => {
    if (String(error.response?.data?.code) === '2003') {
      useAuthStore.getState().logout();
      window.location.href = '/login?reason=session_kicked';
      return Promise.reject(new Error('账号已在其他设备登录'));
    }
    const message =
      error.response?.data?.message ||
      error.message ||
      '网络请求失败';
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(new Error(message));
  },
);

export default client;
