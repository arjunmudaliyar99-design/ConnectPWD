import styles from '../../styles/chat.module.css';
import TypingIndicator from './TypingIndicator';

export default function MessageBubble({ role, content, isLoading, onSpeak }) {
  const isBot = role === 'bot';
  return (
    <div className={`${styles.message} ${isBot ? styles.bot : styles.user}`}>
      {isBot && <div className={styles.avatar}>C</div>}
      <div className={styles.bubbleCol}>
        <div className={`${styles.bubble} ${isBot ? styles.bot : styles.user}`}>
          {isLoading ? <TypingIndicator /> : content}
        </div>
        {isBot && !isLoading && onSpeak && (
          <button
            className={styles.speakerBtn}
            onClick={() => onSpeak(content)}
            title="Read aloud"
            aria-label="Read aloud"
          >
            🔊
          </button>
        )}
      </div>
    </div>
  );
}
