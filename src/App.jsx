import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { StorageProvider } from './context/StorageContext';
import { ToastProvider } from './context/ToastContext';
import AppLayout from './components/layout/AppLayout';

// 5 Simplified Core Pages
import Dashboard from './pages/Dashboard';
import StorageConnections from './pages/StorageConnections';
import CleanupRecommendations from './pages/CleanupRecommendations';
import CleanupHistory from './pages/CleanupHistory';
import UserSettings from './pages/UserSettings';

// Supporting / Scanner / Report Pages
import FileScanner from './pages/FileScanner';
import ReportsAnalytics from './pages/ReportsAnalytics';
import AuthPage from './pages/AuthPage';

// Protected Route Guard
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />;
  }

  return children;
};

export const App = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <StorageProvider>
            <Routes>
              {/* Public Auth Page */}
              <Route path="/auth" element={<AuthPage />} />

              {/* Application Main Layout Pages */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/dashboard" replace />} />

                {/* 1. Home */}
                <Route path="dashboard" element={<Dashboard />} />

                {/* 2. My Storage */}
                <Route path="storage" element={<StorageConnections />} />
                <Route path="connections" element={<StorageConnections />} />

                {/* 3. Junk Files (Combining Duplicates, Blurry, Large, Old, Temp) */}
                <Route path="junk" element={<CleanupRecommendations />} />
                <Route path="recommendations" element={<CleanupRecommendations />} />
                <Route path="files" element={<CleanupRecommendations />} />
                <Route path="files/*" element={<CleanupRecommendations />} />

                {/* 4. Scan History */}
                <Route path="history" element={<CleanupHistory />} />
                <Route path="scans" element={<CleanupHistory />} />

                {/* 5. Settings */}
                <Route path="settings" element={<UserSettings />} />

                {/* Direct Scanner & Reports support */}
                <Route path="scanner" element={<FileScanner />} />
                <Route path="reports" element={<ReportsAnalytics />} />
              </Route>

              {/* Catch-all redirect */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </StorageProvider>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
