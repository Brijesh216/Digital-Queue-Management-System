// frontend/src/pages/Login.jsx
// Login Page Component
// Handles user authentication with form validation and API integration

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Login Page Component
 * 
 * Features:
 * - Username/password form with validation
 * - Loading spinner while authenticating
 * - Error message display
 * - API integration with Axios
 * - JWT token storage
 * - Automatic redirect to dashboard after login
 * - Responsive Tailwind CSS design
 * - Input error states
 * 
 * Form Validation:
 * - Username: Required, min 3 characters
 * - Password: Required, min 6 characters
 * 
 * Error Handling:
 * - Invalid credentials → Show error message
 * - Network error → Show error message
 * - Server error → Show error message
 * 
 * Success Flow:
 * 1. User enters credentials
 * 2. Click Login button
 * 3. Validate form inputs
 * 4. Show loading spinner
 * 5. Call authService.login()
 * 6. Axios sends POST to /auth/login
 * 7. Backend validates credentials
 * 8. If success: Token + user returned
 * 9. Save token to localStorage
 * 10. Save user to localStorage
 * 11. Redirect to /dashboard
 */
export default function Login() {
  // Form state
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  
  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [validationErrors, setValidationErrors] = useState({});

  // Navigation
  const navigate = useNavigate();
  const { login } = useAuth();

  /**
   * Validate Form Inputs
   * 
   * Rules:
   * - Username: Required, at least 3 characters
   * - Password: Required, at least 6 characters
   * 
   * Returns object with validation errors (empty = valid)
   */
  const validateForm = () => {
    const errors = {};

    if (!username.trim()) {
      errors.username = 'Username is required';
    } else if (username.length < 3) {
      errors.username = 'Username must be at least 3 characters';
    }

    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }

    return errors;
  };

  /**
   * Handle Form Submission
   * 
   * Flow:
   * 1. Prevent default form submission
   * 2. Clear previous errors
   * 3. Validate inputs
   * 4. If invalid, show validation errors
   * 5. If valid, set loading and call login
   * 6. Show error if login fails
   * 7. Redirect to dashboard if login succeeds
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    setError('');
    setValidationErrors({});

    // Validate form
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      console.log('❌ Form validation failed:', errors);
      return;
    }

    // Show loading state
    setLoading(true);

    try {
      console.log('Attempting login...');
      
      // Call login service
      const { user } = await login(username, password);
      
      console.log(' Login successful!');
      console.log(' Stored token and user data');
      const destination = ['ADMIN', 'OPERATOR'].includes(user?.role)
        ? '/admin/dashboard'
        : '/dashboard';

      console.log(` Redirecting to ${destination}...`);
      
      navigate(destination, { replace: true });
      
    } catch (err) {
      // Handle login error
      const errorMessage = 
        err.response?.data?.message || 
        'Login failed. Please check your credentials.';
      
      setError(errorMessage);
      console.error(' Login error:', errorMessage);
      
    } finally {
      // Stop loading
      setLoading(false);
    }
  };

  /**
   * Handle Input Change
   * Clear field-specific validation error when user types
   */
  const handleInputChange = (field, value, setter) => {
    setter(value);
    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors({
        ...validationErrors,
        [field]: '',
      });
    }
  };

  return (
    <div className="theme-auth min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Card Container */}
        <div className="theme-auth-panel rounded-lg p-8">
          
          {/* Header */}
          <div className="text-center mb-8">
            <div className="text-4xl font-bold text-blue-600 mb-2">DQ</div>
            <h1 className="text-2xl font-bold text-gray-800">Digital Queue</h1>
            <p className="text-gray-500 text-sm mt-2">Queue Management System</p>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 rounded">
              <div className="flex">
                <div className="flex-shrink-0">
                  <span className="text-red-500 text-xl">⚠️</span>
                </div>
                <div className="ml-3">
                  <p className="text-red-700 text-sm font-medium">{error}</p>
                </div>
              </div>
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            
            {/* Username Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) =>
                  handleInputChange('username', e.target.value, setUsername)
                }
                placeholder="Enter your username"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.username
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
                autoFocus
              />
              {validationErrors.username && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.username}
                </p>
              )}
            </div>

            {/* Password Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) =>
                  handleInputChange('password', e.target.value, setPassword)
                }
                placeholder="Enter your password"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.password
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
              />
              {validationErrors.password && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.password}
                </p>
              )}
            </div>

            {/* Remember Me & Forgot Password */}
            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                  disabled={loading}
                />
                <span className="ml-2">Remember me</span>
              </label>
              <a
                href="#forgot-password"
                className="text-blue-600 hover:text-blue-700 font-medium"
              >
                Forgot Password?
              </a>
            </div>

            {/* Login Button */}
            <button
              type="submit"
              disabled={loading}
              className={`w-full py-3 px-4 rounded-lg font-semibold text-white transition duration-200 flex items-center justify-center gap-2 ${
                loading
                  ? 'bg-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800'
              }`}
            >
              {loading ? (
                <>
                  {/* Loading Spinner */}
                  <div className="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></div>
                  <span>Logging in...</span>
                </>
              ) : (
                <>
                  <span></span>
                  <span>Login</span>
                </>
              )}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white text-gray-500">or</span>
            </div>
          </div>

          {/* Additional Options */}
          <div className="space-y-3">
            {/* Guest Login (optional) */}
            <button
              type="button"
              className="w-full py-2 px-4 border-2 border-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-50 transition"
              disabled={loading}
            >
              Continue as Guest
            </button>
          </div>

          {/* Footer */}
          <div className="mt-8 text-center border-t pt-6">
            <p className="text-gray-600 text-sm">
              Don't have an account?{' '}
              <a
                href="/register"
                className="text-blue-600 hover:text-blue-700 font-semibold"
              >
                Sign up here
              </a>
            </p>
          </div>

        </div>

        {/* Security Notice */}
        <div className="mt-6 text-center text-xs text-gray-500">
          <p>🔒 Your data is encrypted and secure</p>
          <p>By logging in, you agree to our Terms of Service</p>
        </div>
      </div>
    </div>
  );
}

/**
 * Component Features Explained:
 * 
 * 1. FORM VALIDATION
 *    - Validates on submit, not on every keystroke
 *    - Shows field-specific error messages
 *    - Clears errors when user starts typing
 *    - Prevents form submission if invalid
 * 
 * 2. LOADING STATE
 *    - Shows spinner while authenticating
 *    - Disables all inputs during submission
 *    - Prevents duplicate submissions
 *    - Smooth loading animation
 * 
 * 3. ERROR HANDLING
 *    - Displays error alert at top
 *    - Shows field validation errors below input
 *    - Catches network errors
 *    - Logs errors to console for debugging
 * 
 * 4. API INTEGRATION
 *    - Uses authService.login()
 *    - Sends POST request to /auth/login
 *    - Automatically stores JWT in localStorage
 *    - Axios interceptor attaches token to future requests
 * 
 * 5. RESPONSIVE DESIGN
 *    - Works on mobile, tablet, desktop
 *    - Tailwind responsive classes
 *    - Touch-friendly button sizes
 *    - Flexible layout
 * 
 * 6. SECURITY
 *    - Password input type (masked)
 *    - No sensitive data in console (production)
 *    - Token stored securely in localStorage
 *    - JWT attached automatically by Axios
 * 
 * 7. UX IMPROVEMENTS
 *    - Focus management (autofocus on username)
 *    - Clear error messages
 *    - Visual feedback on interactions
 *    - Loading spinner shows progress
 *    - Disabled state prevents resubmission
 */
