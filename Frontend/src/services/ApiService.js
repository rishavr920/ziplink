import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '';

export const ApiService = {
  shortenUrl: async (payload) => {
    const response = await axios.post(`${API_URL}/api/shorten`, payload);
    return response.data;
  },

  verifyPassword: async (code, password) => {
    const response = await axios.post(`${API_URL}/api/verify-password/${code}`, { password });
    return response.data;
  },

  getRedirectInfo: async (code) => {
    const response = await axios.get(`${API_URL}/api/redirect/${code}`);
    return response.data;
  }
};
