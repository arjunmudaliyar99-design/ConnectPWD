import { useState } from 'react';
import styles from './InputBar.module.css';

export default function InputBar({ onSend, disabled, placeholder }) {
  const [text, setText] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
  };

  return (
    <form className={styles.bar} onSubmit={handleSubmit}>
      <input
        className={styles.input}
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder || 'Type your answer...'}
        disabled={disabled}
        autoFocus
      />
      <button className={styles.sendBtn} type="submit" disabled={disabled || !text.trim()}>
        Send
      </button>
    </form>
  );
}
