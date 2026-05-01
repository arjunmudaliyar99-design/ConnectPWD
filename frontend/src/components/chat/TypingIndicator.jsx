import styles from '../../styles/chat.module.css';

export default function TypingIndicator() {
  return (
    <div className={styles.typingDots}>
      <span className={styles.dot} />
      <span className={styles.dot} />
      <span className={styles.dot} />
    </div>
  );
}
