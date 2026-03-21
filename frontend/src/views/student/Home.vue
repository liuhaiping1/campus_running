<script setup>
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>

<template>
  <div>
    <h3>欢迎回来，{{ userStore.realName }}</h3>
    <p>任务大厅内容即将上线</p>
  </div>
</template>
