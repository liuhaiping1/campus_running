<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getOrderDetail, contactOrder, pickupOrder, deliverOrder, cancelOrder } from '@/api/order'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatMoney, formatTime } from '@/utils/format'
import { actionLabel, orderStatusLabels } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const orderId = route.params.id

const loading = ref(false)
const acting = ref(false)
const order = ref(null)

const statusLabel = computed(() => {
  if (!order.value) return ''
  return orderStatusLabels[order.value.orderStatus] || '未知'
})

// 履约操作配置（按状态映射）
const actionByStatus = {
  2: { key: 'contact', label: '已联系发布人', desc: '确认已通过电话或消息联系到发布人', api: contactOrder },
  3: { key: 'pickup', label: '确认取件', desc: '确认已从取件地址取到物品', api: pickupOrder },
  4: { key: 'deliver', label: '确认送达', desc: '确认已将物品送达至收货地址', api: deliverOrder },
  5: { key: 'deliver', label: '确认送达', desc: '确认已将物品送达至收货地址', api: deliverOrder }
}

// 是否可取消（已接单、已联系、已取件状态可取消）
const canCancel = computed(() => {
  if (!order.value) return false
  return [2, 3, 4].includes(order.value.orderStatus)
})

const currentAction = computed(() => {
  if (!order.value) return null
  return actionByStatus[order.value.orderStatus] || null
})

async function loadDetail() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId)
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function handleAction() {
  if (!currentAction.value || acting.value) return
  try {
    await ElMessageBox.confirm(
      currentAction.value.desc,
      `确认操作：${currentAction.value.label}`,
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  acting.value = true
  try {
    await currentAction.value.api(orderId)
    ElMessage.success('操作成功')
    await loadDetail()
  } catch {
    // 错误已在请求拦截器统一处理
  } finally {
    acting.value = false
  }
}

// 取消订单（弹出输入框填写原因）
async function handleCancel() {
  if (acting.value) return
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      '请输入取消原因（如订单信息有误、联系不上发布人等）',
      '取消订单',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        inputType: 'textarea',
        inputPlaceholder: '请输入取消原因',
        inputValidator: (val) => {
          if (!val || !val.trim()) return '取消原因不能为空'
          return true
        }
      }
    )
    reason = result.value
    // 二次确认
    await ElMessageBox.confirm(
      '取消订单后可能影响您的信用记录，确定要取消吗？',
      '再次确认',
      { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
    )
  } catch {
    return
  }
  acting.value = true
  try {
    await cancelOrder(orderId, { cancelReason: reason })
    ElMessage.success('订单已取消')
    await loadDetail()
  } catch {
    // 错误已在请求拦截器统一处理
  } finally {
    acting.value = false
  }
}

