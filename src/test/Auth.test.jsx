import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthPage } from '../pages/AuthPage';
import { AuthProvider } from '../context/AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('Frontend Authentication Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders login form with email and password fields', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <AuthPage />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('CloudClean AI')).toBeDefined();
    expect(screen.getByText('Sign In to Dashboard')).toBeDefined();
    expect(screen.getByPlaceholderText('name@company.com')).toBeDefined();
    expect(screen.getByText(/Demo Auto-Login/i)).toBeDefined();
  });

  it('switches between Login and Registration tabs', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <AuthPage />
        </AuthProvider>
      </MemoryRouter>
    );

    // Click "Create Account" tab
    const createAccountTab = screen.getByRole('button', { name: 'Create Account' });
    fireEvent.click(createAccountTab);

    // Full Name field should now be visible
    expect(screen.getByPlaceholderText('Jane Doe')).toBeDefined();

    // Click back to "Sign In" tab
    const signInTab = screen.getByRole('button', { name: 'Sign In' });
    fireEvent.click(signInTab);
    expect(screen.queryByPlaceholderText('Jane Doe')).toBeNull();
  });

  it('handles quick demo login and navigates to dashboard', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <AuthPage />
        </AuthProvider>
      </MemoryRouter>
    );

    const demoBtn = screen.getByText(/Demo Auto-Login/i);
    fireEvent.click(demoBtn);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });
});
