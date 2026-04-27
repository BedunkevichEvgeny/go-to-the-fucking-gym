import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward /api calls to the Spring Boot backend during local development.
      // The browser always speaks to the same origin (localhost:5173) so no CORS
      // headers are ever needed for smoke-testing on a single machine.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
