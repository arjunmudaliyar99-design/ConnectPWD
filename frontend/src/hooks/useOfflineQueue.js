import { useCallback, useEffect, useRef } from 'react';

const QUEUE_KEY = 'connectpwd-offline-queue';

function getQueue() {
  try {
    return JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]');
  } catch {
    return [];
  }
}

function saveQueue(queue) {
  localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
}

export function useOfflineQueue(submitFn) {
  const processingRef = useRef(false);

  const enqueue = useCallback((payload) => {
    const queue = getQueue();
    queue.push({ payload, timestamp: Date.now() });
    saveQueue(queue);
  }, []);

  const processQueue = useCallback(async () => {
    if (processingRef.current) return;
    processingRef.current = true;

    const queue = getQueue();
    const remaining = [];

    for (const item of queue) {
      try {
        await submitFn(item.payload);
      } catch {
        remaining.push(item);
      }
    }

    saveQueue(remaining);
    processingRef.current = false;
  }, [submitFn]);

  useEffect(() => {
    const handleOnline = () => processQueue();
    window.addEventListener('online', handleOnline);

    if (navigator.onLine) {
      processQueue();
    }

    return () => window.removeEventListener('online', handleOnline);
  }, [processQueue]);

  return { enqueue, processQueue, queueLength: getQueue().length };
}
