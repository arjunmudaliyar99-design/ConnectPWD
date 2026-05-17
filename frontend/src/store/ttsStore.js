import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useTtsStore = create(
  persist(
    (set) => ({
      isTtsMuted: false,
      toggleTtsMuted: () => set((s) => ({ isTtsMuted: !s.isTtsMuted })),
    }),
    {
      name: 'connectpwd-tts',
      partialize: (state) => ({ isTtsMuted: state.isTtsMuted }),
    }
  )
);
