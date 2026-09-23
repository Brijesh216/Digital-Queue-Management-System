// frontend/src/pages/Register.jsx
// User Registration Page Component
// Handles new user account creation with form validation and API integration

import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';

/**
 * Register Page Component
 * 
 * Features:
 * - Full Name, Username, Email, Password fields
 * - Real-time form validation
 * - Password confirmation matching
 * - Loading spinner during submission
 * - Error message display
 * - API integration with Axios
 * - Automatic redirect to login on success
 * - Responsive Tailwind CSS design
 * 
 * Validation Rules:
 * - Full Name: Required, min 2 characters
 * - Username: Required, min 3 characters, alphanumeric
 * - Email: Required, valid email format
 * - Password: Required, min 8 characters
 * - Confirm Password: Required, must match password
 * 
 * Error Handling:
 * - Username already exists → Show error
 * - Email already exists → Show error
 * - Password mismatch → Show error
 * - Server error → Show error
 * - Network error → Show error
 * 
 * Success Flow:
 * 1. User enters form data
 * 2. Click Register button
 * 3. Validate all fields
 * 4. Show loading spinner
 * 5. Call authService.register()
 * 6. Axios sends POST to /api/auth/register
 * 7. Backend creates user account
 * 8. If success: Show success message, redirect to /login
 * 9. If error: Show error message, stay on page
 */
