import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

// 防重复：401 跳转锁和通用错误消息节流
let isRedirecting = false
let lastErrorMessage = ''
let lastErrorTime = 0

const request = axios.create({
  // 优先使用环境变量，默认不留前缀（API 路径自带 /api）
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    // 后端统一返回 Result<T>，code === 0 表示成功
    if (res.code === 0) {
      return res.data
    }
    // 业务错误：展示消息但不吞掉错误，调用方可继续 catch
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    // HTTP 401：未登录或 token 过期，清理状态跳转登录页
    if (error.response?.status === 401) {
      if (!isRedirecting) {
        isRedirecting = true
        const userStore = useUserStore()
        userStore.clearUser()
        const redirect = encodeURIComponent(router.currentRoute.value.fullPath)
        router.push(`/login?redirect=${redirect}`)
        // 1 秒后释放锁，防止短时间内多次跳转
        setTimeout(() => { isRedirecting = false }, 1000)
      }
      return Promise.reject(error)
    }

    // HTTP 403：无权限
    if (error.response?.status === 403) {
      ElMessage.error('无权限执行该操作')
      return Promise.reject(error)
    }

    // 网络错误：1 秒内相同消息不重复弹出
    const msg = error.message || '网络异常，请稍后重试'
    const now = Date.now()
    if (msg !== lastErrorMessage || now - lastErrorTime > 1000) {
      lastErrorMessage = msg
      lastErrorTime = now
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request