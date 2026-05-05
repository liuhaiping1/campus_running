<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime } from '@/utils/format'

const props = defineProps({
  order: { type: Object, required: true },
  // mode: 'hall' | 'runner' | 'my'
  mode: { type: String, default: 'my' },
  clickable: { type: Boolean, default: true }
})

const emit = defineEmits(['accept'])

const router = useRouter()

const isHall = computed(() => props.mode === 'hall')
const isRunner = computed(() => props.mode === 'runner')

function handleClick() {
  if (!props.clickable) return
  if (isRunner.value || isHall.value) {
    router.push(`/runner/order/${props.order.id}`)
  } else {
    router.push(`/order/${props.order.id}`)
  }
}

function handleAccept() {
  emit('accept', props.order.id)
}
</script>

<template>
  <div class="order-card" :class="{ clickable }" @click="handleClick">
    <!-- 顶部：标题 + 状态 -->
    <div class="card-header">
      <span class="card-title">{{ order.title }}</span>
      <StatusTag type="order_status" :value="order.orderStatus" size="small" />
    </div>

    <!-- 订单号 -->
    <div class="card-order-no" v-if="order.orderNo">
      <span class="label">订单号</span>
      <span class="value">{{ order.orderNo }}</span>
    </div>

    <!-- 分类 -->
    <div class="card-info" v-if="order.categoryName">
      <span class="label">分类</span>
      <span class="value">{{ order.categoryName }}</span>
    </div>

    <!-- 地址信息 -->
    <div class="card-addresses">
      <div class="address-row">
        <span class="address-icon pick">取</span>
        <span class="address-text">{{ order.pickupAddress }}</span>
      </div>
      <div class="address-row">
        <span class="address-icon delivery">送</span>
        <span class="address-text">{{ order.deliveryAddress }}</span>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="card-footer">
      <div class="footer-left">
        <template v-if="isHall">
          <span class="income-label">预估收益</span>
          <span class="income-value">¥{{ formatMoney(order.estimatedRunnerIncome) }}</span>
        </template>
        <template v-else>
          <span class="amount">¥{{ formatMoney(order.orderAmount) }}</span>
        </template>
        <span class="divider" v-if="order.distanceKm != null">|</span>
        <span class="distance" v-if="order.distanceKm != null">{{ order.distanceKm }}km</span>
      </div>
      <div class="footer-right">
        <span class="deadline" v-if="order.deadlineTime">
          截止 {{ formatTime(order.deadlineTime) }}
        </span>
        <span class="create-time" v-else-if="order.createTime">
          {{ formatTime(order.createTime) }}
        </span>
        <!-- 接单按钮（仅大厅模式） -->
        <el-button
          v-if="isHall"
          type="primary"
          size="small"
          @click.stop="handleAccept"
        >
          接单
        </el-button>
        <!-- 跑腿员模式：点击查看详情 -->
        <el-button
          v-if="isRunner"
          type="primary"
          size="small"
          link
          @click.stop="handleClick"
        >
          履约详情
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card.clickable {
  cursor: pointer;
}

.order-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.card-order-no,
.card-info {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.label {
  color: #909399;
  flex-shrink: 0;
}

.value {
  color: #606266;
}

.card-addresses {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.address-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.address-icon {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.address-icon.pick {
  background-color: #409EFF;
}

.address-icon.delivery {
  background-color: #67C23A;
}

.address-text {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.income-label {
  font-size: 12px;
  color: #909399;
}

.income-value {
  font-size: 16px;
  font-weight: 600;
  color: #E6A23C;
}

.amount {
  font-size: 15px;
  font-weight: 600;
  color: #F56C6C;
}

.divider {
  color: #e4e7ed;
}

.distance {
  font-size: 12px;
  color: #909399;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.deadline {
  font-size: 12px;
  color: #E6A23C;
}

.create-time {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .order-card {
    padding: 12px;
    gap: 10px;
  }

  .card-title {
    font-size: 15px;
  }

  .footer-right {
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }
}
</style>
