import styles from '../../styles/chat.module.css';
import TypingIndicator from './TypingIndicator';

export default function MessageBubble({ role, content, isLoading, onSpeak, isActivelySpeaking = false, onStopSpeak, isTtsMuted = false }) {
  const isBot = role === 'bot';

  const speakerIcon = isTtsMuted ? '🔇' : isActivelySpeaking ? '🔇' : '🔊';
  const speakerLabel = isTtsMuted
    ? 'TTS muted — click global toggle to unmute'
    : isActivelySpeaking
    ? 'Stop speech'
    : 'Read aloud';

  const handleSpeakerClick = () => {
    if (isTtsMuted) return;
    if (isActivelySpeaking) {
      onStopSpeak?.();
    } else {
      onSpeak?.(content);
    }
  };

  return (
    <div className={`${styles.message} ${isBot ? styles.bot : styles.user}`}>
      {isBot && <div className={styles.avatar}>C</div>}
      <div className={styles.bubbleCol}>
        <div className={`${styles.bubble} ${isBot ? styles.bot : styles.user}`}>
          {isLoading ? <TypingIndicator /> : content}
        </div>
        {isBot && !isLoading && (onSpeak || onStopSpeak) && (
          <button
            className={`${styles.speakerBtn} ${isActivelySpeaking && !isTtsMuted ? styles.speakerBtnActive : ''} ${isTtsMuted ? styles.speakerBtnMuted : ''}`}
            onClick={handleSpeakerClick}
            title={speakerLabel}
            aria-label={speakerLabel}
            aria-pressed={isActivelySpeaking && !isTtsMuted}
            aria-disabled={isTtsMuted}
            type="button"
          >
            {speakerIcon}
          </button>
        )}
      </div>
    </div>
  );
}
