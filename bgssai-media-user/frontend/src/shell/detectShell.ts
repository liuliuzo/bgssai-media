export const BGSSAI_SHELL_QUERY = 'bgssai_shell';
export const CHAT_PANE_QUERY = 'chat_pane';
export const BGSSAI_SHELL_STORAGE = 'bgssai_shell';
export const CHAT_PANE_STORAGE = 'chat_pane';
export const SHELL_DOCUMENT_CLASS = 'bgssai-shell-pane';

export interface ShellMode {
  inShell: boolean;
  bgssaiShell: boolean;
  chatPane: boolean;
}

export const STANDALONE_SHELL_MODE: ShellMode = {
  inShell: false,
  bgssaiShell: false,
  chatPane: false,
};

export function isTruthyFlag(value: string | null | undefined): boolean {
  if (value == null) {
    return false;
  }
  const normalized = String(value).trim().toLowerCase();
  return normalized === '1' || normalized === 'true' || normalized === 'yes';
}

export function isExplicitOff(value: string | null | undefined): boolean {
  if (value == null) {
    return false;
  }
  const normalized = String(value).trim().toLowerCase();
  return normalized === '0' || normalized === 'false' || normalized === 'no';
}

export function readFlagsFromSearch(search: string): { bgssaiShell: boolean; chatPane: boolean; off: boolean } {
  const raw = search.startsWith('?') ? search.slice(1) : search;
  const params = new URLSearchParams(raw);
  const shellValue = params.get(BGSSAI_SHELL_QUERY);
  const paneValue = params.get(CHAT_PANE_QUERY);
  return {
    bgssaiShell: isTruthyFlag(shellValue),
    chatPane: isTruthyFlag(paneValue),
    off: isExplicitOff(shellValue) || isExplicitOff(paneValue),
  };
}

export function readFlagsFromHash(hash: string): { bgssaiShell: boolean; chatPane: boolean; off: boolean } {
  if (!hash) {
    return { bgssaiShell: false, chatPane: false, off: false };
  }
  const withoutHash = hash.startsWith('#') ? hash.slice(1) : hash;
  const queryIndex = withoutHash.indexOf('?');
  const query = queryIndex >= 0 ? withoutHash.slice(queryIndex + 1) : withoutHash;
  return readFlagsFromSearch(query);
}

export function detectShellMode(input: {
  search?: string;
  hash?: string;
  storedShell?: string | null;
  storedChatPane?: string | null;
}): ShellMode {
  const fromSearch = readFlagsFromSearch(input.search || '');
  const fromHash = readFlagsFromHash(input.hash || '');
  if (fromSearch.off || fromHash.off) {
    return STANDALONE_SHELL_MODE;
  }
  const bgssaiShell =
    fromSearch.bgssaiShell || fromHash.bgssaiShell || isTruthyFlag(input.storedShell);
  const chatPane = fromSearch.chatPane || fromHash.chatPane || isTruthyFlag(input.storedChatPane);
  return {
    bgssaiShell,
    chatPane,
    inShell: bgssaiShell || chatPane,
  };
}

export function detectShellModeFromLocation(
  locationLike: { search?: string; hash?: string } = {},
  storage?: Pick<Storage, 'getItem'> | null,
): ShellMode {
  return detectShellMode({
    search: locationLike.search || '',
    hash: locationLike.hash || '',
    storedShell: storage ? storage.getItem(BGSSAI_SHELL_STORAGE) : null,
    storedChatPane: storage ? storage.getItem(CHAT_PANE_STORAGE) : null,
  });
}

export function persistShellMode(mode: ShellMode, storage?: Pick<Storage, 'setItem' | 'removeItem'> | null): void {
  if (!storage) {
    return;
  }
  if (mode.inShell) {
    storage.setItem(BGSSAI_SHELL_STORAGE, mode.bgssaiShell ? '1' : '0');
    storage.setItem(CHAT_PANE_STORAGE, mode.chatPane ? '1' : '0');
    if (!mode.bgssaiShell && mode.chatPane) {
      storage.setItem(BGSSAI_SHELL_STORAGE, '1');
    }
    return;
  }
  storage.removeItem(BGSSAI_SHELL_STORAGE);
  storage.removeItem(CHAT_PANE_STORAGE);
}

export function applyShellDocumentFlags(
  mode: ShellMode,
  root: { classList: { add: (c: string) => void; remove: (c: string) => void }; setAttribute: (k: string, v: string) => void; removeAttribute: (k: string) => void },
): void {
  if (mode.inShell) {
    root.classList.add(SHELL_DOCUMENT_CLASS);
    root.setAttribute('data-bgssai-shell', '1');
    root.setAttribute('data-chat-pane', mode.chatPane ? '1' : '0');
    return;
  }
  root.classList.remove(SHELL_DOCUMENT_CLASS);
  root.removeAttribute('data-bgssai-shell');
  root.removeAttribute('data-chat-pane');
}

export function bootstrapShellMode(): ShellMode {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return STANDALONE_SHELL_MODE;
  }
  let storage: Storage | null = null;
  try {
    storage = window.sessionStorage;
  } catch {
    storage = null;
  }
  const mode = detectShellModeFromLocation(window.location, storage);
  persistShellMode(mode, storage);
  applyShellDocumentFlags(mode, document.documentElement);
  if (document.body) {
    applyShellDocumentFlags(mode, document.body);
  }
  return mode;
}
