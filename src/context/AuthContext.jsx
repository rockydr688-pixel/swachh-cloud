import React, { createContext, useContext, useState, useEffect } from 'react';
import { mockUserSettings } from '../data/mockData';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check localStorage for persisted session
    const storedUser = localStorage.getItem('cloudclean_user');
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error('Failed to parse user session', e);
      }
    } else {
      // Default initial mock logged-in state for smooth testing of SaaS dashboard
      const defaultUser = {
        name: mockUserSettings.profile.fullName,
        email: mockUserSettings.profile.email,
        role: mockUserSettings.profile.role,
        avatar: mockUserSettings.profile.avatar,
        token: 'mock-jwt-token-cloudclean-2026',
      };
      setUser(defaultUser);
      localStorage.setItem('cloudclean_user', JSON.stringify(defaultUser));
      localStorage.setItem('cloudclean_token', defaultUser.token);
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    // Mock authentication with artificial delay
    await new Promise((res) => setTimeout(res, 600));
    const loggedUser = {
      name: email.split('@')[0].replace('.', ' '),
      email,
      role: 'Storage Administrator',
      avatar: mockUserSettings.profile.avatar,
      token: `jwt-token-${Date.now()}`,
    };
    setUser(loggedUser);
    localStorage.setItem('cloudclean_user', JSON.stringify(loggedUser));
    localStorage.setItem('cloudclean_token', loggedUser.token);
    return loggedUser;
  };

  const register = async (name, email, password) => {
    await new Promise((res) => setTimeout(res, 600));
    const newUser = {
      name,
      email,
      role: 'Storage Administrator',
      avatar: mockUserSettings.profile.avatar,
      token: `jwt-token-${Date.now()}`,
    };
    setUser(newUser);
    localStorage.setItem('cloudclean_user', JSON.stringify(newUser));
    localStorage.setItem('cloudclean_token', newUser.token);
    return newUser;
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

