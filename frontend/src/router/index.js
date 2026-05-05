import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫：每次路由切换执行权限校验、角色检查和标题设置
router.beforeEach((to, from, next) => {
  // 1. 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 校园万能帮`
  } else {
    document.title = '校园万能帮跑腿服务平台'
  }

  const userStore = useUserStore()

  // 2. 需要登录但未登录 → 跳转登录页（携带 redirect 参数）
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    const redirect = encodeURIComponent(to.fullPath)
    return next(`/login?redirect=${redirect}`)
  }

  // 3. 已登录时访问登录/注册等 guest 页面 → 跳转首页
  if (to.meta.guest && userStore.isLoggedIn) {
    return next('/')
  }

  // 4. 角色权限校验：meta.roles 不为空时，用户角色必须在列表中
  if (to.meta.roles && to.meta.roles.length > 0) {
    const hasRole = to.meta.roles.some(role => userStore.hasRole(role))
    if (!hasRole) {
      return next('/403')
    }
  }

  // 5. 兼容旧版 requiresAdmin 标记
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return next('/403')
  }

  next()
})

export default router