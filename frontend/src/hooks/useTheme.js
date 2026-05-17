import { useEffect, useState } from 'react';

const STORAGE_KEY = 'connectpwd-theme';

/**
 * Returns [isDark, toggleTheme].
 * Persists to localStorage and applies 'light' class to <html>.
 * Dark mode is the default (no class needed — variables.css defaults are dark).
 */
export function useTheme() {
  const [isDark, setIsDark] = useState(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored !== null) return stored === 'dark';
    } catch {
      // localStorage blocked (private mode, etc.) — fall back to dark
    }
    return true;
  });

  useEffect(() => {
    const root = document.documentElement;
    if (isDark) {
      root.classList.remove('light');
    } else {
      root.classList.add('light');
    }
    try {
      localStorage.setItem(STORAGE_KEY, isDark ? 'dark' : 'light');
    } catch {
      // ignore write failures
    }
  }, [isDark]);

  const toggleTheme = () => setIsDark((d) => !d);

  return [isDark, toggleTheme];
}
