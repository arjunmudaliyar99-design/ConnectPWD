import styles from './ProgressBar.module.css';

export default function ProgressBar({ value }) {
  const pct = Math.min(100, Math.max(0, value));
  return (
    <div className={styles.track} role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
      <div className={styles.fill} style={{ width: `${pct}%` }} />
    </div>
  );
}
