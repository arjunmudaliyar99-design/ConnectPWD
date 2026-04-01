import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { useSessionStore } from '../store/sessionStore';
import { answerApi, scoringApi, reportApi, sessionApi } from '../api/endpoints';
import { useSpeechOutput } from '../hooks/useSpeechOutput';
import TopBar from '../components/TopBar';
import ChatWindow from '../components/ChatWindow';
import InputBar from '../components/InputBar';
import ScaleChips from '../components/ScaleChips';
import ChipOptions from '../components/ChipOptions';
import VoiceRecorder from '../components/VoiceRecorder';
import LevelComplete from '../components/LevelComplete';
import styles from './AssessmentPage.module.css';

export default function AssessmentPage() {
  const language = useAuthStore((s) => s.language);
  const {
    sessionId,
    currentLevel,
    currentQuestion,
    messages,
    status,
    addMessage,
    setCurrentQuestion,
    setCurrentLevel,
    setStatus,
  } = useSessionStore();

  const { speak } = useSpeechOutput();
  const [loading, setLoading] = useState(false);
  const [lastDomain, setLastDomain] = useState(null);

  // Present the current question as a bot message
  useEffect(() => {
    if (!currentQuestion) return;
    const q = currentQuestion;
    const text = language === 'hi' ? (q.textHi || q.textEn) : q.textEn;
    const domain = language === 'hi' ? (q.domainNameHi || q.domainNameEn) : q.domainNameEn;

    if (q.isFirstInDomain && domain !== lastDomain) {
      addMessage({ type: 'domain', text: domain });
      setLastDomain(domain);
    }

    addMessage({ type: 'bot', text });
    speak(text);
  }, [currentQuestion?.code]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAnswer = useCallback(
    async (answerText, scaleValue) => {
      if (!currentQuestion || loading) return;
      setLoading(true);

      addMessage({ type: 'user', text: answerText });

      try {
        const payload = {
          sessionId,
          questionCode: currentQuestion.code,
          answerType: currentQuestion.type === 'SCALE' ? 'SCALE' : 'TEXT',
          answerText: currentQuestion.type === 'SCALE' ? undefined : answerText,
          scaleValue: currentQuestion.type === 'SCALE' ? scaleValue : undefined,
        };

        const { data } = await answerApi.submitText(payload);
        const result = data.data;

        if (result.levelComplete) {
          setStatus('LEVEL_COMPLETE');
          setCurrentQuestion(null);
        } else if (result.nextQuestion) {
          setCurrentQuestion(result.nextQuestion);
        }

        if (result.sessionStatus === 'COMPLETED') {
          setStatus('COMPLETED');
          handleSessionComplete();
        }
      } catch (err) {
        addMessage({
          type: 'bot',
          text: err.response?.data?.error || 'Failed to submit answer. Please try again.',
        });
      } finally {
        setLoading(false);
      }
    },
    [currentQuestion, sessionId, loading] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const handleVoiceRecorded = useCallback(
    async (blob) => {
      if (!currentQuestion || loading) return;
      setLoading(true);
      addMessage({ type: 'user', text: '🎙 Voice recorded' });

      try {
        const { data } = await answerApi.submitVoice(sessionId, currentQuestion.code, blob);
        const result = data.data;

        if (result.levelComplete) {
          setStatus('LEVEL_COMPLETE');
          setCurrentQuestion(null);
        } else if (result.nextQuestion) {
          setCurrentQuestion(result.nextQuestion);
        }
      } catch (err) {
        addMessage({
          type: 'bot',
          text: err.response?.data?.error || 'Failed to submit voice. Please try again.',
        });
      } finally {
        setLoading(false);
      }
    },
    [currentQuestion, sessionId, loading] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const handleContinueLevel = useCallback(async () => {
    setLoading(true);
    try {
      const nextLvl = currentLevel + 1;
      const { data } = await sessionApi.advanceLevel(sessionId, { level: nextLvl });
      const s = data.data;
      setCurrentLevel(nextLvl);
      setCurrentQuestion(s.currentQuestion);
      setStatus('IN_PROGRESS');
    } catch (err) {
      addMessage({
        type: 'bot',
        text: err.response?.data?.error || 'Failed to advance level.',
      });
    } finally {
      setLoading(false);
    }
  }, [currentLevel, sessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSessionComplete = useCallback(async () => {
    try {
      addMessage({ type: 'bot', text: language === 'hi' ? 'स्कोर की गणना हो रही है...' : 'Computing score...' });
      const { data: scoreData } = await scoringApi.compute(sessionId);
      const score = scoreData.data;

      addMessage({
        type: 'bot',
        text: language === 'hi'
          ? `कुल स्कोर: ${score.totalScore} | गंभीरता: ${score.severity} | विकलांगता: ${score.disabilityPct}%`
          : `Total Score: ${score.totalScore} | Severity: ${score.severity} | Disability: ${score.disabilityPct}%`,
      });

      addMessage({ type: 'bot', text: language === 'hi' ? 'रिपोर्ट तैयार हो रही है...' : 'Generating report...' });
      const { data: reportData } = await reportApi.generate(sessionId);
      const report = reportData.data;

      addMessage({
        type: 'bot',
        text: language === 'hi' ? 'रिपोर्ट तैयार है! नीचे डाउनलोड करें।' : 'Report ready! Download below.',
      });
      addMessage({
        type: 'bot',
        text: `📄 [Download PDF](${report.pdfUrl})`,
      });
    } catch {
      addMessage({ type: 'bot', text: 'Failed to generate report. Please try again later.' });
    }
  }, [sessionId, language]); // eslint-disable-line react-hooks/exhaustive-deps

  const progress = currentQuestion
    ? ((currentQuestion.currentPositionInLevel || 0) / (currentQuestion.totalInLevel || 1)) * 100
    : 0;

  const isLevelComplete = status === 'LEVEL_COMPLETE';
  const isCompleted = status === 'COMPLETED';
  const questionType = currentQuestion?.type;

  return (
    <div className={styles.page}>
      <TopBar currentLevel={currentLevel} progress={progress} />

      <ChatWindow messages={messages} />

      {isLevelComplete && !isCompleted && (
        <LevelComplete
          level={currentLevel}
          nextLevel={currentLevel < 4 ? currentLevel + 1 : null}
          onContinue={handleContinueLevel}
        />
      )}

      {!isLevelComplete && !isCompleted && currentQuestion && (
        <div className={styles.inputArea}>
          {questionType === 'SCALE' && (
            <ScaleChips
              language={language}
              onSelect={(val, label) => handleAnswer(label, val)}
              disabled={loading}
            />
          )}

          {questionType === 'CHOICE' && currentQuestion.options && (
            <ChipOptions
              options={currentQuestion.options.map((o) => ({
                value: o,
                label: o,
              }))}
              onSelect={(val) => handleAnswer(val)}
              disabled={loading}
            />
          )}

          {questionType === 'TEXT' && (
            <div className={styles.textInputRow}>
              <InputBar
                onSend={(text) => handleAnswer(text)}
                disabled={loading}
                placeholder={language === 'hi' ? 'अपना उत्तर लिखें...' : 'Type your answer...'}
              />
              <VoiceRecorder onRecorded={handleVoiceRecorded} disabled={loading} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
