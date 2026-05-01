import styles from '../../styles/chat.module.css';

export default function SectionDivider({ label }) {
  return (
    <div className={styles.sectionDivider}>
      {label}
    </div>
  );
}
