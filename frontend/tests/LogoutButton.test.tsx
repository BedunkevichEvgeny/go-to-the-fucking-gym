import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { LogoutButton } from '../src/components/LogoutButton';

vi.mock('../src/services/authApi', () => ({
  logout: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

import * as authApi from '../src/services/authApi';

function renderLogoutButton() {
  return render(
    <MemoryRouter>
      <LogoutButton />
    </MemoryRouter>,
  );
}

describe('LogoutButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders a button', () => {
    renderLogoutButton();
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
  });

  it('calls authApi.logout on click', async () => {
    vi.mocked(authApi.logout).mockResolvedValueOnce(undefined);
    renderLogoutButton();

    fireEvent.click(screen.getByRole('button', { name: /sign out/i }));

    await waitFor(() => {
      expect(authApi.logout).toHaveBeenCalledOnce();
    });
  });

  it('navigates to /login after logout', async () => {
    vi.mocked(authApi.logout).mockResolvedValueOnce(undefined);
    renderLogoutButton();

    fireEvent.click(screen.getByRole('button', { name: /sign out/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });
});

