// frontend/src/services/authService.js
// Authentication service - handles login, register, and auth operations
// Uses Axios to communicate with backend

import api from './api';
import { setAuthToken, setUser, clearAuth } from '../utils/storage';

/**
 * Authentication Service
 * 
 * Central place for all auth-related API calls
 * Each method corresponds to a backend endpoint
 * Handles token storage and user state management
 */

export const authService = {
  /**
   * Login User
   * 
   * API Endpoint: POST /api/auth/login
   * 
   * Request:
   * {
   *   identifier: "brijesh",
   *   password: "password123"
   * }
   * 
   * Response:
   * {
   *   token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
   *   user: {
   *     id: 1,
   *     name: "Brijesh",
   *     email: "brijesh@example.com",
   *     role: "USER"
   *   }
   * }
   * 
   * Error Response (401):
   * {
   *   message: "Invalid username or password"
   * }
   * 
   * Process:
   * 1. Component collects username and password from form
   * 2. Calls authService.login(username, password)
   * 3. Axios sends POST request with credentials
   * 4. Backend validates in AuthController
   * 5. AuthService checks credentials against DB
   * 6. If valid: Generate JWT and return token
   * 7. If invalid: Return 401 error
   * 8. Frontend catches response:
   *    - If success: Save token & user, redirect to dashboard
   *    - If error: Show error message
   * 
   * @param {string} username - Username
   * @param {string} password - Password
   * @returns {Promise<object>} { token, user }
   * @throws {AxiosError} Login failed (400, 401, 500, etc)
   */
  login: async (username, password) => {
    try {
      console.log('🔐 [AuthService] Attempting login for:', username);
      
      // Make POST request to backend
      const response = await api.post('/auth/login', {
        identifier: username,
        password,
      });

      const loginData = response.data?.data || response.data || {};
      const token = loginData.accessToken || response.data?.token;
      const user = loginData.user || response.data?.user || null;

      // Save token to localStorage
      setAuthToken(token);

      // Save user data to localStorage
      setUser(user);

      console.log('✅ [AuthService] Login successful for:', user.name);
      
      return { token, user };
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Login failed';
      console.error('❌ [AuthService] Login error:', errorMsg);
      throw error;
    }
  },

  /**
   * Register New User
   * 
   * API Endpoint: POST /api/auth/register
   * 
   * Request:
   * {
   *   username: "newuser",
   *   email: "newuser@example.com",
   *   password: "password123",
   *   name: "New User"
   * }
   * 
   * Response:
   * {
   *   message: "User registered successfully"
   * }
   * 
   * Error (400):
   * {
   *   message: "Username already exists"
   * }
   * 
   * @param {string} username - Username
   * @param {string} email - Email
   * @param {string} password - Password
   * @param {string} name - Full name (optional)
   * @returns {Promise<object>} Success message
   * @throws {AxiosError} Registration failed
   */
  register: async (username, email, password, confirmPassword, fullName = '', phone = '') => {
    try {
      console.log('📝 [AuthService] Registering user:', username);
      
      // Split full name into first and last name
      const nameParts = fullName.trim().split(' ');
      const firstName = nameParts[0] || '';
      const lastName = nameParts.slice(1).join(' ') || '';
      
      const response = await api.post('/auth/register', {
        username,
        email,
        password,
        confirmPassword,
        firstName,
        lastName,
        phoneNumber: phone,
      });

      console.log('✅ [AuthService] Registration successful');
      return response.data;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Registration failed';
      console.error('❌ [AuthService] Registration error:', errorMsg);
      throw error;
    }
  },

  /**
   * Verify Token
   * 
   * Check if stored JWT token is still valid
   * 
   * API Endpoint: GET /api/auth/verify
   * Requires: Valid JWT in Authorization header
   * 
   * Response:
   * {
   *   id: 1,
   *   name: "Brijesh",
   *   email: "brijesh@example.com",
   *   role: "USER"
   * }
   * 
   * Used to:
   * - Restore session on app startup
   * - Check if token expired
   * - Validate permissions
   * 
   * @returns {Promise<object>} User data if token valid
   * @throws {AxiosError} 401 if token invalid
   */
  verify: async () => {
    try {
      console.log(' [AuthService] Verifying token...');
      
      const response = await api.get('/auth/verify');
      
      console.log(' [AuthService] Token verified');
      return response.data?.data || response.data;
    } catch (error) {
      console.error(' [AuthService] Token verification failed');
      throw error;
    }
  },

  /**
   * Logout User
   * 
   * Clears authentication data from storage
   * Note: No backend endpoint needed (stateless JWT)
   * Just clear client-side data
   * 
   * Process:
   * 1. Clear token from localStorage
   * 2. Clear user data from localStorage
   * 3. Axios interceptor won't attach token to future requests
   * 4. Next request to protected endpoint returns 401
   * 5. Redirect to login
   */
  logout: () => {
    console.log(' [AuthService] Logging out');
    clearAuth();
    console.log(' [AuthService] Auth data cleared');
  },

  /**
   * Get Current User from Storage
   * 
   * Returns cached user data from localStorage
   * (Does not make API call)
   * 
   * @returns {object|null} User data or null
   */
  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  /**
   * Refresh Token (if backend supports)
   * 
   * API Endpoint: POST /api/auth/refresh
   * 
   * Used to extend session without re-login
   * Some JWT implementations refresh token on every use
   * 
   * @returns {Promise<object>} New token
   * @throws {AxiosError} If refresh not supported
   */
  refreshToken: async () => {
    try {
      console.log('[AuthService] Refreshing token...');
      
      const response = await api.post('/auth/refresh');
      const { token } = response.data;
      
      // Update stored token
      setAuthToken(token);
      
      console.log(' [AuthService] Token refreshed');
      return response.data;
    } catch (error) {
      console.error(' [AuthService] Token refresh failed');
      throw error;
    }
  },
};

export default authService;

/**
 * Usage Examples:
 * 
 * // Login
 * try {
 *   const { token, user } = await authService.login('john', 'pass123');
 *   console.log('Logged in as:', user.name);
 * } catch (error) {
 *   console.error('Login failed:', error.response?.data?.message);
 * }
 * 
 * // Register
 * try {
 *   await authService.register('newuser', 'new@example.com', 'pass123');
 *   console.log('Registration successful');
 * } catch (error) {
 *   console.error('Registration failed:', error.response?.data?.message);
 * }
 * 
 * // Verify token on app startup
 * try {
 *   const user = await authService.verify();
 *   setUser(user);
 * } catch (error) {
 *   // Token invalid, clear storage and redirect to login
 *   authService.logout();
 * }
 * 
 * // Logout
 * authService.logout();
 * navigate('/login');
 */
