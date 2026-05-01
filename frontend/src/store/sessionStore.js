import { create } from 'zustand';

export const useSessionStore = create((set) => ({
  sessionId: null,
  moduleType: null,
  triageData: null,
  currentLevel: 1,
  currentQuestion: null,
  questionIndex: 0,
  totalQuestions: 0,
  messages: [],
  status: null,

  setSession: ({ sessionId, moduleType, currentLevel, currentQuestion, status, totalQuestions }) =>
    set({ sessionId, moduleType, currentLevel, currentQuestion, status, totalQuestions, questionIndex: 0, messages: [] }),

  setTriageData: (triageData) => set({ triageData }),

  setCurrentQuestion: (question) => set({ currentQuestion: question }),

  setCurrentLevel: (level) => set({ currentLevel: level }),

  setQuestionIndex: (questionIndex) => set({ questionIndex }),

  setStatus: (status) => set({ status }),

  addMessage: (msg) =>
    set((state) => ({ messages: [...state.messages, msg] })),

  clearSession: () =>
    set({
      sessionId: null,
      moduleType: null,
      triageData: null,
      currentLevel: 1,
      currentQuestion: null,
      questionIndex: 0,
      totalQuestions: 0,
      messages: [],
      status: null,
    }),
}));
