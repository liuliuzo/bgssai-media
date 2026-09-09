import { create } from 'zustand';

const STORAGE_KEY = 'bgssai-media-user-auth';

interface AuthState {
  token: string | null;
  userId: number | null;
  username: string | null;
  hydrated: boolean;
  setAuth: (token: string, userId: number, username: string) => void;
  logout: () => void;
  hydrate: () => void;
}

function loadFromStorage(): Pick<AuthState, 'token' | 'userId' | 'username'> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { token: null, userId: null, username: null };
    const parsed = JSON.parse(raw) as {
      token?: string;
      userId?: number;
      username?: string;
    };
    return {
      token: parsed.token ?? null,
      userId: parsed.userId ?? null,
      username: parsed.username ?? null,
    };
  } catch {
    return { token: null, userId: null, username: null };
  }
}

function saveToStorage(
  token: string | null,
  userId: number | null,
  username: string | null,
) {
  if (!token) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({ token, userId, username }),
  );
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userId: null,
  username: null,
  hydrated: false,

  setAuth: (token, userId, username) => {
    saveToStorage(token, userId, username);
    set({ token, userId, username });
  },

  logout: () => {
    saveToStorage(null, null, null);
    set({ token: null, userId: null, username: null });
  },

  hydrate: () => {
    const stored = loadFromStorage();
    set({ ...stored, hydrated: true });
  },
}));
