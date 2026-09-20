import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

 export default defineConfig({
   plugins: [react()],
   server: {
    port: 4672,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:4673',
    },
  },
})