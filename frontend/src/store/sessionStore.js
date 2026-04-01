import { create } from 'zustand';

export const useSessionStore = create((set) => ({
  sessionId: null,
  consentId: null,
  currentLevel: 1,
  currentQuestion: null,
  messages: [],
  status: null,

  setSession: ({ sessionId, consentId, currentLevel, currentQuestion, status }) =>
    set({ sessionId, consentId, currentLevel, currentQuestion, status, messages: [] }),

  setCurrentQuestion: (question) => set({ currentQuestion: question }),

  setCurrentLevel: (level) => set({ currentLevel: level }),

  setStatus: (status) => set({ status }),

  addMessage: (msg) =>
    set((state) => ({ messages: [...state.messages, msg] })),

  clearSession: () =>
    set({
      sessionId: null,
      consentId: null,
      currentLevel: 1,
      currentQuestion: null,
      messages: [],
      status: null,
    }),
}));
