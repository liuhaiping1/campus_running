<script setup>
// 占位页面组件，用于路由已定义但业务页面尚未开发的场景
defineProps({
  // 页面名称，用于展示
  title: { type: String, default: '页面' },
  // 描述文字
  description: { type: String, default: '' }
})
</script>

<template>
  <div class="placeholder-page">
    <el-result icon="info" :title="title" :sub-title="description || `${title}功能即将上线，敬请期待`">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>
