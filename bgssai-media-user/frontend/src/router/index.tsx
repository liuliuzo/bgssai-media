import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from '@/components/Layout/AppLayout';
import ProtectedRoute from '@/components/ProtectedRoute';
import LoginPage from '@/pages/LoginPage';
import ChatCallbackPage from '@/pages/ChatCallbackPage';
import HomePage from '@/pages/HomePage';
import DramaDetailPage from '@/pages/DramaDetailPage';
import EpisodePlayerPage from '@/pages/EpisodePlayerPage';
import ShortsPage from '@/pages/ShortsPage';
import ShortPlayerPage from '@/pages/ShortPlayerPage';
import UniversalPlayerPage from '@/pages/UniversalPlayerPage';
import ContinueWatchingPage from '@/pages/ContinueWatchingPage';
import FormatMatrixPage from '@/pages/FormatMatrixPage';
import SettingsPage from '@/pages/SettingsPage';
import { useAuthStore } from '@/stores/authStore';

function LoginRedirect({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);

  if (!hydrated) return null;
  if (token) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <LoginRedirect>
              <LoginPage />
            </LoginRedirect>
          }
        />
        <Route path="/login/chat/callback" element={<ChatCallbackPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<HomePage />} />
          <Route path="shorts" element={<ShortsPage />} />
          <Route path="shorts/:mediaId" element={<ShortPlayerPage />} />
          <Route path="drama/:id" element={<DramaDetailPage />} />
          <Route path="play/episode/:id" element={<EpisodePlayerPage />} />
          <Route path="player" element={<UniversalPlayerPage />} />
          <Route path="continue" element={<ContinueWatchingPage />} />
          <Route path="formats" element={<FormatMatrixPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
