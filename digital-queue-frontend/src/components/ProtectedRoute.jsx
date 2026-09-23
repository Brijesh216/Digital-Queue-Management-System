// frontend/src/components/ProtectedRoute.jsx
// Protected Route Component
// Wraps routes to ensure only authenticated users can access them

import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * ProtectedRoute Component
 * 
 * Checks if user is authenticated before rendering a route
 * Redirects to login if not authenticated
 * 
 * Usage:
 * <Routes>
 *   <Route path="/login" element={<Login />} />
 *   <Route 
 *     path="/dashboard" 
 *     element={<ProtectedRoute><Dashboard /></ProtectedRoute>} 
 *   />
 *   <Route 
 *     path="/admin" 
 *     element={<ProtectedRoute requiredRole="ADMIN"><Admin /></ProtectedRoute>} 
 *   />
 * </Routes>
 * 
 * Props:
 * - children: Component to render if authenticated
 * - requiredRole: (optional) Role required to access (e.g., "ADMIN", "USER")
 * 
 * Flow:
 * 1. Get auth state from context
 * 2. If loading, show loading spinner
 * 3. If not authenticated, redirect to login
 * 4. If authenticated but role doesn't match, redirect to unauthorized
 * 5. If authenticated and role matches, render component
 * 
 * @param {ReactNode} children - Component to render if authenticated
 * @param {string} requiredRole - (optional) Required user role
 * @returns {ReactElement}
 */
export function ProtectedRoute({ children, requiredRole = null, allowedRoles = null }) {
  const { isAuthenticated, loading, user } = useAuth();

  // Show loading spinner while checking authentication
  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <div className="animate-spin h-12 w-12 border-4 border-blue-600 border-t-transparent rounded-full mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    console.log('❌ [ProtectedRoute] User not authenticated, redirecting to /login');
    return <Navigate to="/login" replace />;
  }

  // Check if user has required role
  const roleAllowed = allowedRoles
    ? allowedRoles.includes(user?.role)
    : !requiredRole || user?.role === requiredRole;

  if (!roleAllowed) {
    console.log(`❌ [ProtectedRoute] User role "${user?.role}" does not match required role "${requiredRole}"`);
    return <Navigate to="/dashboard" replace />;
  }

  // User is authenticated and has required role
  console.log('✅ [ProtectedRoute] User authenticated, rendering component');
  return children;
}

/**
 * AdminRoute Component
 * 
 * Convenience wrapper for admin-only routes
 * Automatically requires ADMIN role
 * 
 * Usage:
 * <Route 
 *   path="/admin/dashboard" 
 *   element={<AdminRoute><AdminDashboard /></AdminRoute>} 
 * />
 * 
 * @param {ReactNode} children - Component to render if user is admin
 * @returns {ReactElement}
 */
export function AdminRoute({ children }) {
  return <ProtectedRoute allowedRoles={["ADMIN", "OPERATOR"]}>{children}</ProtectedRoute>;
}

/**
 * UserRoute Component
 * 
 * Convenience wrapper for user-only routes
 * Automatically requires USER role
 * 
 * Usage:
 * <Route 
 *   path="/user/dashboard" 
 *   element={<UserRoute><UserDashboard /></UserRoute>} 
 * />
 * 
 * @param {ReactNode} children - Component to render if user has USER role
 * @returns {ReactElement}
 */
export function UserRoute({ children }) {
  return <ProtectedRoute requiredRole="USER">{children}</ProtectedRoute>;
}

/**
 * Usage Examples:
 * 
 * // Basic protected route
 * <Route 
 *   path="/dashboard" 
 *   element={
 *     <ProtectedRoute>
 *       <Dashboard />
 *     </ProtectedRoute>
 *   } 
 * />
 * 
 * // Admin-only route
 * <Route 
 *   path="/admin" 
 *   element={
 *     <AdminRoute>
 *       <AdminPanel />
 *     </AdminRoute>
 *   } 
 * />
 * 
 * // User-only route
 * <Route 
 *   path="/user/profile" 
 *   element={
 *     <UserRoute>
 *       <UserProfile />
 *     </UserRoute>
 *   } 
 * />
 * 
 * // Role-based protection
 * <Route 
 *   path="/special" 
 *   element={
 *     <ProtectedRoute requiredRole="STAFF">
 *       <SpecialPage />
 *     </ProtectedRoute>
 *   } 
 * />
 */
