// frontend/src/context/AuthContext.jsx
// Global Authentication Context
// Manages user state and authentication status across the entire app

import { createContext, useState, useEffect } from 'react';
import authService from '../services/authService';
import { getAuthToken, getUser, setUser as persistUser } from '../utils/storage';

/**
 * AuthContext
 * 
 * Provides authentication state to entire app
 * 
 * Context Values:
 * - user: Current logged-in user object or null
 * - isAuthenticated: Boolean indicating if user is logged in
 * - loading: Boolean indicating if auth check is in progress
 * - error: Error message if auth operation failed
 * - login(): Function to login user
 * - logout(): Function to logout user
 * - register(): Function to register new user
 * 
 * Usage:
 * const { user, isAuthenticated, login, logout } = useContext(AuthContext);
 */
export const AuthContext = createContext();

/**
 * AuthProvider Component
 * 
 * Wraps entire app to provide auth context
 * 
 * Features:
 * - Initializes auth state from localStorage on mount
 * - Verifies JWT token with backend
 * - Handles login/logout operations
 * - Manages loading and error states
 * 
 * Usage in App.jsx:
 * <AuthProvider>
 *   <Routes>
 *     <Route path="/login" element={<Login />} />
 *     <Route path="/dashboard" element={<Dashboard />} />
 *   </Routes>
 * </AuthProvider>
 */
export function AuthProvider({ children }) {
  // Auth state
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  /**
   * Initialize Auth on Mount
   * 
   * Flow:
   * 1. Component mounts
   * 2. Check if token exists in localStorage
   * 3. If yes, verify token with backend
   * 4. If valid, set user state
   * 5. If invalid, clear auth and redirect
   * 6. If no token, set loading=false
   * 
   * This allows user to stay logged in even after page refresh
   */
  useEffect(() => {
    let mounted = true;

    const initializeAuth = async () => {
      try {
        console.log('🔄 [AuthContext] Initializing auth...');
        
        const token = getAuthToken();
        const cachedUser = getUser();

        if (token) {
          console.log('✅ [AuthContext] Found cached token');

          try {
            const verifiedUser = await authService.verify();
            const sameIdentity = !cachedUser || (
              String(cachedUser.id) === String(verifiedUser?.id) &&
              cachedUser.username === verifiedUser?.username
            );

            if (!sameIdentity || !verifiedUser?.id || !verifiedUser?.username) {
              throw new Error('Stored user does not match the authenticated token');
            }

            if (mounted) {
              persistUser(verifiedUser);
              setUser(verifiedUser);
              setIsAuthenticated(true);
              console.log('✅ [AuthContext] Token verified for:', verifiedUser.username);
            }
          } catch (err) {
            console.warn('⚠️ [AuthContext] Token verification failed, clearing auth');
            authService.logout();
            if (mounted) {
              setUser(null);
              setIsAuthenticated(false);
            }
          }
        } else {
          console.log('ℹ️ [AuthContext] No cached auth data');
          if (mounted) {
            setUser(null);
            setIsAuthenticated(false);
          }
        }
      } catch (err) {
        console.error('❌ [AuthContext] Auth initialization error:', err);
        authService.logout();
        if (mounted) {
          setUser(null);
          setIsAuthenticated(false);
          setError('Failed to initialize authentication');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    initializeAuth();

    return () => {
      mounted = false;
    };
  }, []);

  /**
   * Login Handler
   * 
   * Flow:
   * 1. Call authService.login()
   * 2. Wait for API response
   * 3. If success: Update user state, set isAuthenticated=true
   * 4. If error: Set error message
   * 
   * @param {string} username
   * @param {string} password
   * @returns {Promise<object>} { token, user }
   */
  const login = async (username, password) => {
    try {
      setError('');
      setLoading(true);
      
      const { token, user: userData } = await authService.login(username, password);

      setUser(userData);
      setIsAuthenticated(true);
      
      console.log('✅ [AuthContext] Login successful');
      return { token, user: userData };
      
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Login failed';
      setError(errorMsg);
      console.error('❌ [AuthContext] Login error:', errorMsg);
      throw err;
      
    } finally {
      setLoading(false);
    }
  };

  /**
   * Register Handler
   * 
   * @param {string} username
   * @param {string} email
   * @param {string} password
   * @param {string} name
   * @returns {Promise<object>}
   */
  const register = async (username, email, password, name = '') => {
    try {
      setError('');
      setLoading(true);
      
      const result = await authService.register(username, email, password, name);
      
      console.log('✅ [AuthContext] Registration successful');
      return result;
      
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Registration failed';
      setError(errorMsg);
      console.error('❌ [AuthContext] Registration error:', errorMsg);
      throw err;
      
    } finally {
      setLoading(false);
    }
  };

  /**
   * Logout Handler
   * 
   * Clears all auth state
   */
  const logout = () => {
    console.log('👋 [AuthContext] Logging out');
    authService.logout();
    setUser(null);
    setIsAuthenticated(false);
    setError('');
  };

  /**
   * Update User Handler
   * 
   * Updates user state (e.g., profile changes)
   * 
   * @param {object} userData - Updated user data
   */
  const updateUser = (userData) => {
    setUser((prev) => ({
      ...prev,
      ...userData,
    }));
  };

  // Context value
  const value = {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    logout,
    register,
    updateUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Usage Example:
 * 
 * import { AuthContext } from './context/AuthContext';
 * 
 * function MyComponent() {
 *   const { user, isAuthenticated, login, logout } = useContext(AuthContext);
 *   
 *   if (!isAuthenticated) {
 *     return <p>Not logged in</p>;
 *   }
 *   
 *   return (
 *     <div>
 *       <p>Welcome {user.name}</p>
 *       <button onClick={logout}>Logout</button>
 *     </div>
 *   );
 * }
 * 
 * Or use the useAuth hook (see useAuth.js)
 */
