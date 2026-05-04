import { useEffect, useState } from 'react';
import { getMe } from '../services/authApi';

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

export interface AuthState {
  status: AuthStatus;
  username: string | null;
}

/**
 * Hook that calls GET /api/auth/me on mount to determine auth state.
 * - 200 → authenticated
 * - 401 → unauthenticated
 */
export function useAuth(): AuthState {
  const [state, setState] = useState<AuthState>({
    status: 'loading',
    username: null,
  });

  useEffect(() => {
    getMe()
      .then((data) => {
        setState({ status: 'authenticated', username: data.username });
      })
      .catch(() => {
        setState({ status: 'unauthenticated', username: null });
      });
  }, []);

  return state;
}

