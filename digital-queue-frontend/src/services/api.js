// frontend/src/services/api.js
// Axios instance with JWT authentication interceptors
// Handles automatic token attachment and error responses

import axios from 'axios';
import { getAuthToken, clearAuth } from '../utils/storage';

/**
 * Axios Instance Configuration
 * 
 * This creates a reusable Axios instance with:
 * 1. Base URL pointing to backend API
 * 2. Request interceptor to attach JWT token
 * 3. Response interceptor to handle auth errors
 * 
 * All API calls in the app should use this instance
 */

// Create base Axios instance
const api = axios.create({
  baseURL: import.meta.env.REACT_APP_API_URL || 'http://localhost:3000/api/v1',
  timeout: 10000, // 10 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * REQUEST INTERCEPTOR
 * 
 * Runs BEFORE every request is sent to backend
 * 
 * Purpose:
 * - Get JWT token from localStorage
 * - Attach token to Authorization header
 * - Format: Authorization: Bearer {token}
 * 
 * Flow:
 * 1. Component calls: api.get('/queue')
 * 2. Interceptor runs: Gets token from storage
 * 3. Attaches header: { Authorization: 'Bearer eyJh...' }
 * 4. Request sent to backend with token
 * 5. Backend JwtAuthenticationFilter validates token
 * 6. If valid: Process request, If invalid: Return 401
 */
api.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    
    // If token exists, add it to the Authorization header
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('✅ [Axios] JWT attached to request');
    } else {
      console.log('⚠️ [Axios] No token found in storage');
    }
    
    return config;
  },
  (error) => {
    console.error('❌ [Axios] Request error:', error);
    return Promise.reject(error);
  }
);

/**
 * RESPONSE INTERCEPTOR
 * 
 * Runs AFTER response is received from backend
 * 
 * Purpose:
 * - Check if response contains errors
 * - Handle 401 (Unauthorized) errors
 * - Clear auth and redirect to login if token invalid
 * - Log errors for debugging
 * 
 * Error Handling:
 * - 401: Token invalid/expired → Clear storage & redirect to /login
 * - 403: Forbidden → User lacks permission
 * - 5xx: Server error → Log and show error
 * - 4xx: Client error → Show error message
 */
api.interceptors.response.use(
  (response) => {
    // Success response - pass through
    console.log('✅ [Axios] Response received:', response.status);
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;

    // Handle 401 Unauthorized (token invalid or expired)
    if (status === 401) {
      console.error('❌ [Axios] 401 Unauthorized - Token invalid or expired');
      
      // Clear authentication data
      clearAuth();
      
      // Redirect to login
      // Note: This assumes you're in browser context
      // For SSR, handle this differently
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
      
      return Promise.reject(error);
    }

    // Handle 403 Forbidden (user lacks permission)
    if (status === 403) {
      console.error('❌ [Axios] 403 Forbidden - User lacks permission');
      return Promise.reject(error);
    }

    // Handle other errors
    if (error.response) {
      // Server responded with error status code
      console.error('❌ [Axios] Response error:', {
        status: status,
        message: message,
        url: error.config?.url,
      });
    } else if (error.request) {
      // Request made but no response received
      console.error('❌ [Axios] No response received:', error.request);
    } else {
      // Error occurred during request setup
      console.error('❌ [Axios] Request setup error:', error.message);
    }

    return Promise.reject(error);
  }
);

export default api;

/**
 * Usage Examples:
 * 
 * // GET request
 * api.get('/queue')
 *   .then(res => console.log(res.data))
 *   .catch(err => console.error(err));
 * 
 * // POST request
 * api.post('/auth/login', { username: 'john', password: 'pass' })
 *   .then(res => console.log(res.data))
 *   .catch(err => console.error(err));
 * 
 * // With async/await
 * try {
 *   const res = await api.get('/queue');
 *   console.log(res.data);
 * } catch (error) {
 *   console.error(error);
 * }
 * 
 * // Custom headers for specific request
 * api.post('/upload', data, {
 *   headers: { 'Content-Type': 'multipart/form-data' }
 * })
 */
