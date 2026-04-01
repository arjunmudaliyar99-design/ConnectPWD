import api from './axiosInstance';

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
};

export const consentApi = {
  submit: (data) => api.post('/consent', data),
};

export const sessionApi = {
  start: (data) => api.post('/session/start', data),
  get: (sessionId) => api.get(`/session/${sessionId}`),
  advanceLevel: (sessionId, data) => api.post(`/session/${sessionId}/advance`, data),
};

export const answerApi = {
  submitText: (data) => api.post('/answer/text', data),
  submitVoice: (sessionId, questionCode, audioBlob) => {
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('questionCode', questionCode);
    formData.append('audio', audioBlob, 'recording.webm');
    return api.post('/answer/voice', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const scoringApi = {
  compute: (sessionId) => api.post(`/session/${sessionId}/score`),
  get: (sessionId) => api.get(`/session/${sessionId}/score`),
};

export const reportApi = {
  generate: (sessionId) => api.post(`/session/${sessionId}/report`),
  get: (sessionId) => api.get(`/session/${sessionId}/report`),
};
