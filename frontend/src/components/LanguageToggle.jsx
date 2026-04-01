import { useAuthStore } from '../store/authStore';
import styles from './LanguageToggle.module.css';

export default function LanguageToggle() {
  const language = useAuthStore((s) => s.language);
  const setLanguage = useAuthStore((s) => s.setLanguage);

  return (
    <button
      className={styles.toggle}
      onClick={() => setLanguage(language === 'en' ? 'hi' : 'en')}
      aria-label="Toggle language"
    >
      {language === 'en' ? 'हिंदी' : 'English'}
    </button>
  );
}
