<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 列表数据
  data: { type: Array, default: () => [] },
  // 加载状态
  loading: { type: Boolean, default: false },
  // 空状态文案
  emptyText: { type: String, default: '暂无数据' }
})

// 屏幕宽度 <= 768px 视为移动端，展示卡片插槽
const isMobile = computed(() => {
  if (typeof window === 'undefined') return false
  return window.innerWidth <= 768
})
</script>

<template>
  <div class="responsive-container">
    <!-- PC 端表格区域 -->
    <div v-if="!isMobile && !loading" class="table-area">
      <slot v-if="data.length > 0" name="table" :data="data" />
      <div v-else class="empty-area">
        <el-empty :description="emptyText" />
      </div>
    </div>

    <!-- 移动端卡片区域 -->
    <div v-if="isMobile && !loading" class="card-area">
      <slot v-if="data.length > 0" name="card" :data="data" />
      <div v-else class="empty-area">
        <el-empty :description="emptyText" />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 分页插槽（PC 和移动端共用） -->
    <div v-if="$slots.pagination" class="pagination-area">
      <slot name="pagination" />
    </div>
  </div>
</template>

<style scoped>
.responsive-container {
  width: 100%;
}

.empty-area {
  padding: 48px 0;
}

.loading-area {
  padding: 24px;
}

.pagination-area {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
