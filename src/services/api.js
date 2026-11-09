import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token if present in localStorage
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cloudclean_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for centralized error formatting and token expiration
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear expired credentials if rejected by backend
      localStorage.removeItem('cloudclean_token');
      localStorage.removeItem('cloudclean_user');
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.errors ||
      error.message ||
      'An unexpected network error occurred.';

    return Promise.reject(new Error(typeof message === 'object' ? JSON.stringify(message) : message));
  }
);

export default apiClient;
