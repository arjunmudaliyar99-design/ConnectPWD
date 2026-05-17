import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useSessionStore } from '../store/sessionStore';
import { answerApi, scoringApi, reportApi, sessionApi } from '../api/endpoints';
import { useSpeechOutput } from '../hooks/useSpeechOutput';
import { useTtsStore } from '../store/ttsStore';
import { useSpeechInput } from '../hooks/useSpeechInput';
import { useVoiceRecorder } from '../hooks/useVoiceRecorder';
import QuestionInput from '../components/chat/QuestionInput';
import ProgressBar from '../components/chat/ProgressBar';
import ThemeToggle from '../components/chat/ThemeToggle';
import styles from '../styles/chat.module.css';

const STRINGS = {
  en: {
    levelComplete: (n) => `Level ${n} complete! Continuing to the next level…`,
    completed: 'Assessment complete! Click "Generate Detailed Report" below to download your personalised PDF.',
    computingScore: 'Computing your score…',
    scoreMsg: (s) => `Total Score: ${s.totalScore} | Severity: ${typeof s.severity === 'string' ? s.severity : s.severity?.message ?? s.severity} | Disability: ${s.disabilityPct}%`,
    reportReady: 'Your detailed report is ready — download it below.',
    reportFail: 'Failed to generate report. Please try again later.',
    voiceLabel: '🎙 Voice recorded',
    error: (msg) => (typeof msg === 'string' ? msg : msg?.message) || 'Failed to submit answer. Please try again.',
    generateReport: 'Generate Detailed Report',
    generatingReport: 'Generating your report…',
    downloadReport: '⬇ Download PDF Report',
    reportError: 'Report generation failed. Please try again.',
  },
  hi: {
    levelComplete: (n) => `स्तर ${n} पूरा हुआ! अगले स्तर पर जा रहे हैं…`,
    completed: 'मूल्यांकन पूर्ण! अपनी विस्तृत रिपोर्ट पाने के लिए नीचे "विस्तृत रिपोर्ट बनाएं" पर क्लिक करें।',
    computingScore: 'स्कोर की गणना हो रही है…',
    scoreMsg: (s) => `कुल स्कोर: ${s.totalScore} | गंभीरता: ${typeof s.severity === 'string' ? s.severity : s.severity?.message ?? s.severity} | विकलांगता: ${s.disabilityPct}%`,
    reportReady: 'आपकी विस्तृत रिपोर्ट तैयार है — नीचे डाउनलोड करें।',
    reportFail: 'रिपोर्ट तैयार करने में विफल। बाद में पुनः प्रयास करें।',
    voiceLabel: '🎙 आवाज़ रिकॉर्ड की',
    error: (msg) => (typeof msg === 'string' ? msg : msg?.message) || 'उत्तर सबमिट करने में विफल। पुनः प्रयास करें।',
    generateReport: 'विस्तृत रिपोर्ट बनाएं',
    generatingReport: 'रिपोर्ट तैयार हो रही है…',
    downloadReport: '⬇ PDF रिपोर्ट डाउनलोड करें',
    reportError: 'रिपोर्ट तैयार करने में विफल। पुनः प्रयास करें।',
  },
};

