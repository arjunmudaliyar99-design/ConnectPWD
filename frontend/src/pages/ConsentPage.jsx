import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useSessionStore } from '../store/sessionStore';
import { consentApi, sessionApi } from '../api/endpoints';
import TopBar from '../components/TopBar';
import ConsentForm from '../components/ConsentForm';
import styles from './ConsentPage.module.css';

export default function ConsentPage() {
  const navigate = useNavigate();
  const language = useAuthStore((s) => s.language);
  const setSession = useSessionStore((s) => s.setSession);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (form) => {
    setLoading(true);
    setError('');

    try {
      const { data: consentData } = await consentApi.submit(form);
      const consentId = consentData.data.id;

      const { data: sessionData } = await sessionApi.start({
        consentId,
        language,
      });

      const s = sessionData.data;
      setSession({
        sessionId: s.sessionId,
        consentId,
        currentLevel: s.currentLevel,
        currentQuestion: s.currentQuestion,
        status: s.status,
      });

      navigate('/assess');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit consent');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <TopBar />
      <main className={styles.main}>
        {error && <p className={styles.error}>{error}</p>}
        <ConsentForm language={language} onSubmit={handleSubmit} loading={loading} />
      </main>
    </div>
  );
}
