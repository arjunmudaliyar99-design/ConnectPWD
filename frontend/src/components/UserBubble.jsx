import styles from './UserBubble.module.css';

export default function UserBubble({ text }) {
  return (
    <div className={styles.bubble}>
      <div className={styles.content}>{text}</div>
    </div>
  );
}
