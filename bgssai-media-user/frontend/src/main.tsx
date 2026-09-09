import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { useAuthStore } from '@/stores/authStore';

useAuthStore.getState().hydrate();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
