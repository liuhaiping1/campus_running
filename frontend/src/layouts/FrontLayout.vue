<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { Menu, HomeFilled, EditPen, Document, Bell, Message, Tickets, Money, List, User, Folder } from '@element-plus/icons-vue'
import { useResponsive } from '@/composables/useResponsive'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const { isMobile } = useResponsive()

const activeMenu = computed(() => {
  if (route.path === '/') return '/'
  const segments = route.path.split('/').filter(Boolean)
  if (segments[0] === 'runner') {
    return `/${segments[0]}/${segments[1] || ''}`
  }
  return `/${segments[0] || ''}`
})

/** 顶部导航菜单项：统一保留学生端入口，RUNNER 额外追加跑腿入口 */
const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页' },
    { path: '/notices', label: '公告' }
  ]
  if (!userStore.isLoggedIn) return items
  // 登录后通用入口
  items.push({ path: '/profile', label: '个人中心' })
  items.push({ path: '/order/my', label: '我的订单' })
  items.push({ path: '/user/messages', label: '消息中心' })
  // 学生角色（或非管理员）：发布和地址管理
  const isStudent = userStore.hasRole('STUDENT') || !userStore.isAdmin
  if (isStudent) {
    items.push({ path: '/order/create', label: '发布订单' })
    items.push({ path: '/user/address', label: '地址管理' })
  }
  // 跑腿员额外入口
  if (userStore.isRunner) {
    items.push({ path: '/runner/hall', label: '任务大厅' })
    items.push({ path: '/runner/orders', label: '我的接单' })
    items.push({ path: '/runner/income', label: '收益' })
    items.push({ path: '/runner/evaluations', label: '评价' })
  }
  // 管理员后台入口
  if (userStore.isAdmin) {
    items.push({ path: '/admin/dashboard', label: '后台管理' })
  }
  return items
})

/** 底部 Tab 栏配置（移动端）：RUNNER 可同时访问发布和接单入口 */
const tabItems = computed(() => {
  const tabs = [
    { path: '/', label: '首页', icon: HomeFilled }
  ]
  if (userStore.isAdmin) {
    tabs.push({ path: '/admin/dashboard', label: '后台', icon: List })
  } else if (userStore.isRunner) {
    tabs.push({ path: '/order/create', label: '发布', icon: EditPen })
    tabs.push({ path: '/runner/orders', label: '接单', icon: Document })
    tabs.push({ path: '/runner/income', label: '收益', icon: Money })
    tabs.push({ path: '/profile', label: '我的', icon: User })
  } else {
    tabs.push({ path: '/order/create', label: '发布', icon: EditPen })
    tabs.push({ path: '/order/my', label: '订单', icon: Document })
    tabs.push({ path: '/user/messages', label: '消息', icon: Message })
    tabs.push({ path: '/profile', label: '我的', icon: User })
  }
  return tabs.slice(0, 5)
})

// 当前激活的 Tab（精确匹配或前缀匹配）
const activeTab = computed(() => {
  const path = route.path
  const matched = tabItems.value.find(t => path === t.path || path.startsWith(t.path + '/'))
  return matched ? matched.path : '/'
})

function handleTabClick(path) {
  router.push(path)
}

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<template>
  <el-container class="front-layout">
    <!-- 顶部导航栏 -->
    <el-header class="front-header">
      <div class="header-left">
        <h2 class="project-name" @click="router.push('/')">校园万能帮</h2>
        <template v-if="isMobile">
          <el-dropdown trigger="click" @command="router.push($event)">
            <el-button :icon="Menu" text size="large" style="color: #333" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in navItems" :key="item.path" :command="item.path">
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-menu
          v-else
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          router
          class="header-menu"
        >
          <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
            {{ item.label }}
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧用户区 -->
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-button text @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button text @click="router.push('/login')">登录</el-button>
          <el-button text @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <!-- 内容区：居中、限制最大宽度 -->
    <el-main class="front-main">
      <div class="front-content">
        <router-view />
      </div>
    </el-main>

    <!-- 移动端底部 Tab 栏 -->
    <div v-if="isMobile" class="bottom-tab-bar">
      <div
        v-for="tab in tabItems"
        :key="tab.path"
        class="tab-item"
        :class="{ active: activeTab === tab.path }"
        @click="handleTabClick(tab.path)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>
  </el-container>
</template>

<style scoped>
.front-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.front-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 24px;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.project-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  color: var(--color-primary, #409EFF);
}

.header-menu {
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  height: 60px;
  line-height: 60px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-size: 14px;
  color: #303133;
}

.front-main {
  display: flex;
  justify-content: center;
  padding: 24px;
}

.front-content {
  width: 100%;
  max-width: var(--content-max-width, 1200px);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .front-header {
    padding: 0 12px;
  }

  .header-left {
    gap: 8px;
  }

  .project-name {
    font-size: 16px;
  }

  /* 移动端隐藏部分菜单文字 */
  .header-menu {
    font-size: 13px;
  }

  .front-main {
    padding: 12px;
    padding-bottom: 68px;
  }
}

/* 底部 Tab 栏 */
.bottom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 100;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1;
  height: 100%;
  cursor: pointer;
  color: #909399;
  transition: color 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  color: var(--color-primary, #409EFF);
}

.tab-label {
  font-size: 11px;
  line-height: 1;
}
</style>
