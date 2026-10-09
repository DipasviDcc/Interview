import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backend = 'http://127.0.0.1:8080'
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/ask': backend,
      '/api': backend,
      '/v3/api-docs': backend,
      '/swagger-ui': backend,
    },
  },
  preview: {
    proxy: { '/ask': backend, '/api': backend, '/v3/api-docs': backend, '/swagger-ui': backend },
  },
})
