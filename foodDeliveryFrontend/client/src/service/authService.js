// src/services/authService.js
import axios from 'axios';

const API_URL = 'http://localhost:8081/api';

export const authService = {
  // Sign up new user
  register: (userData) => {
    return axios.post(`${API_URL}/auth/signup`, userData);
  },

  // Sign in user
  login: (username, password) => {
    return axios.post(`${API_URL}/auth/signin`, { username, password });
  },

  // Request password reset
  requestPasswordReset: (email) => {
    return axios.post(`${API_URL}/password/forgot?email=${email}`);
  },

  // Reset password with token
  resetPassword: (token, newPassword) => {
    return axios.post(`${API_URL}/password/reset`, { token, newPassword });
  },

  // Validate reset token
  validateToken: (token) => {
    return axios.get(`${API_URL}/password/validate?token=${token}`);
  }
};