import { useNavigate } from 'react-router-dom';
import { logout } from '../services/authApi';

/**
 * Logout button that calls POST /api/auth/logout and redirects to /login on success.
 */
export function LogoutButton() {
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <button type="button" className="button ghost" onClick={handleLogout}>
      Sign out
    </button>
  );
}

