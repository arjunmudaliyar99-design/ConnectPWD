import { useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';

export function useSpeechOutput() {
  const language = useAuthStore((s) => s.language);
  const utteranceRef = useRef(null);

  const speak = useCallback(
    (text) => {
      if (!('speechSynthesis' in window) || !text) return;

      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = language === 'hi' ? 'hi-IN' : 'en-IN';
      utterance.rate = 0.9;
      utteranceRef.current = utterance;
      window.speechSynthesis.speak(utterance);
    },
    [language]
  );

  const stop = useCallback(() => {
    window.speechSynthesis.cancel();
  }, []);

  return { speak, stop };
}
