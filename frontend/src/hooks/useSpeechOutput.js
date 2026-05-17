import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { useTtsStore } from '../store/ttsStore';

export function useSpeechOutput() {
  const language = useAuthStore((s) => s.language);
  const isTtsMuted = useTtsStore((s) => s.isTtsMuted);
  const utteranceRef = useRef(null);
  const [isSpeaking, setIsSpeaking] = useState(false);

  // Stop immediately whenever global mute is toggled on
  useEffect(() => {
    if (isTtsMuted) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
    }
  }, [isTtsMuted]);

  const speak = useCallback(
    (text) => {
      if (!('speechSynthesis' in window) || !text || isTtsMuted) return;

      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = language === 'hi' ? 'hi-IN' : 'en-IN';
      utterance.rate = 0.9;
      utterance.onstart = () => setIsSpeaking(true);
      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => setIsSpeaking(false);
      utteranceRef.current = utterance;
      window.speechSynthesis.speak(utterance);
    },
    [language, isTtsMuted]
  );

  const stop = useCallback(() => {
    window.speechSynthesis.cancel();
    setIsSpeaking(false);
  }, []);

  return { speak, stop, isSpeaking };
}
