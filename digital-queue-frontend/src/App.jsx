
// frontend/src/App.jsx
// Main App Component
// Sets up routing, providers, and layouts

import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute, AdminRoute, UserRoute } from './components/ProtectedRoute';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
// import AdminPanel from './pages/AdminPanel'; // To be created later
// import UserProfile from './pages/UserProfile'; // To be created later

/**
 * App Component
 * 
 * Main application component that:
 * 1. Sets up React Router for navigation
 * 2. Wraps app with AuthProvider for auth context
 * 3. Defines all route configurations
 * 4. Handles redirects for unauthorized access
 * 
 * Route Structure:
 * - /login: Public route, login page
 * - /register: Public route, registration page
 * - /dashboard: Protected, user dashboard
 * - /admin: Protected (ADMIN role), admin panel
 * - /user/profile: Protected (USER role), user profile
 * - /: Redirect to /dashboard or /login
 * 
 * Architecture:
 * 1. Router: Handles URL navigation
 * 2. AuthProvider: Manages global auth state
 * 3. Routes: Define route paths
 * 4. ProtectedRoute: Checks authentication before rendering
 * 5. Components: Render pages based on route
 */
export default function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* PUBLIC ROUTES */}
          {/* These routes are accessible to anyone */}

          {/* Login Page */}
          <Route path="/login" element={<Login />} />

          {/* Register Page */}
          <Route path="/register" element={<Register />} />

          {/* PROTECTED ROUTES */}
          {/* These routes require authentication */}

          {/* Dashboard (accessible to all authenticated users) */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/dashboard"
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            }
          />

          {/* Admin Panel (requires ADMIN role) */}
          {/* 
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminPanel />
                </AdminRoute>
              }
            /> 
          */}

          {/* User Profile (requires USER role) */}
          {/* 
            <Route
              path="/user/profile"
              element={
                <UserRoute>
                  <UserProfile />
                </UserRoute>
              }
            /> 
          */}

          {/* CATCH-ALL ROUTES */}

          {/* Root path redirects to login (dashboard not created yet) */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* 404 Not Found */}
          {/* 
            <Route path="*" element={<NotFound />} /> 
          */}
        </Routes>
      </AuthProvider>
    </Router>
  );
}

/**
 * IMPORTANT: Setup Instructions
 * 
 * 1. CREATE REACT APP (if not already done)
 *    $ npm create vitelatest digital-queue-frontend -- --template react
 *    $ cd digital-queue-frontend
 *    $ npm install
 * 
 * 2. INSTALL DEPENDENCIES
 *    $ npm install axios react-router-dom
 *    $ npm install -D tailwindcss postcss autoprefixer
 *    $ npx tailwindcss init -p
 * 
 * 3. CONFIGURE TAILWIND
 *    Edit tailwind.config.js:
 *    ```

 *    Edit src/index.css:
 *  
 * 
 * 4. COPY FILES TO PROJECT
 *    Copy the following files to your src/ directory:
 * 
 *    src/utils/
 *    - storage.js
 * 
 *    src/services/
 *    - api.js
 *    - authService.js
 * 
 *    src/context/
 *    - AuthContext.jsx
 * 
 *    src/hooks/
 *    - useAuth.js
 * 
 *    src/components/
 *    - ProtectedRoute.jsx
 * 
 *    src/pages/
 *    - Login.jsx
 * 
 * 5. SET UP ENVIRONMENT VARIABLES
 *    Copy .env.example to .env:
 *    $ cp .env.example .env
 * 
 *    Edit .env with your values:
 *    REACT_APP_API_URL=http://localhost:3000/api
 * 
 * 6. UPDATE App.jsx
 *    Replace your src/App.jsx with this file
 * 
 * 7. START DEVELOPMENT SERVER
 *    $ npm run dev
 * 
 *    App will be available at: http://localhost:5173
 * 
 * 8. TEST LOGIN
 *    - Backend must be running on http://localhost:3000
 *    - Go to http://localhost:5173/login
 *    - Enter demo credentials: username=demo, password=demo123
 *    - Click Login
 *    - Token should be stored in localStorage
 *    - Should redirect to /dashboard
 * 
 * TROUBLESHOOTING:
 * 
 * Q: "404 Not Found" when submitting login form
 * A: Make sure backend is running on http://localhost:3000
 *    Check backend logs for errors
 * 
 * Q: "401 Unauthorized" after login
 * A: Token might be invalid or endpoint wrong
 *    Check that POST /auth/login exists on backend
 *    Verify response format: { token, user }
 * 
 * Q: "CORS error" when fetching from backend
 * A: Backend CORS is not configured for http://localhost:5173
 *    Add to CORS filter in Spring:
 *    corsConfiguration.setAllowedOrigins(
 *      Arrays.asList("http://localhost:3000", "http://localhost:5173")
 *    );
 * 
 * Q: Page reloads and login is lost
 * A: Token should persist in localStorage (handled by storage.js)
 *    Check that AuthContext useEffect is verifying token on mount
 *    Browser DevTools → Application → Local Storage should show token
 * 
 * Q: Tailwind CSS not working
 * A: Make sure tailwind.config.js exists
 *    Check that src/index.css imports @tailwind directives
 *    Restart dev server: npm run dev
 * 
 * NEXT STEPS (after login works):
 * 
 * 1. Create Dashboard page
 * 2. Create Register page
 * 3. Create queue display components
 * 4. Integrate WebSocket for real-time updates
 * 5. Create admin panel
 * 6. Add error boundary for error handling
 * 7. Add loading skeleton screens
 * 8. Add toast notifications
 * 9. Implement responsive navigation
 * 10. Add logout functionality
 */
