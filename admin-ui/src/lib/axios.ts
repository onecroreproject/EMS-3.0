import axios from 'axios';

// In production, you can set VITE_API_URL in your .env file
const baseURL = import.meta.env.VITE_API_URL || '';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token automatically to every API call
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle expired tokens automatically
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token is expired or invalid
      localStorage.removeItem('adminToken');
      sessionStorage.removeItem('adminToken');
      
      // Redirect to login page if we aren't already there
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
