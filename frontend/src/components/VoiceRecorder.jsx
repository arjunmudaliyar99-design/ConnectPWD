import { useVoiceRecorder } from '../hooks/useVoiceRecorder';
import styles from './VoiceRecorder.module.css';

export default function VoiceRecorder({ onRecorded, disabled }) {
  const { isRecording, startRecording, stopRecording } = useVoiceRecorder();

  const handleToggle = async () => {
    if (isRecording) {
      const blob = await stopRecording();
      if (blob && onRecorded) {
        onRecorded(blob);
      }
    } else {
      startRecording();
    }
  };

  return (
    <button
      className={`${styles.btn} ${isRecording ? styles.recording : ''}`}
      onClick={handleToggle}
      disabled={disabled}
      aria-label={isRecording ? 'Stop recording' : 'Start recording'}
    >
      {isRecording ? '⏹ Stop' : '🎙 Record'}
    </button>
  );
}
