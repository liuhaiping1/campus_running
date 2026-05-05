<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  // 按钮文本
  text: { type: String, default: '确认' },
  // 确认弹窗文案
  confirmText: { type: String, default: '确定要执行该操作吗？' },
  // 确认弹窗标题
  confirmTitle: { type: String, default: '操作确认' },
  // Element Plus 按钮类型
  type: { type: String, default: 'danger' },
  // 按钮大小
  size: { type: String, default: 'default' },
  // 是否禁用
  disabled: { type: Boolean, default: false },
  // 确认按钮文案
  confirmButtonText: { type: String, default: '确定' },
  // 取消按钮文案
  cancelButtonText: { type: String, default: '取消' },
  // 是否使用 popconfirm 样式（气泡确认，适合表格行内操作）
  popconfirm: { type: Boolean, default: false },
  // 操作函数，返回 Promise
  action: { type: Function, required: true },
  // 成功提示文案，为空则不弹提示
  successText: { type: String, default: '' }
})

const emit = defineEmits(['success', 'error'])

// 按钮 loading 防重复点击
const loading = ref(false)

async function handleClick() {
  if (props.popconfirm) {
    // popconfirm 模式下直接执行（el-popconfirm 已确认）
    await executeAction()
    return
  }

  // 使用 ElMessageBox 二次确认
  try {
    await ElMessageBox.confirm(props.confirmText, props.confirmTitle, {
      confirmButtonText: props.confirmButtonText,
      cancelButtonText: props.cancelButtonText,
      type: 'warning'
    })
    await executeAction()
  } catch {
    // 用户取消操作，不做任何处理
  }
}

async function executeAction() {
  loading.value = true
  try {
    await props.action()
    if (props.successText) {
      ElMessage.success(props.successText)
    }
    emit('success')
  } catch (err) {
    // 错误已由请求层统一提示，这里只转发事件
    emit('error', err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-popconfirm
    v-if="popconfirm"
    :title="confirmText"
    :confirm-button-text="confirmButtonText"
    :cancel-button-text="cancelButtonText"
    @confirm="executeAction"
  >
    <template #reference>
      <el-button
        :type="type"
        :size="size"
        :disabled="disabled"
        :loading="loading"
      >
        {{ text }}
      </el-button>
    </template>
  </el-popconfirm>

  <el-button
    v-else
    :type="type"
    :size="size"
    :disabled="disabled"
    :loading="loading"
    @click="handleClick"
  >
    {{ text }}
  </el-button>
</template>
