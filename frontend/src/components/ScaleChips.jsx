import styles from './ScaleChips.module.css';

const SCALE_LABELS = {
  en: ['Rarely', 'Sometimes', 'Frequently', 'Mostly', 'Always'],
  hi: ['कभी-कभार', 'कभी-कभी', 'अक्सर', 'ज़्यादातर', 'हमेशा'],
};

export default function ScaleChips({ language, onSelect, disabled }) {
  const labels = SCALE_LABELS[language] || SCALE_LABELS.en;

  return (
    <div className={styles.container}>
      {labels.map((label, i) => (
        <button
          key={i + 1}
          className={styles.chip}
          onClick={() => onSelect(i + 1, label)}
          disabled={disabled}
        >
          <span className={styles.value}>{i + 1}</span>
          <span className={styles.label}>{label}</span>
        </button>
      ))}
    </div>
  );
}