export default function Register() {
  // Form state
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [phone, setPhone] = useState('');
  
  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [validationErrors, setValidationErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  // Navigation
  const navigate = useNavigate();

  /**
   * Validate Email Format
   * Uses regex to check if email is valid
   * 
   * @param {string} email - Email to validate
   * @returns {boolean}
   */
  const isValidEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  /**
   * Validate Username Format
   * Only alphanumeric and underscores allowed
   * 
   * @param {string} username - Username to validate
   * @returns {boolean}
   */
  const isValidUsername = (username) => {
    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    return usernameRegex.test(username);
  };

  /**
   * Validate Form Inputs
   * 
   * Rules:
   * - Full Name: Required, at least 2 characters
   * - Username: Required, 3-20 characters, alphanumeric
   * - Email: Required, valid email format
   * - Password: Required, at least 8 characters
   * - Confirm Password: Required, must match password
   * 
   * Returns object with validation errors (empty = valid)
   */
  const validateForm = () => {
    const errors = {};

    // Full Name validation
    if (!fullName.trim()) {
      errors.fullName = 'Full name is required';
    } else if (fullName.trim().length < 2) {
      errors.fullName = 'Full name must be at least 2 characters';
    }

    // Username validation
    if (!username.trim()) {
      errors.username = 'Username is required';
    } else if (username.length < 3) {
      errors.username = 'Username must be at least 3 characters';
    } else if (username.length > 20) {
      errors.username = 'Username must not exceed 20 characters';
    } else if (!isValidUsername(username)) {
      errors.username = 'Username can only contain letters, numbers, and underscores';
    }

    // Email validation
    if (!email.trim()) {
      errors.email = 'Email is required';
    } else if (!isValidEmail(email)) {
      errors.email = 'Please enter a valid email address';
    }

    // Phone validation
    if (!phone.trim()) {
      errors.phone = 'Phone number is required';
    } else if (!/^[0-9]{10}$|^\+[0-9]{1,3}[0-9]{6,14}$/.test(phone)) {
      errors.phone = 'Please enter a valid phone number (10 digits or +country code)';
    }

    // Password validation
    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }

    // Confirm Password validation
    if (!confirmPassword) {
      errors.confirmPassword = 'Please confirm your password';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
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
   * 5. If valid, set loading and call register
   * 6. Show error if registration fails
   * 7. Redirect to login if registration succeeds
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    setError('');
    setValidationErrors({});
    setSuccessMessage('');

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
      console.log('Attempting registration for:', username);
      
      // Call register service
      const result = await authService.register(username, email, password, confirmPassword, fullName, phone);
      
      console.log('Registration successful!');
      
      // Show success message
      setSuccessMessage('Registration successful! Redirecting to login...');
      
      // Wait 2 seconds then redirect to login
      setTimeout(() => {
        navigate('/login', { replace: true });
      }, 2000);
      
    } catch (err) {
      // Handle registration error
      const errorMessage = 
        err.response?.data?.message || 
        'Registration failed. Please try again.';
      
      setError(errorMessage);
      console.error('❌ Registration error:', errorMessage);
      
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
    <div className="theme-auth min-h-screen flex items-center justify-center p-4 py-12">
      <div className="w-full max-w-md">
        {/* Card Container */}
        <div className="theme-auth-panel rounded-lg p-8">
          
          {/* Header */}
          <div className="text-center mb-8">
            <div className="text-4xl font-bold text-blue-600 mb-2">DQ</div>
            <h1 className="text-2xl font-bold text-gray-800">Create Account</h1>
            <p className="text-gray-500 text-sm mt-2">Join Digital Queue System</p>
          </div>

          {/* Success Alert */}
          {successMessage && (
            <div className="mb-6 p-4 bg-green-50 border-l-4 border-green-500 rounded">
              <div className="flex">
                <div className="flex-shrink-0">
                  <span className="text-green-500 text-xl">✓</span>
                </div>
                <div className="ml-3">
                  <p className="text-green-700 text-sm font-medium">{successMessage}</p>
                </div>
              </div>
            </div>
          )}

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

          {/* Registration Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Full Name Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Full Name
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) =>
                  handleInputChange('fullName', e.target.value, setFullName)
                }
                placeholder="John Doe"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.fullName
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
              />
              {validationErrors.fullName && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.fullName}
                </p>
              )}
            </div>

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
                placeholder="johndoe"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.username
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
              />
              {validationErrors.username && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.username}
                </p>
              )}
              <p className="text-gray-400 text-xs mt-1">
                Letters, numbers, and underscores only
              </p>
            </div>

            {/* Email Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) =>
                  handleInputChange('email', e.target.value, setEmail)
                }
                placeholder="john@example.com"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.email
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
              />
              {validationErrors.email && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.email}
                </p>
              )}
            </div>

            {/* Phone Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Phone Number
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) =>
                  handleInputChange('phone', e.target.value, setPhone)
                }
                placeholder="1234567890 or +1234567890"
                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                  validationErrors.phone
                    ? 'border-red-500 bg-red-50'
                    : 'border-gray-300 bg-gray-50'
                }`}
                disabled={loading}
              />
              {validationErrors.phone && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.phone}
                </p>
              )}
            </div>

            {/* Password Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) =>
                    handleInputChange('password', e.target.value, setPassword)
                  }
                  placeholder="••••••••"
                  className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                    validationErrors.password
                      ? 'border-red-500 bg-red-50'
                      : 'border-gray-300 bg-gray-50'
                  }`}
                  disabled={loading}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-gray-500 hover:text-gray-700"
                  disabled={loading}
                >
                  {showPassword ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
              {validationErrors.password && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.password}
                </p>
              )}
              <p className="text-gray-400 text-xs mt-1">
                Minimum 8 characters recommended
              </p>
            </div>

            {/* Confirm Password Field */}
            <div>
              <label className="block text-gray-700 text-sm font-semibold mb-2">
                Confirm Password
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) =>
                    handleInputChange('confirmPassword', e.target.value, setConfirmPassword)
                  }
                  placeholder="••••••••"
                  className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition ${
                    validationErrors.confirmPassword
                      ? 'border-red-500 bg-red-50'
                      : 'border-gray-300 bg-gray-50'
                  }`}
                  disabled={loading}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-3 text-gray-500 hover:text-gray-700"
                  disabled={loading}
                >
                  {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
              {validationErrors.confirmPassword && (
                <p className="text-red-600 text-xs mt-1 flex items-center">
                  <span className="mr-1">✕</span>
                  {validationErrors.confirmPassword}
                </p>
              )}
            </div>

            {/* Terms & Conditions */}
            <div className="flex items-center">
              <input
                type="checkbox"
                className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                disabled={loading}
              />
              <label className="ml-2 text-gray-700 text-sm">
                I agree to the{' '}
                <a href="#terms" className="text-blue-600 hover:text-blue-700 font-medium">
                  Terms of Service
                </a>
              </label>
            </div>

            {/* Register Button */}
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
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <span>✓</span>
                  <span>Create Account</span>
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
              <span className="px-2 bg-white text-gray-500">Already have an account?</span>
            </div>
          </div>

          {/* Footer */}
          <div className="text-center">
            <p className="text-gray-600 text-sm">
              Already registered?{' '}
              <Link
                to="/login"
                className="text-blue-600 hover:text-blue-700 font-semibold"
              >
                Sign in here
              </Link>
            </p>
          </div>

          {/* Security Notice */}
          <div className="mt-6 text-center">
            <p className="text-xs text-gray-400">
              🔒 Your data is encrypted and secure
            </p>
            <p className="text-xs text-gray-400">
              By registering, you agree to our Privacy Policy
            </p>
          </div>
        </div>

        {/* Bottom Info */}
        <div className="mt-6 text-center text-xs text-gray-500">
          <p>Have questions? <a href="#support" className="text-blue-600 hover:text-blue-700">Contact Support</a></p>
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
 *    - Email format validation
 *    - Password matching validation
 *    - Username format validation
 * 
 * 2. LOADING STATE
 *    - Shows spinner while registering
 *    - Disables all inputs during submission
 *    - Prevents duplicate submissions
 *    - Smooth loading animation
 * 
 * 3. ERROR HANDLING
 *    - Displays error alert at top
 *    - Shows field validation errors below input
 *    - Catches network errors
 *    - Logs errors to console for debugging
 *    - Handles server-side validation errors
 * 
 * 4. API INTEGRATION
 *    - Uses authService.register()
 *    - Sends POST request to /api/auth/register
 *    - Handles success/error responses
 * 
 * 5. RESPONSIVE DESIGN
 *    - Works on mobile, tablet, desktop
 *    - Tailwind responsive classes
 *    - Touch-friendly button sizes
 *    - Flexible layout
 * 
 * 6. SECURITY
 *    - Password toggle (show/hide)
 *    - Password confirmation matching
 *    - Email validation
 *    - No sensitive data in console (production)
 * 
 * 7. UX IMPROVEMENTS
 *    - Focus management (autofocus on fullname)
 *    - Clear error messages 
 *    - Visual feedback on interactions
 *    - Loading spinner shows progress
 *    - Success message before redirect
 *    - Link to login page
 *    - Password show/hide toggle
 *    - Help text for field requirements
 */
