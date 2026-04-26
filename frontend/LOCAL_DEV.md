# Frontend Local Development

## Requirements
- Node.js 20+
- npm 10+

## Install and run
```powershell
Set-Location frontend
npm install
npm run dev
```

## Environment
The frontend expects the backend at `http://localhost:8080/api` by default.
Set `VITE_API_BASE_URL` to override it.

## Tests
```powershell
Set-Location frontend
npm run test
npm run build
```

