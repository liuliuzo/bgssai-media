import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { useAuthStore } from '@/stores/authStore';
import { bootstrapShellMode } from '@/shell/detectShell';
import './styles/shell.css';

bootstrapShellMode();
useAuthStore.getState().hydrate();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
