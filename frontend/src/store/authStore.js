import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      role: null,
      language: 'en',

      setAuth: ({ accessToken, refreshToken, userId, role, language }) =>
        set({ accessToken, refreshToken, userId, role, language }),

      setTokens: ({ accessToken, refreshToken }) =>
        set({ accessToken, refreshToken }),

      setLanguage: (language) => set({ language }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          role: null,
          language: 'en',
        }),
    }),
    {
      name: 'connectpwd-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        role: state.role,
        language: state.language,
      }),
    }
  )
);
