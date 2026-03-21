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
  <el-container class="front-layout">
    <el-header class="front-header">
      <div class="header-left">
        <h2>校园万能帮</h2>
        <el-menu mode="horizontal" :ellipsis="false" router>
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/runner/hall" v-if="userStore.isRunner">任务大厅</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <span class="username">{{ userStore.realName }}</span>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.front-layout { min-height: 100vh; }
.front-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e6e6e6;
}
.header-left { display: flex; align-items: center; gap: 24px; }
.header-left h2 { margin: 0; white-space: nowrap; }
.header-right { display: flex; align-items: center; gap: 12px; }
</style>
