import { type ReactNode } from 'react';
import { useAuth } from '../hooks/useAuth';
import { LoginPage } from '../pages/LoginPage';

interface AuthGuardProps {
  children: ReactNode;
}

/**
 * Wraps protected routes.
 * - Loading → shows spinner
 * - Unauthenticated → renders LoginPage
 * - Authenticated → renders children
 */
export function AuthGuard({ children }: AuthGuardProps) {
  const { status } = useAuth();

  if (status === 'loading') {
    return (
      <div className="auth-loading" aria-label="Loading">
        <span className="spinner" />
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return <LoginPage />;
  }

  return <>{children}</>;
}

