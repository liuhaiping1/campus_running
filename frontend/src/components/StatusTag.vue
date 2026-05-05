<script setup>
import { computed, onMounted } from 'vue'
import { useDictStore } from '@/stores/dict'

const props = defineProps({
  type: { type: String, required: true },
  // value 支持 String/Number/Boolean，audit_success 类型会传入布尔值
  value: { type: [String, Number, Boolean], required: true },
  size: { type: String, default: 'default' }
})

const dictStore = useDictStore()

const statusMap = {
  order_status: {
    0: { label: '待支付', class: 'info' },
    1: { label: '待接单', class: 'warning' },
    2: { label: '已接单', class: 'primary' },
    3: { label: '已联系用户', class: 'primary' },
    4: { label: '已取件', class: 'warning' },
    5: { label: '配送中', class: 'primary' },
    6: { label: '已送达', class: 'success' },
    7: { label: '已完成', class: 'success' },
    8: { label: '已取消', class: 'info' },
    9: { label: '已关闭', class: 'info' },
    10: { label: '申诉中', class: 'danger' }
  },
  pay_status: {
    0: { label: '未支付', class: 'info' },
    1: { label: '支付中', class: 'warning' },
    2: { label: '已支付', class: 'success' },
    3: { label: '退款中', class: 'warning' },
    4: { label: '已退款', class: 'info' },
    5: { label: '支付关闭', class: 'info' },
    6: { label: '部分退款', class: 'warning' }
  },
  auth_status: {
    0: { label: '待审核', class: 'warning' },
    1: { label: '审核通过', class: 'success' },
    2: { label: '审核驳回', class: 'danger' },
    3: { label: '已失效', class: 'info' }
  },
  settlement_status: {
    0: { label: '待结算', class: 'warning' },
    1: { label: '结算中', class: 'primary' },
    2: { label: '已结算', class: 'success' },
    3: { label: '已回滚', class: 'danger' }
  },
  message_read_status: {
    0: { label: '未读', class: 'danger' },
    1: { label: '已读', class: 'info' }
  },
  refund_status: {
    0: { label: '待审核', class: 'warning' },
    1: { label: '审核中', class: 'primary' },
    2: { label: '已退款', class: 'success' },
    3: { label: '已驳回', class: 'danger' }
  },
  appeal_status: {
    0: { label: '待处理', class: 'warning' },
    1: { label: '处理中', class: 'primary' },
    2: { label: '申诉成立', class: 'success' },
    3: { label: '申诉驳回', class: 'danger' },
    4: { label: '已关闭', class: 'info' }
  },
  notice_status: {
    0: { label: '草稿', class: 'info' },
    1: { label: '已发布', class: 'success' },
    2: { label: '已下架', class: 'warning' }
  },
  // 公告类型：普通/重要/维护，与公告状态不同
  notice_type: {
    1: { label: '普通', class: 'info' },
    2: { label: '重要', class: 'warning' },
    3: { label: '维护', class: 'danger' }
  },
  category_status: {
    0: { label: '禁用', class: 'info' },
    1: { label: '启用', class: 'success' }
  },
  user_status: {
    1: { label: '正常', class: 'success' },
    2: { label: '禁用', class: 'danger' }
  },
  audit_success: {
    true: { label: '成功', class: 'success' },
    false: { label: '失败', class: 'danger' }
  }
}

const tagInfo = computed(() => {
  const label = dictStore.getDictLabel(props.type, props.value)
  const cssClass = dictStore.getDictClass(props.type, props.value)
  if (label !== String(props.value) && cssClass) {
    return { label, class: cssClass }
  }
  const map = statusMap[props.type]
  if (map && map[props.value] !== undefined) {
    return map[props.value]
  }
  return { label: String(props.value), class: 'info' }
})

onMounted(() => {
  dictStore.loadDict(props.type)
})
</script>

<template>
  <el-tag :type="tagInfo.class" :size="size" disable-transitions>
    {{ tagInfo.label }}
  </el-tag>
</template>
