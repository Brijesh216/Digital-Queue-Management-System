// frontend/src/hooks/useAuth.js
// Custom hook to access AuthContext
// Makes consuming auth state easier throughout the app

import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

/**
 * useAuth Hook
 * 
 * Provides easy access to authentication state and methods
 * Must be used inside AuthProvider
 * 
 * Returns:
 * {
 *   user: object|null,           // Current user data
 *   isAuthenticated: boolean,    // Is user logged in
 *   loading: boolean,            // Is auth loading
 *   error: string,               // Error message if any
 *   login: function,             // Login method
 *   logout: function,            // Logout method
 *   register: function,          // Register method
 *   updateUser: function         // Update user method
 * }
 * 
 * Usage:
 * const { user, isAuthenticated, login, logout } = useAuth();
 * 
 * @returns {object} Auth context value
 * @throws {Error} If used outside AuthProvider
 */
export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      '❌ useAuth must be used within an AuthProvider. ' +
      'Make sure your component is wrapped with <AuthProvider>'
    );
  }

  return context;
}

/**
 * Example Usage:
 * 
 * import { useAuth } from './hooks/useAuth';
 * 
 * function Dashboard() {
 *   const { user, logout } = useAuth();
 *   
 *   return (
 *     <div>
 *       <h1>Welcome {user.name}</h1>
 *       <button onClick={logout}>Logout</button>
 *     </div>
 *   );
 * }
 * 
 * function ProtectedRoute() {
 *   const { isAuthenticated, loading } = useAuth();
 *   
 *   if (loading) return <p>Loading...</p>;
 *   if (!isAuthenticated) return <Navigate to="/login" />;
 *   
 *   return <Dashboard />;
 * }
 * 
 * function LoginForm() {
 *   const { login, error } = useAuth();
 *   
 *   const handleSubmit = async (e) => {
 *     e.preventDefault();
 *     try {
 *       await login(username, password);
 *     } catch (err) {
 *       console.error('Login failed:', error);
 *     }
 *   };
 *   
 *   return (
 *     <form onSubmit={handleSubmit}>
 *       {error && <p className="text-red-600">{error}</p>}
 *       <input type="text" placeholder="Username" />
 *       <input type="password" placeholder="Password" />
 *       <button type="submit">Login</button>
 *     </form>
 *   );
 * }
 */