/** 复制手机号到剪贴板 */
async function copyPhone(phone) {
  try {
    await navigator.clipboard.writeText(phone)
    ElMessage.success('手机号已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="runner-order-detail-page">
    <!-- 返回按钮 -->
    <div class="back-row">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 订单不存在 -->
    <EmptyState v-else-if="!order" description="订单不存在或已被删除" />

    <!-- 详情 -->
    <template v-else>
      <!-- 顶部状态大字号展示 -->
      <div class="status-hero">
        <span class="status-label">{{ statusLabel }}</span>
      </div>

      <!-- 订单基本信息 -->
      <el-card class="detail-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">订单信息</span>
            <StatusTag type="order_status" :value="order.orderStatus" />
          </div>
        </template>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ order.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ order.title }}</el-descriptions-item>
          <el-descriptions-item v-if="order.orderDesc" label="描述">{{ order.orderDesc }}</el-descriptions-item>
          <el-descriptions-item label="取件地址">{{ order.pickupAddress }}</el-descriptions-item>
          <el-descriptions-item label="送达地址">{{ order.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="配送距离">{{ order.distanceKm }}km</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ formatTime(order.deadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ order.contactName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            <span class="contact-phone-row">
              <a v-if="order.contactPhone" :href="'tel:' + order.contactPhone" class="phone-link">{{ order.contactPhone }}</a>
              <span v-else>-</span>
              <el-button
                v-if="order.contactPhone"
                size="small"
                text
                type="primary"
                @click="copyPhone(order.contactPhone)"
              >复制</el-button>
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 金额明细 -->
      <el-card class="detail-card">
        <template #header>
          <span class="card-title">费用明细</span>
        </template>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="基础费">¥{{ formatMoney(order.baseFee) }}</el-descriptions-item>
          <el-descriptions-item v-if="order.distanceFee" label="距离费">¥{{ formatMoney(order.distanceFee) }}</el-descriptions-item>
          <el-descriptions-item v-if="order.timeFee" label="时段费">¥{{ formatMoney(order.timeFee) }}</el-descriptions-item>
          <el-descriptions-item v-if="order.tipFee" label="小费">¥{{ formatMoney(order.tipFee) }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount-highlight">¥{{ formatMoney(order.orderAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预估收益">
            <span class="income-highlight">¥{{ formatMoney(order.estimatedRunnerIncome) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 状态时间线 -->
      <el-card class="detail-card" v-if="order.statusLogs && order.statusLogs.length > 0">
        <template #header>
          <span class="card-title">状态日志</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="log in order.statusLogs"
            :key="log.id"
            :timestamp="formatTime(log.createTime)"
            placement="top"
          >
            <div class="timeline-content">
              <span class="timeline-action">{{ actionLabel(log.triggerAction) }}</span>
              <StatusTag
                type="order_status"
                :value="log.afterStatus"
                size="small"
                style="margin-left: 8px"
              />
            </div>
            <p class="timeline-remark" v-if="log.remark">{{ log.remark }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- 操作区 -->
      <div class="action-bar">
        <!-- 当前履约操作 -->
        <el-button
          v-if="currentAction"
          type="primary"
          size="large"
          :loading="acting"
          @click="handleAction"
          style="flex: 1"
        >
          {{ currentAction.label }}
        </el-button>

        <!-- 取消订单 -->
        <el-button
          v-if="canCancel"
          type="danger"
          size="large"
          :loading="acting"
          @click="handleCancel"
        >
          取消订单
        </el-button>

        <!-- 无可用操作时提示 -->
        <div v-if="!currentAction && !canCancel" class="no-action-tip">
          <el-alert
            :title="order.orderStatus === 7 ? '订单已完成' : order.orderStatus === 8 ? '订单已取消' : '当前状态无需操作'"
            type="info"
            show-icon
            :closable="false"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.runner-order-detail-page {
  max-width: 680px;
  margin: 0 auto;
}

.back-row {
  margin-bottom: 16px;
}

.loading-wrap {
  padding: 24px 0;
}

/* 顶部状态横幅 */
.status-hero {
  text-align: center;
  padding: 24px 0;
  margin-bottom: 16px;
}

.status-label {
  font-size: 28px;
  font-weight: 700;
  color: #409EFF;
}

.detail-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.amount-highlight {
  font-size: 16px;
  font-weight: 600;
  color: #F56C6C;
}

.income-highlight {
  font-size: 16px;
  font-weight: 600;
  color: #E6A23C;
}

/* 时间线 */
.timeline-content {
  display: flex;
  align-items: center;
}

.timeline-action {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.timeline-remark {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

/* 联系电话 */
.contact-phone-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.phone-link {
  color: #409EFF;
  text-decoration: none;
}

/* 操作区 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-bottom: 24px;
}

.no-action-tip {
  flex: 1;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .runner-order-detail-page {
    padding: 0 0 80px;
  }

  .status-label {
    font-size: 24px;
  }

  .action-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 12px 16px;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
    z-index: 50;
    margin-top: 0;
  }
}
</style>
