import styles from './BotBubble.module.css';

export default function BotBubble({ text }) {
  return (
    <div className={styles.bubble}>
      <span className={styles.avatar}>🤖</span>
      <div className={styles.content}>{text}</div>
    </div>
  );
}
