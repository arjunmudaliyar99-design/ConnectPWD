import styles from '../../styles/chat.module.css';

export default function ProgressBar({ current, total }) {
  const pct = total > 0 ? Math.round((current / total) * 100) : 0;
  return (
    <div className={styles.progressWrapper}>
      <div className={styles.progressTrack}>
        <div className={styles.progressFill} style={{ width: `${pct}%` }} />
      </div>
      <span className={styles.progressLabel}>{current}/{total}</span>
    </div>
  );
}
