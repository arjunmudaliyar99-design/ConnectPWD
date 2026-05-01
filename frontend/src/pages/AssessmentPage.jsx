import { useState, useEffect, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { useSessionStore } from '../store/sessionStore';
import { answerApi, scoringApi, reportApi, sessionApi } from '../api/endpoints';
import { useSpeechOutput } from '../hooks/useSpeechOutput';
import { useSpeechInput } from '../hooks/useSpeechInput';
import { useVoiceRecorder } from '../hooks/useVoiceRecorder';
import MessageBubble from '../components/chat/MessageBubble';
import QuestionInput from '../components/chat/QuestionInput';
import SectionDivider from '../components/chat/SectionDivider';
import ProgressBar from '../components/chat/ProgressBar';
import styles from '../styles/chat.module.css';

const STRINGS = {
  en: {
    levelComplete: (n) => `Level ${n} complete! Continuing to the next level…`,
    completed: 'Assessment complete! Generating your report…',
    computingScore: 'Computing your score…',
    scoreMsg: (s) => `Total Score: ${s.totalScore} | Severity: ${s.severity} | Disability: ${s.disabilityPct}%`,
    reportReady: 'Report ready! Download below.',
    reportFail: 'Failed to generate report. Please try again later.',
    voiceLabel: '🎙 Voice recorded',
    error: (msg) => msg || 'Failed to submit answer. Please try again.',
  },
  hi: {
    levelComplete: (n) => `स्तर ${n} पूरा हुआ! अगले स्तर पर जा रहे हैं…`,
    completed: 'मूल्यांकन पूर्ण! आपकी रिपोर्ट तैयार हो रही है…',
    computingScore: 'स्कोर की गणना हो रही है…',
    scoreMsg: (s) => `कुल स्कोर: ${s.totalScore} | गंभीरता: ${s.severity} | विकलांगता: ${s.disabilityPct}%`,
    reportReady: 'रिपोर्ट तैयार है! नीचे डाउनलोड करें।',
    reportFail: 'रिपोर्ट तैयार करने में विफल। बाद में पुनः प्रयास करें।',
    voiceLabel: '🎙 आवाज़ रिकॉर्ड की',
    error: (msg) => msg || 'उत्तर सबमिट करने में विफल। पुनः प्रयास करें।',
  },
};

export default function AssessmentPage() {
  const language = useAuthStore((s) => s.language) || 'en';
  const t = STRINGS[language] || STRINGS.en;
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
    setCurrentQuestion,
    setCurrentLevel,
    setQuestionIndex,
    setStatus,
  } = useSessionStore();

  const { speak, stop: stopSpeak } = useSpeechOutput();
  const { isRecording, startRecording, stopRecording } = useVoiceRecorder();
  const { isListening, transcript, startListening, stopListening } = useSpeechInput();
  const [loading, setLoading] = useState(false);
  const [typingBot, setTypingBot] = useState(false);
  const [lastSection, setLastSection] = useState(null);
  const pendingTranscriptRef = useRef('');
  const bottomRef = useRef(null);

  const scrollToBottom = () => {
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  };

  const pushBot = useCallback((content) => {
    return new Promise((resolve) => {
      setTypingBot(true);
      setTimeout(() => {
        addMessage({ id: Date.now() + Math.random(), role: 'bot', content });
        setTypingBot(false);
        resolve();
      }, 400);
    });
  }, [addMessage]);

  useEffect(() => {
    if (!currentQuestion) return;
    const q = currentQuestion;
    const text = language === 'hi' ? (q.textHi || q.textEn || q.text) : (q.textEn || q.text);
    const section = q.sectionTitle || (language === 'hi' ? q.domainNameHi || q.domainNameEn : q.domainNameEn);

    if (section && section !== lastSection) {
      addMessage({ id: `sec-${q.sectionId || section}`, role: 'divider', content: section });
      setLastSection(section);
    }

    pushBot(text).then(() => speak(text));
    scrollToBottom();
  }, [currentQuestion?.code ?? currentQuestion?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  // Keep a ref of the latest transcript for the combined voice handler
  useEffect(() => {
    pendingTranscriptRef.current = transcript;
  }, [transcript]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, typingBot]);

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

      const displayText = answerText != null ? String(answerText) : String(scaleValue);
      addMessage({ id: Date.now() + Math.random(), role: 'user', content: displayText });

      try {
        const responseType = answerType || currentQuestion.responseType || currentQuestion.type;
        const payload = {
          sessionId,
          questionCode: currentQuestion.code || currentQuestion.id,
          answerType: toBackendAnswerType(responseType),
          answerText: answerText ?? undefined,
          scaleValue: scaleValue ?? undefined,
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
        await pushBot(t.error(err.response?.data?.error));
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
      const userLabel = capturedTranscript
        ? `🎙 "${capturedTranscript}"`
        : t.voiceLabel;
      addMessage({ id: Date.now() + Math.random(), role: 'user', content: userLabel });

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
        await pushBot(t.error(err.response?.data?.error));
      } finally {
        setLoading(false);
      }
    },
    [currentQuestion, sessionId, loading, currentLevel, questionIndex] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const handleContinueLevel = useCallback(async (nextLvl) => {
    setLoading(true);
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

  const handleSessionComplete = useCallback(async () => {
    try {
      await pushBot(t.computingScore);
      const { data: scoreData } = await scoringApi.compute(sessionId);
      const score = scoreData.data ?? scoreData;
      await pushBot(t.scoreMsg(score));
      const { data: reportData } = await reportApi.generate(sessionId);
      const report = reportData.data ?? reportData;
      await pushBot(t.reportReady);
      addMessage({ id: Date.now(), role: 'bot', content: `📄 [Download PDF](${report.pdfUrl})` });
    } catch {
      addMessage({ id: Date.now(), role: 'bot', content: t.reportFail });
    }
  }, [sessionId, language]); // eslint-disable-line react-hooks/exhaustive-deps

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

  return (
    <div className={styles.chatContainer}>
      <div className={styles.chatHeader}>
        <ProgressBar current={displayIndex} total={displayTotal} />
      </div>

      <div className={styles.messageList}>
        {messages.map((m) =>
          m.role === 'divider' ? (
            <SectionDivider key={m.id} label={m.content} />
          ) : (
            <MessageBubble
              key={m.id}
              role={m.role}
              content={m.content}
              onSpeak={m.role === 'bot' ? (text) => { stopSpeak(); speak(text); } : undefined}
            />
          )
        )}
        {typingBot && <MessageBubble role="bot" isLoading />}
        <div ref={bottomRef} />
      </div>

      {!isCompleted && !typingBot && currentQuestion && (
        <div className={styles.inputArea}>
          <QuestionInput
            question={normalisedQuestion}
            onSubmit={handleAnswer}
            language={language}
            isRecording={isRecording}
            isListening={isListening}
            onMicToggle={loading ? undefined : handleMicToggle}
            voiceTranscript={transcript}
          />
        </div>
      )}
    </div>
  );
}

