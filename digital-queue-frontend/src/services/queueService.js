import api from './api';

const unwrap = (response) => response.data?.data ?? response.data;

const queueService = {
  async createQueue(queueData) {
    const response = await api.post('/queues', queueData);
    return unwrap(response);
  },

  async getQueues() {
    const response = await api.get('/queues');
    return unwrap(response) || [];
  },

  async getQueue(queueId) {
    const response = await api.get(`/queues/${queueId}`);
    return unwrap(response);
  },

  async getWaitingTokens(queueId) {
    const response = await api.get(`/queues/${queueId}/waiting-tokens`);
    return unwrap(response) || [];
  },

  async getActiveToken(queueId) {
    try {
      const response = await api.get(`/queues/${queueId}/active-token`);
      return unwrap(response);
    } catch (error) {
      if (error.response?.status === 404) return null;
      throw error;
    }
  },

  async joinQueue(queueId) {
    const response = await api.post(`/queues/${queueId}/join`, {});
    return unwrap(response);
  },

  async getMyToken(queueId) {
    const response = await api.get(`/queues/${queueId}/my-token`);
    return unwrap(response);
  },

  async callNextToken(queueId) {
    const response = await api.post(`/admin/queues/${queueId}/next`);
    return unwrap(response);
  },

  async completeToken(tokenId) {
    const response = await api.post(`/admin/tokens/${tokenId}/complete`);
    return unwrap(response);
  },

  async skipToken(tokenId) {
    const response = await api.post(`/admin/tokens/${tokenId}/skip`);
    return unwrap(response);
  },

  async pauseQueue(queueId) {
    const response = await api.post(`/admin/queues/${queueId}/pause`);
    return unwrap(response);
  },

  async resumeQueue(queueId) {
    const response = await api.post(`/admin/queues/${queueId}/resume`);
    return unwrap(response);
  },
};

export default queueService;
