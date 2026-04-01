import { useRef, useEffect } from 'react';
import styles from './ChatWindow.module.css';
import BotBubble from './BotBubble';
import UserBubble from './UserBubble';
import DomainHeader from './DomainHeader';

export default function ChatWindow({ messages }) {
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className={styles.chatWindow}>
      {messages.map((msg, i) => {
        if (msg.type === 'domain') return <DomainHeader key={i} text={msg.text} />;
        if (msg.type === 'bot') return <BotBubble key={i} text={msg.text} />;
        if (msg.type === 'user') return <UserBubble key={i} text={msg.text} />;
        return null;
      })}
      <div ref={endRef} />
    </div>
  );
}
