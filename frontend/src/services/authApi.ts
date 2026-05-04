const BASE = '';

/**
 * Login with username and password via JSON POST to /api/auth/login.
 * Uses credentials: 'include' so JSESSIONID cookie is set by the browser.
 */
export async function login(username: string, password: string): Promise<void> {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    throw new Error('Invalid credentials');
  }
}

/**
 * Fetch the currently authenticated user's username.
 * Returns 401 when unauthenticated — used by useAuth as the auth-check signal.
 */
export async function getMe(): Promise<{ username: string }> {
  const res = await fetch(`${BASE}/api/auth/me`, {
    credentials: 'include',
  });
  if (!res.ok) {
    throw new Error('Unauthenticated');
  }
  return res.json();
}

/**
 * Logout the current session via POST /api/auth/logout.
 */
export async function logout(): Promise<void> {
  await fetch(`${BASE}/api/auth/logout`, {
    method: 'POST',
    credentials: 'include',
  });
}

