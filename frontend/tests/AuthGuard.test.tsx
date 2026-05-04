import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthGuard } from '../src/components/AuthGuard';

vi.mock('../src/hooks/useAuth');
vi.mock('../src/services/authApi', () => ({
  login: vi.fn(),
  getMe: vi.fn(),
  logout: vi.fn(),
}));

import * as useAuthModule from '../src/hooks/useAuth';

function renderGuard(children = <div>Protected Content</div>) {
  return render(
    <MemoryRouter>
      <AuthGuard>{children}</AuthGuard>
    </MemoryRouter>,
  );
}

describe('AuthGuard', () => {
  it('shows loading spinner when status is loading', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue({
      status: 'loading',
      username: null,
    });
    renderGuard();
    expect(screen.getByLabelText('Loading')).toBeInTheDocument();
  });

  it('renders LoginPage when unauthenticated', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue({
      status: 'unauthenticated',
      username: null,
    });
    renderGuard();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('renders children when authenticated', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue({
      status: 'authenticated',
      username: 'admin',
    });
    renderGuard();
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });
});

