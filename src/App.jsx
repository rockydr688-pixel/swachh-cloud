import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { StorageProvider } from './context/StorageContext';
import { ToastProvider } from './context/ToastContext';
import AppLayout from './components/layout/AppLayout';

// Pages
import Dashboard from './pages/Dashboard';
import StorageConnections from './pages/StorageConnections';
import FileScanner from './pages/FileScanner';
import AllFiles from './pages/AllFiles';
import DuplicateFiles from './pages/DuplicateFiles';
import BlurryImages from './pages/BlurryImages';
import LargeFiles from './pages/LargeFiles';
import InactiveFiles from './pages/InactiveFiles';
import CleanupRecommendations from './pages/CleanupRecommendations';
import CleanupHistory from './pages/CleanupHistory';
import ReportsAnalytics from './pages/ReportsAnalytics';
import UserSettings from './pages/UserSettings';
import AuthPage from './pages/AuthPage';

// Protected Route Guard
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
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
                <Route path="dashboard" element={<Dashboard />} />
                <Route path="connections" element={<StorageConnections />} />
                <Route path="scanner" element={<FileScanner />} />
                <Route path="files" element={<AllFiles />} />
                <Route path="files/duplicates" element={<DuplicateFiles />} />
                <Route path="files/blurry" element={<BlurryImages />} />
                <Route path="files/large" element={<LargeFiles />} />
                <Route path="files/inactive" element={<InactiveFiles />} />
                <Route path="recommendations" element={<CleanupRecommendations />} />
                <Route path="history" element={<CleanupHistory />} />
                <Route path="reports" element={<ReportsAnalytics />} />
                <Route path="settings" element={<UserSettings />} />
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
