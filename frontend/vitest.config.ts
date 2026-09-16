import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

/**
 * Config Vitest separee de vite.config.ts (Phase 10 - tache #88) pour ne
 * pas embarquer le plugin Tailwind (inutile en test, et plus lent) ni
 * risquer d'affecter `vite build`/`vite dev`. Le plugin @vitejs/plugin-react
 * est repris ici pour la transformation JSX/TSX des composants et des tests.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    globals: false,
  },
})
