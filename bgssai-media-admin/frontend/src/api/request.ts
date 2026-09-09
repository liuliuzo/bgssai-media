import axios from 'axios';
import { message } from 'antd';
import type { ApiResponse } from '../types';
import { useAuthStore } from '../store/authStore';

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Jwttoken = token;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse<unknown>;
    if (!data.success) {
      message.error(data.message || '请求失败');
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return data.result as never;
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }
    const msg =
      error.response?.data?.message || error.message || '网络错误';
    message.error(msg);
    return Promise.reject(error);
  },
);

export default request;
