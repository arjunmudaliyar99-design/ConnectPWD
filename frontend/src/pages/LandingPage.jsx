import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/endpoints';
import LanguageToggle from '../components/LanguageToggle';
import styles from './LandingPage.module.css';

export default function LandingPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const language = useAuthStore((s) => s.language);

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    phone: '',
    role: 'CAREGIVER',
  });

  const isHindi = language === 'hi';

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const payload = isLogin
        ? { email: form.email, password: form.password }
        : { ...form, language };

      const { data } = isLogin
        ? await authApi.login(payload)
        : await authApi.register(payload);

      setAuth(data.data);
      navigate('/consent');
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.langBar}>
        <LanguageToggle />
      </div>

      <div className={styles.hero}>
        <h1 className={styles.title}>ConnectPWD</h1>
        <p className={styles.subtitle}>
          {isHindi
            ? 'विकलांग व्यक्तियों के लिए भारत का पहला वॉयस-सक्षम, द्विभाषी ISAA मूल्यांकन प्लेटफॉर्म'
            : "India's first voice-enabled, bilingual ISAA assessment platform for persons with disabilities"}
        </p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab} ${isLogin ? styles.activeTab : ''}`}
            onClick={() => setIsLogin(true)}
          >
            {isHindi ? 'लॉगिन' : 'Login'}
          </button>
          <button
            type="button"
            className={`${styles.tab} ${!isLogin ? styles.activeTab : ''}`}
            onClick={() => setIsLogin(false)}
          >
            {isHindi ? 'रजिस्टर' : 'Register'}
          </button>
        </div>

        {!isLogin && (
          <>
            <input
              name="fullName"
              className={styles.input}
              placeholder={isHindi ? 'पूरा नाम' : 'Full Name'}
              value={form.fullName}
              onChange={handleChange}
              required
            />
            <input
              name="phone"
              className={styles.input}
              placeholder={isHindi ? 'फोन नंबर' : 'Phone Number'}
              value={form.phone}
              onChange={handleChange}
            />
            <select name="role" className={styles.input} value={form.role} onChange={handleChange}>
              <option value="CAREGIVER">{isHindi ? 'देखभालकर्ता' : 'Caregiver'}</option>
              <option value="PSYCHOLOGIST">{isHindi ? 'मनोविज्ञानी' : 'Psychologist'}</option>
            </select>
          </>
        )}

        <input
          name="email"
          type="email"
          className={styles.input}
          placeholder={isHindi ? 'ईमेल' : 'Email'}
          value={form.email}
          onChange={handleChange}
          required
        />
        <input
          name="password"
          type="password"
          className={styles.input}
          placeholder={isHindi ? 'पासवर्ड' : 'Password'}
          value={form.password}
          onChange={handleChange}
          required
          minLength={8}
        />

        {error && <p className={styles.error}>{error}</p>}

        <button className={styles.button} type="submit" disabled={loading}>
          {loading ? '...' : isLogin ? (isHindi ? 'लॉगिन' : 'Login') : (isHindi ? 'रजिस्टर' : 'Register')}
        </button>
      </form>
    </div>
  );
}
