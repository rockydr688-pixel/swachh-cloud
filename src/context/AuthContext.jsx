import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient from '../services/api';
import { mockUserSettings } from '../data/mockData';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isLiveBackend, setIsLiveBackend] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('cloudclean_token');
      const storedUser = localStorage.getItem('cloudclean_user');

      if (storedToken) {
        try {
          // Attempt verifying session with actual Spring Boot backend
          const response = await apiClient.get('/auth/me');
          if (response.data?.success && response.data?.data) {
            setUser(response.data.data);
            setIsLiveBackend(true);
            localStorage.setItem('cloudclean_user', JSON.stringify(response.data.data));
            setLoading(false);
            return;
          }
        } catch (err) {
          // Fallback to local session if backend is temporarily offline
          if (storedUser) {
            try {
              setUser(JSON.parse(storedUser));
            } catch (e) {
              localStorage.removeItem('cloudclean_user');
            }
          }
        }
      }

      // Default mock admin user if no prior session exists
      if (!user) {
        const defaultUser = {
          id: 'admin-001',
          name: mockUserSettings.profile.fullName,
          email: mockUserSettings.profile.email,
          role: mockUserSettings.profile.role,
          avatar: mockUserSettings.profile.avatar,
          token: 'demo-jwt-token-cloudclean',
        };
        setUser(defaultUser);
        localStorage.setItem('cloudclean_user', JSON.stringify(defaultUser));
        localStorage.setItem('cloudclean_token', defaultUser.token);
      }

      setLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email, password) => {
    try {
      // 1. Try real Spring Boot authentication
      const response = await apiClient.post('/auth/login', { email, password });
      if (response.data?.success && response.data?.data) {
        const { token, user: userData } = response.data.data;
        const mappedUser = {
          id: userData.id,
          name: userData.fullName || userData.name || email.split('@')[0],
          email: userData.email,
          role: userData.role || 'ROLE_USER',
          token,
        };
        setUser(mappedUser);
        setIsLiveBackend(true);
        localStorage.setItem('cloudclean_token', token);
        localStorage.setItem('cloudclean_user', JSON.stringify(mappedUser));
        return mappedUser;
      }
    } catch (backendError) {
      // If server returned 401 Unauthorized or 400 Bad Request, rethrow real error
      if (backendError.response?.status === 401 || backendError.response?.status === 400) {
        throw backendError;
      }

      // If backend is unreachable (offline dev mode), perform graceful simulation fallback
      await new Promise((res) => setTimeout(res, 400));
      const simulatedUser = {
        name: email.split('@')[0].replace('.', ' '),
        email,
        role: 'Storage Administrator',
        avatar: mockUserSettings.profile.avatar,
        token: `mock-jwt-token-${Date.now()}`,
      };
      setUser(simulatedUser);
      localStorage.setItem('cloudclean_token', simulatedUser.token);
      localStorage.setItem('cloudclean_user', JSON.stringify(simulatedUser));
      return simulatedUser;
    }
  };

  const register = async (name, email, password, company = 'CloudClean Enterprise') => {
    try {
      // 1. Try real Spring Boot registration
      const response = await apiClient.post('/auth/register', {
        fullName: name,
        email,
        password,
        company,
      });

      if (response.data?.success && response.data?.data) {
        const { token, user: userData } = response.data.data;
        const mappedUser = {
          id: userData.id,
          name: userData.fullName,
          email: userData.email,
          role: userData.role,
          token,
        };
        setUser(mappedUser);
        setIsLiveBackend(true);
        localStorage.setItem('cloudclean_token', token);
        localStorage.setItem('cloudclean_user', JSON.stringify(mappedUser));
        return mappedUser;
      }
    } catch (backendError) {
      if (backendError.response?.status) {
        throw backendError;
      }

      // If backend offline, graceful fallback
      await new Promise((res) => setTimeout(res, 400));
      const simulatedUser = {
        name,
        email,
        role: 'Storage Administrator',
        avatar: mockUserSettings.profile.avatar,
        token: `mock-jwt-token-${Date.now()}`,
      };
      setUser(simulatedUser);
      localStorage.setItem('cloudclean_token', simulatedUser.token);
      localStorage.setItem('cloudclean_user', JSON.stringify(simulatedUser));
      return simulatedUser;
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('cloudclean_user');
    localStorage.removeItem('cloudclean_token');
  };

  const updateProfile = (data) => {
    const updated = { ...user, ...data };
    setUser(updated);
    localStorage.setItem('cloudclean_user', JSON.stringify(updated));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        loading,
        isLiveBackend,
        login,
        register,
        logout,
        updateProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
