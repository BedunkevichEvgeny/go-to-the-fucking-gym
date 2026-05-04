import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../services/authApi';

/**
 * Login page with username/password form.
 * - Client-side trim validation before calling API
 * - Displays inline error on 401
 * - Redirects to / on success
 */
export function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    username?: string;
    password?: string;
  }>({});

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const errors: { username?: string; password?: string } = {};
    if (!username.trim()) errors.username = 'Username is required';
    if (!password.trim()) errors.password = 'Password is required';

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    try {
      await login(username.trim(), password.trim());
      navigate('/');
    } catch {
      setError('Invalid credentials');
    }
  }

  return (
    <div className="login-page">
      <section className="card stack-sm login-card">
        <p className="eyebrow">Workout Tracker</p>
        <h1>Sign in</h1>
        <form onSubmit={handleSubmit} className="stack-sm" noValidate>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
            />
            {fieldErrors.username && (
              <p className="field-error" role="alert">
                {fieldErrors.username}
              </p>
            )}
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
            {fieldErrors.password && (
              <p className="field-error" role="alert">
                {fieldErrors.password}
              </p>
            )}
          </div>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="button primary">
            Sign in
          </button>
        </form>
      </section>
    </div>
  );
}

