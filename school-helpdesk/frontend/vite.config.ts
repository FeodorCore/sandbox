import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Порт backend-а в режиме разработки (по умолчанию 8080).
// В production собранный фронтенд раздаёт сам Spring Boot, поэтому прокси не нужен.
const BACKEND = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  build: {
    // собираем сразу в статические ресурсы backend-а:
    // один JAR/WAR раздаёт и API, и интерфейс на порту 80
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: false,
    proxy: {
      '/api': {
        target: BACKEND,
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
    },
  },
});


