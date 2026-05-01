import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useSessionStore } from '../store/sessionStore';
import { sessionApi } from '../api/endpoints';
import MessageBubble from '../components/chat/MessageBubble';
import styles from '../styles/chat.module.css';

const STRINGS = {
  en: {
    greeting: "Hi! I'm ConnectPWD's assessment assistant. Let me ask you a few quick questions to get started.",
    q1: "Who are you seeking support for?",
    q1opts: [
      { label: 'Myself (adult)', value: 'SELF' },
      { label: 'My child', value: 'MY_CHILD' },
      { label: 'Our organisation', value: 'ORGANISATION' },
    ],
    q2: "What is the age of the person being assessed?",
    q2placeholder: "Enter age (years)",
    q3: "What kind of support are you looking for?",
    q3opts: [
      { label: 'Immediate assessment & guidance', value: 'IMMEDIATE' },
      { label: 'Long-term counselling', value: 'LONG_TERM' },
    ],
    orgSoon: "Organisation assessments are coming soon. Stay tuned!",
    childSoon: "Children's self-assessment (under 18) is coming soon.",
    counsellingSoon: "Our counselling vertical is coming soon. For now, let's proceed with the assessment.",
    starting: "Great! Starting your assessment now…",
    error: "Something went wrong. Please try again.",
    send: 'Next',
  },
  hi: {
    greeting: "नमस्ते! मैं ConnectPWD का असेसमेंट असिस्टेंट हूँ। शुरुआत के लिए मुझे कुछ त्वरित प्रश्न पूछने दें।",
    q1: "आप किसके लिए सहायता चाहते हैं?",
    q1opts: [
      { label: 'स्वयं के लिए (वयस्क)', value: 'SELF' },
      { label: 'मेरे बच्चे के लिए', value: 'MY_CHILD' },
      { label: 'हमारे संगठन के लिए', value: 'ORGANISATION' },
    ],
    q2: "मूल्यांकन किए जाने वाले व्यक्ति की आयु क्या है?",
    q2placeholder: "आयु दर्ज करें (वर्ष)",
    q3: "आप किस प्रकार की सहायता खोज रहे हैं?",
    q3opts: [
      { label: 'तत्काल मूल्यांकन और मार्गदर्शन', value: 'IMMEDIATE' },
      { label: 'दीर्घकालिक परामर्श', value: 'LONG_TERM' },
    ],
    orgSoon: "संगठन मूल्यांकन जल्द आ रहा है।",
    childSoon: "बच्चों का स्व-मूल्यांकन (18 वर्ष से कम) जल्द आ रहा है।",
    counsellingSoon: "हमारा काउंसलिंग वर्टिकल जल्द आएगा। अभी के लिए मूल्यांकन के साथ आगे बढ़ते हैं।",
    starting: "बढ़िया! अभी आपका मूल्यांकन शुरू हो रहा है…",
    error: "कुछ गलत हो गया। कृपया पुनः प्रयास करें।",
    send: 'आगे',
  },
};

export default function TriagePage() {
  const navigate = useNavigate();
  const language = useAuthStore((s) => s.language) || 'en';
  const setTriageData = useSessionStore((s) => s.setTriageData);
  const setSession = useSessionStore((s) => s.setSession);
  const t = STRINGS[language] || STRINGS.en;

  const [messages, setMessages] = useState([]);
  const [step, setStep] = useState(0);
  const [seeking, setSeeking] = useState(null);
  const [age, setAge] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    // Greeting + Q1
    setMessages([
      { id: 1, role: 'bot', content: t.greeting },
      { id: 2, role: 'bot', content: t.q1 },
    ]);
    setStep(1);
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const addBot = (content) => {
    setMessages((prev) => [...prev, { id: Date.now() + Math.random(), role: 'bot', content }]);
  };

  const addUser = (content) => {
    setMessages((prev) => [...prev, { id: Date.now() + Math.random(), role: 'user', content }]);
  };

  const handleSeekingSelect = (opt) => {
    addUser(opt.label);
    setSeeking(opt.value);

    if (opt.value === 'ORGANISATION') {
      setTimeout(() => addBot(t.orgSoon), 400);
      setStep(99);
      return;
    }
    setTimeout(() => {
      addBot(t.q2);
      setStep(2);
    }, 400);
  };

  const handleAgeSubmit = () => {
    const n = parseInt(age, 10);
    if (isNaN(n) || n < 1 || n > 120) return;
    addUser(String(n));

    if (seeking === 'SELF' && n < 18) {
      setTimeout(() => addBot(t.childSoon), 400);
      setStep(99);
      return;
    }
    setTimeout(() => {
      addBot(t.q3);
      setStep(3);
    }, 400);
  };

  const handleChallengeSelect = async (opt) => {
    addUser(opt.label);
    const challengeType = opt.value;

    if (challengeType === 'LONG_TERM') {
      setTimeout(() => addBot(t.counsellingSoon), 400);
    }

    const seekingFor = seeking === 'SELF' ? 'SELF' : 'MY_CHILD';
    const moduleType = seekingFor === 'SELF' ? 'ADULT_SELF' : 'PARENT';
    const triagePayload = {
      seekingFor,
      age: parseInt(age, 10),
      challengeType,
    };

    setTimeout(async () => {
      addBot(t.starting);
      setLoading(true);
      setTriageData(triagePayload);
      try {
        const res = await sessionApi.start({ moduleType, triageData: triagePayload, language });
        const data = res.data?.data ?? res.data;
        setSession({
          sessionId: data.sessionId,
          moduleType: data.moduleType,
          currentLevel: data.currentLevel,
          currentQuestion: data.currentQuestion,
          status: data.status,
          totalQuestions: data.totalQuestions,
        });
        navigate('/assess');
      } catch {
        addBot(t.error);
      } finally {
        setLoading(false);
      }
    }, challengeType === 'LONG_TERM' ? 800 : 400);
  };

  return (
    <div className={styles.chatContainer}>
      <div className={styles.messageList}>
        {messages.map((m) => (
          <MessageBubble key={m.id} role={m.role} content={m.content} />
        ))}
        {loading && <MessageBubble role="bot" isLoading />}
        <div ref={bottomRef} />
      </div>

      {step === 1 && (
        <div className={styles.inputArea}>
          <div className={styles.chipGroup}>
            {t.q1opts.map((opt) => (
              <button
                key={opt.value}
                className={styles.chip}
                onClick={() => handleSeekingSelect(opt)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {step === 2 && (
        <div className={styles.inputArea}>
          <div className={styles.inputWrapper}>
            <input
              type="number"
              className={styles.numberField}
              value={age}
              min={1}
              max={120}
              onChange={(e) => setAge(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleAgeSubmit()}
              placeholder={t.q2placeholder}
              autoFocus
            />
            <div className={styles.sendRow}>
              <button className={styles.sendBtn} disabled={!age} onClick={handleAgeSubmit}>
                {t.send}
              </button>
            </div>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className={styles.inputArea}>
          <div className={styles.chipGroup}>
            {t.q3opts.map((opt) => (
              <button
                key={opt.value}
                className={styles.chip}
                onClick={() => handleChallengeSelect(opt)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
