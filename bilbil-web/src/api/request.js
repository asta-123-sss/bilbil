import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import router from '../router'

// 统一 axios 实例：baseURL 为 /api，开发环境经 Vite 代理转发到后端
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 请求拦截：自动携带登录 token
request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.token = auth.token
  }
  return config
})

// 响应拦截：统一解包 Result{code,msg,data}
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res.data
    }
    if (res && res.code === 401) {
      const auth = useAuthStore()
      auth.logout()
      ElMessage.error(res.msg || '登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error((res && res.msg) || '请求失败')
    return Promise.reject(new Error((res && res.msg) || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
