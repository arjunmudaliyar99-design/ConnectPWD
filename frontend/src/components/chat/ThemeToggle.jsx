import { useTheme } from '../../hooks/useTheme';
import styles from './ThemeToggle.module.css';

/**
 * Sliding pill toggle — Sun (light) / Moon (dark).
 * Always visible; 44×44 px touch target minimum.
 */
export default function ThemeToggle() {
  const [isDark, toggleTheme] = useTheme();

  return (
    <button
      className={`${styles.toggle} ${isDark ? styles.dark : styles.light}`}
      onClick={toggleTheme}
      aria-label="Toggle between dark and light mode"
      aria-pressed={!isDark}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      type="button"
    >
      <span className={styles.track}>
        <span className={styles.thumb}>
          <span className={styles.icon} aria-hidden="true">
            {isDark ? '🌙' : '☀️'}
          </span>
        </span>
      </span>
    </button>
  );
}
