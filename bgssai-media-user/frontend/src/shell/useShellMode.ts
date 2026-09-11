import { useEffect, useState } from 'react';
import { bootstrapShellMode, type ShellMode, STANDALONE_SHELL_MODE } from './detectShell';

export function useShellMode(): ShellMode {
  const [mode, setMode] = useState<ShellMode>(() => {
    if (typeof window === 'undefined') {
      return STANDALONE_SHELL_MODE;
    }
    return bootstrapShellMode();
  });

  useEffect(() => {
    setMode(bootstrapShellMode());
  }, []);

  return mode;
}
