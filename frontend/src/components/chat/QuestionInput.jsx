import { useState, useEffect } from 'react';
import styles from '../../styles/chat.module.css';

const YES_SOMETIMES_NO_OPTIONS = {
  en: ['Yes', 'Sometimes', 'No'],
  hi: ['हाँ', 'कभी-कभी', 'नहीं'],
};

export default function QuestionInput({
  question,
  onSubmit,
  language = 'en',
  isRecording = false,
  isListening = false,
  onMicToggle,
  voiceTranscript = '',
}) {
  const [value, setValue] = useState('');
  const [selected, setSelected] = useState(null);

  // Fill text/number field when voice transcript arrives
  useEffect(() => {
    const t = question?.responseType;
    if (voiceTranscript && t && (t === 'NUMBER' || t.includes('TEXT') || t.includes('SUBJECTIVE') || t.includes('OPEN') || t.includes('DESCRIPTIVE'))) {
      setValue(voiceTranscript);
    }
  }, [voiceTranscript, question?.responseType]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!question) return null;
  const type = question.responseType;
  const micActive = isRecording || isListening;

  const MicButton = () => (
    <button
      className={`${styles.micBtn} ${micActive ? styles.micActive : ''}`}
      onClick={onMicToggle}
      title={micActive ? (language === 'hi' ? 'रिकॉर्डिंग रोकें' : 'Stop recording') : (language === 'hi' ? 'आवाज़ से जवाब दें' : 'Answer by voice')}
      aria-label={micActive ? 'Stop recording' : 'Start recording'}
      type="button"
    >
      {micActive ? '⏹' : '🎙'}
    </button>
  );

  const handleChipSelect = (opt) => {
    setSelected(opt);
    onSubmit({ answerText: opt, answerType: type });
    setSelected(null);
  };

  const handleScaleSelect = (num) => {
    setSelected(num);
    onSubmit({ scaleValue: num, answerType: type });
    setSelected(null);
  };

  const handleTextSubmit = () => {
    if (!value.trim()) return;
    onSubmit({ answerText: value.trim(), answerType: type });
    setValue('');
  };

  const handleNumberSubmit = () => {
    const n = parseInt(value, 10);
    if (isNaN(n)) return;
    onSubmit({ scaleValue: n, answerType: type });
    setValue('');
  };

  if (type === 'YES_SOMETIMES_NO') {
    const opts = YES_SOMETIMES_NO_OPTIONS[language] || YES_SOMETIMES_NO_OPTIONS.en;
    return (
      <div className={styles.inputWrapper}>
        <div className={styles.chipGroup}>
          {opts.map((opt) => (
            <button
              key={opt}
              className={`${styles.chip} ${selected === opt ? styles.selected : ''}`}
              onClick={() => handleChipSelect(opt)}
            >
              {opt}
            </button>
          ))}
        </div>
        <div className={styles.voiceRow}>
          {onMicToggle && <MicButton />}
          {micActive && (
            <span className={styles.voiceStatus}>
              <span className={styles.recordingDot} />
              {language === 'hi' ? 'सुन रहा है…' : 'Listening…'}
            </span>
          )}
        </div>
      </div>
    );
  }

  if (type === 'RATING_SCALE') {
    return (
      <div className={styles.inputWrapper}>
        <div className={styles.scaleRow}>
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              className={`${styles.scaleBtn} ${selected === n ? styles.selected : ''}`}
              onClick={() => handleScaleSelect(n)}
            >
              {n}
            </button>
          ))}
        </div>
        <div className={styles.voiceRow}>
          {onMicToggle && <MicButton />}
          {micActive && (
            <span className={styles.voiceStatus}>
              <span className={styles.recordingDot} />
              {language === 'hi' ? 'सुन रहा है…' : 'Listening…'}
            </span>
          )}
        </div>
      </div>
    );
  }

  if (type === 'NUMBER') {
    return (
      <div className={styles.inputWrapper}>
        <div className={styles.sendRow} style={{ justifyContent: 'flex-start', gap: 10 }}>
          <input
            type="number"
            className={styles.numberField}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleNumberSubmit()}
            placeholder={language === 'hi' ? 'संख्या दर्ज करें' : 'Enter number'}
          />
          {onMicToggle && <MicButton />}
        </div>
        {micActive && (
          <span className={styles.voiceStatus}>
            <span className={styles.recordingDot} />
            {language === 'hi' ? 'सुन रहा है…' : 'Listening…'}
          </span>
        )}
        <div className={styles.sendRow}>
          <button className={styles.sendBtn} disabled={!value} onClick={handleNumberSubmit}>
            {language === 'hi' ? 'भेजें' : 'Send'}
          </button>
        </div>
      </div>
    );
  }

  const maxLen = type === 'DESCRIPTIVE_50' ? 50 : type === 'SUBJECTIVE_150' ? 150 : 300;
  const rows = type === 'DESCRIPTIVE_50' ? 2 : type === 'SUBJECTIVE_150' ? 4 : 6;
  const placeholder = language === 'hi' ? 'यहाँ लिखें...' : 'Type your answer…';

  return (
    <div className={styles.inputWrapper}>
      <textarea
        className={styles.textareaField}
        rows={rows}
        maxLength={maxLen}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={placeholder}
      />
      {micActive && (
        <span className={styles.voiceStatus}>
          <span className={styles.recordingDot} />
          {language === 'hi' ? 'सुन रहा है — बोलें…' : 'Listening — speak now…'}
        </span>
      )}
      <div className={styles.sendRow}>
        <span className={styles.progressLabel}>{value.length}/{maxLen}</span>
        {onMicToggle && <MicButton />}
        <button className={styles.sendBtn} disabled={!value.trim()} onClick={handleTextSubmit}>
          {language === 'hi' ? 'भेजें' : 'Send'}
        </button>
      </div>
    </div>
  );
}

