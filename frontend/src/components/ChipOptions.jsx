import styles from './ChipOptions.module.css';

export default function ChipOptions({ options, onSelect, disabled }) {
  return (
    <div className={styles.container}>
      {options.map((opt) => (
        <button
          key={opt.value}
          className={styles.chip}
          onClick={() => onSelect(opt.value, opt.label)}
          disabled={disabled}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}
