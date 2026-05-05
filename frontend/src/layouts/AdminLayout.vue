<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { Fold, Expand, DataAnalysis, UserFilled, User, Document, Money, Warning, Folder, Bell, List } from '@element-plus/icons-vue'
import { useResponsive } from '@/composables/useResponsive'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const { isMobile } = useResponsive()

const isCollapsed = ref(isMobile.value)

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 侧边栏菜单项
const menuItems = [
  { path: '/admin/dashboard', title: '统计概览', icon: DataAnalysis },
  { path: '/admin/runner-auth', title: '跑腿员审核', icon: UserFilled },
  { path: '/admin/users', title: '用户管理', icon: User },
  { path: '/admin/orders', title: '订单管理', icon: Document },
  { path: '/admin/refunds', title: '退款处理', icon: Money },
  { path: '/admin/appeals', title: '申诉处理', icon: Warning },
  { path: '/admin/categories', title: '分类管理', icon: Folder },
  { path: '/admin/notices', title: '公告管理', icon: Bell },
  { path: '/admin/audit-logs', title: '审计日志', icon: List }
]

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" :class="{ 'admin-aside': true, 'collapsed-mobile': isCollapsed }">
      <div class="aside-header">
        <span v-show="!isCollapsed" class="logo-text">管理后台</span>
        <el-button
          :icon="isCollapsed ? Expand : Fold"
          text
          class="collapse-btn"
          @click="toggleSidebar"
        />
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="aside-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 移动端遮罩层 -->
    <transition name="fade">
      <div
        v-if="isMobile && !isCollapsed"
        class="sidebar-mask"
        @click="toggleSidebar"
      />
    </transition>

    <!-- 右侧内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="admin-topbar">
        <div class="topbar-left">
          <el-button
            :icon="isCollapsed ? Expand : Fold"
            text
            class="mobile-toggle"
            @click="toggleSidebar"
          />
          <span class="current-page-title">{{ route.meta.title || '' }}</span>
        </div>
        <div class="topbar-right">
          <span class="admin-name">{{ userStore.realName || userStore.username }}</span>
          <el-button text type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

/* 侧边栏 */
.admin-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.aside-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 60px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.collapse-btn {
  color: #bfcbd9;
}

.aside-menu {
  border-right: none;
}

/* 顶部栏 */
.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 24px;
  height: 60px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-toggle {
  display: none;
}

.current-page-title {
  font-size: 16px;
  font-weight: 500;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  font-size: 14px;
  color: #606266;
}

/* 内容区 */
.admin-main {
  background-color: #f5f7fa;
  padding: 24px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-aside {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    /* 移动端侧边栏覆盖显示 */
    width: 220px !important;
  }

  .admin-aside.collapsed-mobile {
    width: 0 !important;
  }

  .mobile-toggle {
    display: inline-flex;
  }

  .admin-main {
    padding: 12px;
  }

  .admin-topbar {
    padding: 0 12px;
  }
}

/* 遮罩层 */
.sidebar-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 199;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
