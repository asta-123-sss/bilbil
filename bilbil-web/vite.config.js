import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发环境把 /api 代理到后端，避免跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