export default function AssessmentPage() {
  const language = useAuthStore((s) => s.language) || 'en';
  const t = STRINGS[language] || STRINGS.en;
  const navigate = useNavigate();
  const {
    sessionId,
    moduleType,
    currentLevel,
    currentQuestion,
    questionIndex,
    totalQuestions,
    messages,
    status,
    addMessage,
    removeMessages,
    setCurrentQuestion,
    setCurrentLevel,
    setQuestionIndex,
    setStatus,
    clearSession,
  } = useSessionStore();

  const { speak, stop: stopSpeak, isSpeaking } = useSpeechOutput();
  const { isTtsMuted, toggleTtsMuted } = useTtsStore();
  const { isRecording, startRecording, stopRecording } = useVoiceRecorder();
  const { isListening, transcript, startListening, stopListening } = useSpeechInput();
  const [loading, setLoading] = useState(false);
  const [typingBot, setTypingBot] = useState(false);
  const [lastSection, setLastSection] = useState(null);
  const [speakingMsgId, setSpeakingMsgId] = useState(null);
  const [canGoBack, setCanGoBack] = useState(false);
  const [reportState, setReportState] = useState({ status: 'idle', url: null });
  const [completionInfo, setCompletionInfo] = useState({ lines: [] });
  // Each entry: { question, botMsgId, dividerMsgId, section, prevSection, userAnswerMsgId }
  const questionHistory = useRef([]);
  const lastSectionRef = useRef(null); // mirrors lastSection for non-stale reads in callbacks
  const pendingTranscriptRef = useRef('');
  const bottomRef = useRef(null);

  const scrollToBottom = () => {
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  };

  const pushBot = useCallback((content) => {
    return new Promise((resolve) => {
      const msgId = Date.now() + Math.random();
      setTypingBot(true);
      setTimeout(() => {
        addMessage({ id: msgId, role: 'bot', content });
        setTypingBot(false);
        resolve(msgId);
      }, 400);
    });
  }, [addMessage]);

  // Guard against React StrictMode double-fire (dev) and accidental re-renders
  const pushedQuestionRef = useRef(null);

  useEffect(() => {
    if (!currentQuestion) return;
    const key = currentQuestion?.code ?? currentQuestion?.id;
    if (pushedQuestionRef.current === key) return;
    pushedQuestionRef.current = key;

    const q = currentQuestion;
    const text = language === 'hi' ? (q.textHi || q.textEn || q.text) : (q.textEn || q.text);
    const section = q.sectionTitle || (language === 'hi' ? q.domainNameHi || q.domainNameEn : q.domainNameEn);

    const prevSection = lastSectionRef.current;
    let dividerMsgId = null;
    if (section && section !== lastSection) {
      dividerMsgId = `sec-${q.sectionId || section}`;
      addMessage({ id: dividerMsgId, role: 'divider', content: section });
      setLastSection(section);
      lastSectionRef.current = section;
    }

    pushBot(text).then((msgId) => {
      questionHistory.current.push({
        question: q,
        botMsgId: msgId,
        dividerMsgId,
        section: section || lastSectionRef.current,
        prevSection,
        userAnswerMsgId: null,
      });
      setCanGoBack(questionHistory.current.length > 1);
      // useTtsStore.getState() reads live value — avoids stale closure
      if (!useTtsStore.getState().isTtsMuted) {
        setSpeakingMsgId(msgId);
        speak(text);
      }
    });
    scrollToBottom();
  }, [currentQuestion?.code ?? currentQuestion?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  // Keep a ref of the latest transcript for the combined voice handler
  useEffect(() => {
    pendingTranscriptRef.current = transcript;
  }, [transcript]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, typingBot]);

  // Clear speakingMsgId when speech ends naturally
  useEffect(() => {
    if (!isSpeaking) setSpeakingMsgId(null);
  }, [isSpeaking]);

  // Escape key: immediately stop TTS (accessibility for sensory overload)
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') stopSpeak(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [stopSpeak]);

  const toBackendAnswerType = (responseType) => {
    if (!responseType) return 'TEXT';
    if (responseType === 'RATING_SCALE' || responseType === 'NUMBER') return 'SCALE';
    if (responseType === 'YES_SOMETIMES_NO' || responseType === 'MCQ') return 'CHOICE';
    return 'TEXT';
  };

  const handleAnswer = useCallback(
    async ({ answerText, scaleValue, answerType }) => {
      if (!currentQuestion || loading) return;
      setLoading(true);

      const userMsgId = Date.now() + Math.random();
      const displayText = answerText != null ? String(answerText) : String(scaleValue);
      addMessage({ id: userMsgId, role: 'user', content: displayText });
      const histEntry = questionHistory.current[questionHistory.current.length - 1];
      if (histEntry) histEntry.userAnswerMsgId = userMsgId;

      try {
        const responseType = answerType || currentQuestion.responseType || currentQuestion.type;
        // For ISAA (non-module) YES_SOMETIMES_NO answers, derive a numeric scaleValue
        // so the scoring engine can sum domain scores correctly.
        let resolvedScale = scaleValue ?? undefined;
        if (!moduleType && responseType === 'YES_SOMETIMES_NO' && answerText != null && resolvedScale === undefined) {
          const norm = String(answerText).toLowerCase();
          if (norm === 'yes' || norm === '\u0939\u093E\u0901') resolvedScale = 5;
          else if (norm === 'sometimes' || norm === '\u0915\u092D\u0940-\u0915\u092D\u0940') resolvedScale = 3;
          else if (norm === 'no' || norm === '\u0928\u0939\u0940\u0902') resolvedScale = 1;
        }
        const payload = {
          sessionId,
          questionCode: currentQuestion.code || currentQuestion.id,
          answerType: toBackendAnswerType(responseType),
          answerText: answerText ?? undefined,
          scaleValue: resolvedScale,
        };

        const { data } = await answerApi.submitText(payload);
        const result = data.data ?? data;

        if (result.sessionStatus === 'COMPLETED') {
          setStatus('COMPLETED');
          await pushBot(t.completed);
          handleSessionComplete();
        } else if (result.levelComplete) {
          setStatus('LEVEL_COMPLETE');
          await pushBot(t.levelComplete(currentLevel));
          handleContinueLevel(currentLevel + 1);
        } else if (result.nextQuestion) {
          setCurrentQuestion(result.nextQuestion);
          setQuestionIndex((questionIndex ?? 0) + 1);
        }
      } catch (err) {
        if (err.response?.status === 409) {
          try {
            const { data: sd } = await sessionApi.get(sessionId);
            const s = sd.data ?? sd;
            if (s.currentQuestion) {
              setCurrentQuestion(s.currentQuestion);
              setQuestionIndex((questionIndex ?? 0) + 1);
            }
          } catch {
            await pushBot(t.error(language === 'hi' ? 'उत्तर पहले से दर्ज है।' : 'Answer already recorded — moving forward.'));
          }
        } else {
          await pushBot(t.error(err.response?.data?.error));
        }
      } finally {
        setLoading(false);
      }
    },
    [currentQuestion, sessionId, loading, currentLevel, questionIndex] // eslint-disable-line react-hooks/exhaustive-deps
  );

  // Combined mic toggle: starts/stops both audio recording and speech-to-text
  const handleMicToggle = useCallback(async () => {
    if (isRecording || isListening) {
      stopListening();
      const blob = await stopRecording();
      const capturedTranscript = pendingTranscriptRef.current;
      if (blob) handleVoiceRecorded(blob, capturedTranscript);
    } else {
      pendingTranscriptRef.current = '';
      startListening();
      await startRecording();
    }
  }, [isRecording, isListening]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleVoiceRecorded = useCallback(
    async (blob, capturedTranscript) => {
      if (!currentQuestion || loading) return;
      setLoading(true);
      const voiceUserMsgId = Date.now() + Math.random();
      const userLabel = capturedTranscript
        ? `🎙 "${capturedTranscript}"`
        : t.voiceLabel;
      addMessage({ id: voiceUserMsgId, role: 'user', content: userLabel });
      const voiceHistEntry = questionHistory.current[questionHistory.current.length - 1];
      if (voiceHistEntry) voiceHistEntry.userAnswerMsgId = voiceUserMsgId;

      try {
        const { data } = await answerApi.submitVoice(
          sessionId,
          currentQuestion.code || currentQuestion.id,
          blob,
          capturedTranscript || undefined
        );
        const result = data.data ?? data;

        if (result.sessionStatus === 'COMPLETED') {
          setStatus('COMPLETED');
          await handleSessionComplete();
        } else if (result.levelComplete) {
          setStatus('LEVEL_COMPLETE');
          await pushBot(t.levelComplete(currentLevel));
          handleContinueLevel(currentLevel + 1);
        } else if (result.nextQuestion) {
          setCurrentQuestion(result.nextQuestion);
          setQuestionIndex((questionIndex ?? 0) + 1);
        }
      } catch (err) {
        if (err.response?.status === 409) {
          try {
            const { data: sd } = await sessionApi.get(sessionId);
            const s = sd.data ?? sd;
            if (s.currentQuestion) {
              setCurrentQuestion(s.currentQuestion);
              setQuestionIndex((questionIndex ?? 0) + 1);
            }
          } catch {
            await pushBot(t.error(language === 'hi' ? 'उत्तर पहले से दर्ज है।' : 'Answer already recorded — moving forward.'));
          }
        } else {
          await pushBot(t.error(err.response?.data?.error));
        }
      } finally {
        setLoading(false);
      }
    },
    [currentQuestion, sessionId, loading, currentLevel, questionIndex] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const handleContinueLevel = useCallback(async (nextLvl) => {
    setLoading(true);
    questionHistory.current = [];
    setCanGoBack(false);
    try {
      const { data } = await sessionApi.advanceLevel(sessionId, { level: nextLvl });
      const s = data.data ?? data;
      setCurrentLevel(nextLvl);
      setCurrentQuestion(s.currentQuestion);
      setStatus('IN_PROGRESS');
    } catch (err) {
      await pushBot(t.error(err.response?.data?.error));
    } finally {
      setLoading(false);
    }
  }, [sessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleReturnToMenu = useCallback(() => {
    stopSpeak();
    clearSession();
    navigate('/triage');
  }, [stopSpeak, clearSession, navigate]);

  const handleBack = useCallback(() => {
    if (loading) return;

    // On the very first question — go back to the main menu
    if (questionHistory.current.length <= 1) {
      handleReturnToMenu();
      return;
    }

    stopSpeak();
    setSpeakingMsgId(null);

    // Pop the current (displayed, unanswered) question off history
    const current = questionHistory.current.pop();
    const prev = questionHistory.current[questionHistory.current.length - 1];

    // Build list of message IDs to remove from chat
    const toRemove = [current.botMsgId];
    if (current.dividerMsgId) toRemove.push(current.dividerMsgId);
    // Remove prev question's submitted answer so user can re-answer cleanly
    if (prev.userAnswerMsgId) {
      toRemove.push(prev.userAnswerMsgId);
      prev.userAnswerMsgId = null;
    }
    removeMessages(toRemove);

    // Restore lastSection to what it was before prev question was shown
    setLastSection(prev.prevSection ?? null);
    lastSectionRef.current = prev.prevSection ?? null;

    // Prevent the question-push useEffect from re-adding the bot bubble
    pushedQuestionRef.current = prev.question?.code ?? prev.question?.id;

    setCurrentQuestion(prev.question);
    setQuestionIndex(Math.max(0, questionHistory.current.length - 1));
    setCanGoBack(questionHistory.current.length > 1);
  }, [loading, stopSpeak, removeMessages, handleReturnToMenu]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSessionComplete = useCallback(async () => {
    try {
      if (moduleType) {
        // Module assessments (e.g. PARENT, ADULT_SELF) don't use ISAA scoring.
        // Skip to the completion message directly.
        await pushBot(t.completed);
        setCompletionInfo({ lines: [t.completed] });
      } else {
        // Full ISAA assessment — compute the score before showing completion.
        await pushBot(t.computingScore);
        const { data: scoreData } = await scoringApi.compute(sessionId);
        const score = scoreData.data ?? scoreData;
        if (!score) {
          await pushBot(t.completed);
          setCompletionInfo({ lines: [t.completed] });
          return;
        }
        await pushBot(t.scoreMsg(score));
        await pushBot(t.completed);
        setCompletionInfo({ lines: [t.scoreMsg(score), t.completed] });
      }
    } catch (err) {
      console.error('Score computation failed:', err.response?.data ?? err);
      addMessage({ id: Date.now(), role: 'bot', content: t.reportFail });
      setCompletionInfo({ lines: [t.reportFail] });
    }
  }, [sessionId, language, moduleType]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGenerateReport = useCallback(async () => {
    setReportState({ status: 'loading', url: null });
    try {
      const response = await reportApi.generateDetailed(sessionId);
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      setReportState({ status: 'ready', url });
      addMessage({ id: Date.now(), role: 'bot', content: t.reportReady });
    } catch (err) {
      // When responseType is 'blob', error bodies are also blobs — parse them
      let errMsg = t.reportError;
      try {
        if (err.response?.data instanceof Blob) {
          const text = await err.response.data.text();
          const json = JSON.parse(text);
          // ApiResponse shape: { success, error: { code, message } }
          const raw = json?.error?.message ?? json?.message;
          if (typeof raw === 'string') errMsg = raw;
        } else if (typeof err.response?.data?.message === 'string') {
          errMsg = err.response.data.message;
        } else if (typeof err.message === 'string') {
          errMsg = err.message;
        }
      } catch { /* ignore parse errors */ }
      // Guarantee errMsg is always a primitive string before storing
      const safeMsg = typeof errMsg === 'string' ? errMsg : t.reportError;
      console.error('Report generation failed:', safeMsg);
      setReportState({ status: 'error', url: null, message: safeMsg });
    }
  }, [sessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  const isCompleted = status === 'COMPLETED';
  const displayIndex = moduleType ? (questionIndex ?? 0) : (currentQuestion?.currentPositionInLevel ?? 0);
  const displayTotal = moduleType ? totalQuestions : (currentQuestion?.totalInLevel ?? 0);

  // Normalise question shape for QuestionInput
  const normalisedQuestion = currentQuestion
    ? {
        ...currentQuestion,
        responseType: currentQuestion.responseType || currentQuestion.type,
      }
    : null;

  // Derive wizard display values directly from currentQuestion
  const wizardQuestionText = currentQuestion
    ? (language === 'hi'
        ? (currentQuestion.textHi || currentQuestion.textEn || currentQuestion.text)
        : (currentQuestion.textEn || currentQuestion.text))
    : null;

  const wizardSectionLabel = currentQuestion
    ? (currentQuestion.sectionTitle ||
        (language === 'hi'
          ? (currentQuestion.domainNameHi || currentQuestion.domainNameEn)
          : currentQuestion.domainNameEn))
    : null;

  return (
    <div className={styles.chatContainer}>
      <div className={styles.chatHeader}>
        {!isCompleted && (
          <button
            className={styles.backBtn}
            onClick={handleBack}
            disabled={loading}
            aria-label={questionHistory.current.length <= 1 ? 'Return to main menu' : 'Go back to previous question'}
            type="button"
          >
            ← {language === 'hi' ? 'वापस' : 'Back'}
          </button>
        )}
        <ProgressBar current={displayIndex} total={displayTotal} />
        <ThemeToggle />
      </div>

      <div className={styles.wizardBody}>
        {!isCompleted && (
          <>
            {currentQuestion ? (
              <div key={questionIndex} className={styles.wizardCard}>
                {wizardSectionLabel && (
                  <p className={styles.wizardSection}>{wizardSectionLabel}</p>
                )}
                <p className={styles.wizardQuestion}>{wizardQuestionText}</p>
                <div className={`${styles.wizardInputWrap}${loading ? ` ${styles.wizardInputDisabled}` : ''}`}>
                  <QuestionInput
                    question={normalisedQuestion}
                    onSubmit={handleAnswer}
                    language={language}
                    isRecording={isRecording}
                    isListening={isListening}
                    onMicToggle={loading ? undefined : handleMicToggle}
                    voiceTranscript={transcript}
                    isTtsMuted={isTtsMuted}
                    onTtsToggle={toggleTtsMuted}
                  />
                </div>
              </div>
            ) : (
              <div className={styles.wizardLoading}>
                <div className={styles.wizardDots}>
                  <span /><span /><span />
                </div>
              </div>
            )}
          </>
        )}

        {isCompleted && (
          <div className={styles.wizardCompleted}>
            {completionInfo.lines.map((line, i) => (
              <p key={i} className={styles.wizardCompletionLine}>{line}</p>
            ))}
            <div className={styles.wizardReportActions}>
              {reportState.status === 'idle' && (
                <button
                  className={styles.reportBtn}
                  onClick={handleGenerateReport}
                  type="button"
                >
                  📄 {t.generateReport}
                </button>
              )}
              {reportState.status === 'loading' && (
                <p className={styles.reportLoading}>{t.generatingReport}</p>
              )}
              {reportState.status === 'ready' && (
                <a
                  className={styles.reportLink}
                  href={reportState.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {t.downloadReport}
                </a>
              )}
              {reportState.status === 'error' && (
                <>
                  <p className={styles.reportError}>
                    <span>{typeof reportState.message === 'string' ? reportState.message : t.reportError}</span>
                  </p>
                  <button
                    className={styles.reportBtn}
                    onClick={() => { setReportState({ status: 'idle', url: null }); handleGenerateReport(); }}
                    type="button"
                  >
                    📄 {t.generateReport}
                  </button>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

