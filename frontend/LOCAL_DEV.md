# Frontend Local Development

## Requirements
- Node.js 20+
- npm 10+
- Backend running locally or accessible at configured API URL

## Installation & Running

### Install Dependencies
```powershell
Set-Location frontend
npm install
```

### Start Development Server (Hot Reload)
```powershell
npm run dev
```

Frontend will start at `http://localhost:5173` by default (Vite).

## Environment Configuration

### API Base URL

By default, the frontend expects the backend API at `http://localhost:8080/api`.

To override, set the `VITE_API_BASE_URL` environment variable:

```powershell
$env:VITE_API_BASE_URL = "http://your-backend-host:8080/api"
npm run dev
```

Or create a `.env.local` file:
```
VITE_API_BASE_URL=http://your-backend-host:8080/api
```

### HTTP Authentication

The frontend uses HTTP Basic Auth headers for API requests. When making requests:
- Username/password pairs are sent in the `Authorization: Basic` header
- Use credentials from `backend/LOCAL_DEV.md` table (e.g., `user1:password1`)
- Auth is handled automatically by Axios interceptors in `frontend/src/services/api.ts`

## Testing

### Run Tests
```powershell
Set-Location frontend
npm run test
```

Tests run in watch mode (Vitest). Press `q` to exit watch mode.

### Run Tests Once (CI Mode)
```powershell
npm run test -- --run
```

### Code Coverage
```powershell
npm run test -- --coverage
```

## Building for Production

### Build
```powershell
npm run build
```

Output: `frontend/dist/` directory (ready to serve as static files)

### Preview Production Build
```powershell
npm run preview
```

Previews the production build locally for testing.

## Linting & Code Quality

### ESLint Check
```powershell
npm run lint
```

### Fix Linting Issues (Automatic)
```powershell
npm run lint -- --fix
```

### Prettier Format Check
```powershell
npm run format:check
```

### Format Code (Automatic)
```powershell
npm run format
```

## Development Workflow

### 1. Start Backend
```powershell
# In one terminal
cd backend
mvn spring-boot:run
```

### 2. Start Frontend
```powershell
# In another terminal
cd frontend
npm run dev
```

### 3. Open Browser
Navigate to `http://localhost:5173`

### 4. Login
- Use credentials from `backend/LOCAL_DEV.md` (e.g., `user1`/`password1`)
- Auth header is added automatically to all API requests

## Troubleshooting

### "Cannot find module" or npm errors
```powershell
# Clear npm cache and reinstall
rm -Recurse -Force node_modules package-lock.json
npm install
```

### "Backend API not responding (CORS error)"
- Ensure backend is running: `mvn spring-boot:run` from `backend/` directory
- Check backend is accessible: `curl http://localhost:8080/api/program-sessions/next`
- Verify `VITE_API_BASE_URL` matches backend address
- Check browser console for actual error details

### "Port 5173 already in use"
```powershell
# Use a different port
npm run dev -- --port 5174
```

### "Blank page or 404 after build"
- Check `npm run build` output for errors
- Ensure `dist/` directory exists and has files
- For deployment, verify the web server serves `dist/index.html` for all routes (SPA routing)

### "Tests failing with "Cannot find module" errors"
```powershell
# Ensure test dependencies are installed
npm install
# Rebuild Vitest
npm run test -- --clearCache
```

## Hot Module Replacement (HMR)

During development, changes to component files are automatically reflected in the browser without full page reload:
- `.tsx` component changes → instant update
- `.css` changes → instant style update
- `.ts` hook changes → refresh (may require page reload in some cases)

If HMR doesn't work:
1. Check browser console for errors
2. Try manual page refresh
3. Restart dev server: `npm run dev`
