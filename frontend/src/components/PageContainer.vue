<script setup>
import { ArrowLeft, Loading } from '@element-plus/icons-vue'

defineProps({
  title: String,
  subtitle: String,
  showBack: Boolean,
  loading: Boolean,
  fullWidth: Boolean
})

const emit = defineEmits(['back'])

function handleBack() {
  emit('back')
}
</script>

<template>
  <div class="page-container" :class="{ 'full-width': fullWidth }">
    <div class="page-header">
      <div class="header-left">
        <el-button v-if="showBack" text @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="title-area">
          <h1 class="page-title">{{ title }}</h1>
          <span v-if="subtitle" class="page-subtitle">{{ subtitle }}</span>
        </div>
      </div>
      <div class="header-right">
        <slot name="extra" />
      </div>
    </div>
    <div v-if="loading" class="loading-mask">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <div class="page-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  position: relative;
}
.page-container.full-width {
  max-width: 100%;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  min-height: 40px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title-area {
  display: flex;
  flex-direction: column;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}
.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.loading-mask {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}
.loading-mask .el-icon {
  font-size: 32px;
  color: #409EFF;
}
.page-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}
@media (max-width: 768px) {
  .page-container {
    padding: 16px;
  }
  .page-title {
    font-size: 18px;
  }
}
</style>