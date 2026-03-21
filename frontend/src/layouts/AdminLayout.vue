<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <h3 class="logo">管理后台</h3>
      <el-menu router default-active="/admin">
        <el-menu-item index="/admin">统计概览</el-menu-item>
        <el-menu-item index="/admin/runner-auth">跑腿员审核</el-menu-item>
        <el-menu-item index="/admin/orders">订单管理</el-menu-item>
        <el-menu-item index="/admin/category">分类管理</el-menu-item>
        <el-menu-item index="/admin/notice">公告管理</el-menu-item>
        <el-menu-item index="/admin/audit-log">审计日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span>{{ userStore.realName }}</span>
        <el-button text @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout { min-height: 100vh; }
.admin-aside { background: #304156; }
.admin-aside .logo { color: #fff; text-align: center; padding: 16px 0; margin: 0; }
.admin-header { display: flex; align-items: center; justify-content: flex-end; gap: 12px; border-bottom: 1px solid #e6e6e6; }
</style>
