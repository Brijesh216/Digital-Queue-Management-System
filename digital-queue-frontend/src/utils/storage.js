// frontend/src/utils/storage.js
// localStorage utility functions for JWT token and user data management

/**
 * Store JWT token for this browser session.
 * sessionStorage survives refreshes while keeping tabs/windows isolated.
 * Called after successful login
 * 
 * @param {string} token - JWT token from backend
 */
export const setAuthToken = (token) => {
  if (token) {
    sessionStorage.setItem('auth_token', token);
    // Set default Authorization header for all future requests
    sessionStorage.setItem('token_timestamp', new Date().toISOString());
  }
};

/**
 * Retrieve JWT token for this browser session
 * Used by Axios interceptor to attach to requests
 * 
 * @returns {string|null} JWT token or null if not stored
 */
export const getAuthToken = () => {
  return sessionStorage.getItem('auth_token');
};

/**
 * Store user data for this browser session
 * 
 * @param {object} user - User object { id, name, role, email }
 */
export const setUser = (user) => {
  if (user) {
    sessionStorage.setItem('user', JSON.stringify(user));
  }
};

/**
 * Retrieve user data for this browser session
 * 
 * @returns {object|null} User object or null
 */
export const getUser = () => {
  const user = sessionStorage.getItem('user');
  return user ? JSON.parse(user) : null;
};

/**
 * Clear all authentication data from this browser session
 * Called on logout
 */
export const clearAuth = () => {
  sessionStorage.removeItem('auth_token');
  sessionStorage.removeItem('user');
  sessionStorage.removeItem('token_timestamp');
  localStorage.removeItem('auth_token');
  localStorage.removeItem('user');
  localStorage.removeItem('token_timestamp');
};

/**
 * Check if user is authenticated (has token)
 * 
 * @returns {boolean}
 */
export const isAuthenticated = () => {
  return !!getAuthToken();
};
