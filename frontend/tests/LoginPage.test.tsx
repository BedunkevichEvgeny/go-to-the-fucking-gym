import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../src/pages/LoginPage';

// Mock authApi
vi.mock('../src/services/authApi', () => ({
  login: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

import * as authApi from '../src/services/authApi';

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders username field, password field, and submit button', () => {
    renderLoginPage();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('password field has type="password"', () => {
    renderLoginPage();
    expect(screen.getByLabelText(/password/i)).toHaveAttribute('type', 'password');
  });

  it('calls authApi.login and navigates to / on valid submission', async () => {
    vi.mocked(authApi.login).mockResolvedValueOnce(undefined);
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'admin' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith('admin', 'admin');
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('shows "Invalid credentials" error on 401', async () => {
    vi.mocked(authApi.login).mockRejectedValueOnce(new Error('Invalid credentials'));
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'wrong' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid credentials');
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('shows "Username is required" when username is empty', async () => {
    renderLoginPage();
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'admin' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Username is required')).toBeInTheDocument();
    });
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('shows "Password is required" when password is empty', async () => {
    renderLoginPage();
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'admin' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Password is required')).toBeInTheDocument();
    });
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('treats whitespace-only username as empty', async () => {
    renderLoginPage();
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: '   ' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'admin' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Username is required')).toBeInTheDocument();
    });
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('treats whitespace-only password as empty', async () => {
    renderLoginPage();
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: '   ' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Password is required')).toBeInTheDocument();
    });
    expect(authApi.login).not.toHaveBeenCalled();
  });
});

