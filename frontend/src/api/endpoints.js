import api from './axiosInstance';

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
};

export const sessionApi = {
  start: ({ moduleType, triageData, language }) =>
    api.post('/session/start', { moduleType, triageData, language }),
  get: (sessionId) => api.get(`/session/${sessionId}`),
  advanceLevel: (sessionId, data) => api.post(`/session/${sessionId}/advance`, data),
};

export const answerApi = {
  submitText: (data) => api.post('/answer/text', data),
  submitVoice: (sessionId, questionCode, audioBlob, transcript) => {
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('questionCode', questionCode);
    formData.append('audio', audioBlob, 'recording.webm');
    if (transcript) formData.append('transcript', transcript);
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

export const adminApi = {
  stats: () => api.get('/admin/stats'),
  listUsers: (page = 0, size = 20) =>
    api.get('/admin/users', { params: { page, size, sortBy: 'createdAt', dir: 'desc' } }),
  listSessions: (page = 0, size = 20) =>
    api.get('/admin/sessions', { params: { page, size, sortBy: 'startedAt', dir: 'desc' } }),
  listResponses: (sessionId) => api.get(`/admin/sessions/${sessionId}/responses`),
  getScore: (sessionId) => api.get(`/admin/sessions/${sessionId}/score`),
};
