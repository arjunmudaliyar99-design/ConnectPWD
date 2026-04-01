import styles from './TopBar.module.css';
import LanguageToggle from './LanguageToggle';
import ProgressBar from './ProgressBar';
import { useAuthStore } from '../store/authStore';

export default function TopBar({ currentLevel, progress }) {
  const logout = useAuthStore((s) => s.logout);

  return (
    <header className={styles.topBar}>
      <div className={styles.left}>
        <span className={styles.logo}>ConnectPWD</span>
        {currentLevel && (
          <span className={styles.level}>Level {currentLevel}</span>
        )}
      </div>
      <div className={styles.center}>
        {progress != null && <ProgressBar value={progress} />}
      </div>
      <div className={styles.right}>
        <LanguageToggle />
        <button className={styles.logoutBtn} onClick={logout}>
          Logout
        </button>
      </div>
    </header>
  );
}
