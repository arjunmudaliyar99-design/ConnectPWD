import styles from './LevelComplete.module.css';

export default function LevelComplete({ level, nextLevel, onContinue }) {
  return (
    <div className={styles.container}>
      <div className={styles.icon}>✅</div>
      <h2 className={styles.title}>Level {level} Complete!</h2>
      {nextLevel ? (
        <>
          <p className={styles.text}>Ready to proceed to Level {nextLevel}?</p>
          <button className={styles.button} onClick={onContinue}>
            Continue to Level {nextLevel}
          </button>
        </>
      ) : (
        <p className={styles.text}>Assessment complete! Generating your report...</p>
      )}
    </div>
  );
}
