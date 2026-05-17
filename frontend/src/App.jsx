import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { useTheme } from './hooks/useTheme';
import LandingPage from './pages/LandingPage';
import TriagePage from './pages/TriagePage';
import AssessmentPage from './pages/AssessmentPage';
import AdminPage from './pages/admin/AdminPage';
import ErrorBoundary from './components/ErrorBoundary';

function ProtectedRoute({ children }) {
  const token = useAuthStore((s) => s.accessToken);
  if (!token) return <Navigate to="/" replace />;
  return children;
}

function AdminRoute({ children }) {
  const token = useAuthStore((s) => s.accessToken);
  const role  = useAuthStore((s) => s.role);
  if (!token)           return <Navigate to="/" replace />;
  if (role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

// Mounts at the root to keep the <html> class in sync with localStorage
function ThemeApplier() {
  useTheme(); // side-effect only — applies class to document.documentElement
  return null;
}

export default function App() {
  return (
    <ErrorBoundary>
      <ThemeApplier />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route
            path="/triage"
            element={
              <ProtectedRoute>
                <TriagePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/assess"
            element={
              <ProtectedRoute>
                <AssessmentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminPage />
              </AdminRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
