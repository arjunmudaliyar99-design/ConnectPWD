import styles from './DomainHeader.module.css';

export default function DomainHeader({ text }) {
  return (
    <div className={styles.header}>
      <span className={styles.line} />
      <span className={styles.text}>{text}</span>
      <span className={styles.line} />
    </div>
  );
}
