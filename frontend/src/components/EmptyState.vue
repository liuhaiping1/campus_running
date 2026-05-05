<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' }
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description">
      <el-button v-if="actionText" type="primary" @click="emit('action')">
        {{ actionText }}
      </el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.empty-state {
  padding: 48px 0;
}
</style>