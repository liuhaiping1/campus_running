<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getOrderDetail, cancelOrder, completeOrder, contactOrder, pickupOrder, deliverOrder, payOrder } from '@/api/order'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ConfirmActionButton from '@/components/ConfirmActionButton.vue'
import { formatTime, formatMoney } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'
import { actionLabel } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const orderId = String(route.params.id)
const loading = ref(false)
const order = ref(null)
const statusLogs = ref([])
const actionLoading = ref(false)
const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const { isMobile } = useResponsive()

// 判断当前用户与订单的关系
const isPublisher = () => order.value?.publisherId === userStore.userId
const isRunner = () => order.value?.runnerId === userStore.userId

async function fetchDetail() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId)
    statusLogs.value = order.value.statusLogs || []
  } catch {
    order.value = null
    statusLogs.value = []
  } finally {
    loading.value = false
  }
}

// 发布人可用操作
const publisherActions = computed(() => {
  if (!order.value || !isPublisher()) return []
  const status = order.value.orderStatus
  const actions = []
  if (status === 0 && order.value.payStatus !== 2) {
    actions.push({
      key: 'pay',
      label: order.value.payStatus === 1 ? '继续支付' : '去支付',
      type: 'primary'
    })
  }
  // 取消：未完成且未取消状态下可用
  if ([0, 1, 2, 3, 4].includes(status)) actions.push({ key: 'cancel', label: '取消订单', type: 'danger' })
  if (status === 6) actions.push({ key: 'complete', label: '确认完成', type: 'success' })
  if (status === 7 && !order.value.evaluationId) actions.push({ key: 'evaluate', label: '去评价', type: 'primary' })
  if (status >= 2 && status <= 7) actions.push({ key: 'appeal', label: '提交申诉', type: 'warning' })
  return actions
})

// 跑腿员可用操作
const runnerActions = computed(() => {
  if (!order.value || !isRunner()) return []
  const status = order.value.orderStatus
  if (status === 2) return [{ key: 'contact', label: '已联系用户', type: 'primary' }]
  if (status === 3) return [{ key: 'pickup', label: '确认取件', type: 'warning' }]
  if (status === 4) return [{ key: 'deliver', label: '确认送达', type: 'success' }]
  return []
})

async function handleAction(key) {
  if (key === 'pay') {
    try {
      const payForm = await payOrder(orderId)
      const newWindow = window.open('', '_blank')
      newWindow.document.write(payForm)
      newWindow.document.close()
    } catch {
      // 错误由请求层提示
    }
    return
  }
  if (key === 'evaluate') {
    router.push(`/order/${orderId}/evaluation`)
    return
  }
  if (key === 'appeal') {
    router.push(`/order/${orderId}/appeal`)
    return
  }
  if (key === 'cancel') {
    cancelReason.value = ''
    cancelDialogVisible.value = true
    return
  }

  // 其他操作：调用对应接口
  actionLoading.value = true
  try {
    const actionMap = {
      contact: () => contactOrder(orderId),
      pickup: () => pickupOrder(orderId),
      deliver: () => deliverOrder(orderId),
      complete: () => completeOrder(orderId)
    }
    if (actionMap[key]) {
      await actionMap[key]()
      ElMessage.success('操作成功')
      fetchDetail()
    }
  } catch {
    // 错误由请求层提示
  } finally {
    actionLoading.value = false
  }
}

// 确认取消订单
async function confirmCancel() {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请填写取消原因')
    return
  }
  actionLoading.value = true
  try {
    await cancelOrder(orderId, { cancelReason: cancelReason.value.trim() })
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
    fetchDetail()
  } catch {
    // 错误由请求层提示
  } finally {
    actionLoading.value = false
  }
}

// 各操作按钮需要的确认文案
const confirmTextMap = {
  contact: '确认已联系到发布人？',
  pickup: '确认已取到物品？',
  deliver: '确认已送达目的地？',
  complete: '确认订单已完成？'
}

onMounted(fetchDetail)
</script>

<template>
  <PageContainer title="订单详情" show-back @back="router.push('/order/my')">
    <div v-loading="loading" class="detail-page">
      <!-- 订单不存在 -->
      <el-result
        v-if="!loading && !order"
        icon="error"
        title="订单不存在"
        sub-title="请检查订单号是否正确"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/order/my')">返回我的订单</el-button>
        </template>
      </el-result>

      <template v-else-if="order">
        <!-- 订单状态醒目展示 -->
        <div class="order-status-bar">
          <div class="status-left">
            <StatusTag type="order_status" :value="order.orderStatus" size="large" />
            <StatusTag type="pay_status" :value="order.payStatus" size="large" />
            <span class="order-no">{{ order.orderNo }}</span>
          </div>
          <div class="status-right">
            <span class="order-amount" v-if="order.orderAmount">¥{{ formatMoney(order.orderAmount) }}</span>
          </div>
        </div>

        <!-- 订单基本信息 -->
        <el-descriptions title="基本信息" :column="2" border class="info-block">
          <el-descriptions-item label="任务标题">{{ order.title }}</el-descriptions-item>
          <el-descriptions-item label="任务分类">{{ order.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="2">{{ order.orderDesc }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ formatTime(order.deadlineTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 地址信息 -->
        <el-descriptions title="地址信息" :column="2" border class="info-block">
          <el-descriptions-item label="取件地址" :span="2">{{ order.pickupAddress }}</el-descriptions-item>
          <el-descriptions-item label="送达地址" :span="2">{{ order.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="预估距离">{{ order.distanceKm }} km</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ formatTime(order.acceptTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 费用明细 -->
        <el-descriptions title="费用明细" :column="2" border class="info-block">
          <el-descriptions-item label="基础费用">{{ order.baseFee != null ? '¥' + formatMoney(order.baseFee) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="距离费用">{{ order.distanceFee != null ? '¥' + formatMoney(order.distanceFee) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="小费">¥{{ formatMoney(order.tipFee ?? 0) }}</el-descriptions-item>
          <el-descriptions-item label="订单总金额">
            <span class="amount-highlight">{{ order.orderAmount != null ? '¥' + formatMoney(order.orderAmount) : '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="isRunner()" label="平台抽成">{{ order.platformCommission != null ? '¥' + formatMoney(order.platformCommission) : '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="isRunner()" label="跑腿员预估收益">{{ order.estimatedRunnerIncome != null ? '¥' + formatMoney(order.estimatedRunnerIncome) : '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 取消原因（如已取消） -->
        <el-descriptions v-if="order.orderStatus === 8 && order.cancelReason" title="取消信息" :column="1" border class="info-block">
          <el-descriptions-item label="取消原因">{{ order.cancelReason }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatTime(order.cancelTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 操作按钮 -->
        <div v-if="publisherActions.length > 0 || runnerActions.length > 0" class="action-bar">
          <template v-for="act in publisherActions" :key="act.key">
            <el-button
              v-if="act.key === 'cancel'"
              :type="act.type"
              :loading="actionLoading"
              @click="handleAction(act.key)"
            >
              {{ act.label }}
            </el-button>
            <el-button
              v-else-if="act.key !== 'appeal'"
              :type="act.type"
              :loading="actionLoading"
              @click="handleAction(act.key)"
            >
              {{ act.label }}
            </el-button>
            <el-button
              v-else
              :type="act.type"
              plain
              @click="handleAction(act.key)"
            >
              {{ act.label }}
            </el-button>
          </template>
          <template v-for="act in runnerActions" :key="act.key">
            <ConfirmActionButton
              :text="act.label"
              :type="act.type"
              :confirm-text="confirmTextMap[act.key] || '确定执行该操作？'"
              :action="() => handleAction(act.key)"
              :success-text="act.label + '成功'"
            />
          </template>
        </div>

        <!-- 状态流转日志 -->
        <div v-if="statusLogs.length > 0" class="info-block">
          <h4 class="section-title">状态流转</h4>
          <el-timeline>
            <el-timeline-item
              v-for="log in statusLogs"
              :key="log.id"
              :timestamp="formatTime(log.createTime)"
              placement="top"
            >
              <p class="log-remark">{{ log.remark }}</p>
              <p class="log-meta">
                <StatusTag type="order_status" :value="log.afterStatus" size="small" />
                <span style="margin-left:8px;font-size:12px;color:#909399">
                  操作人: {{ log.operatorRole }} | {{ actionLabel(log.triggerAction) }}
                </span>
              </p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </div>

    <!-- 取消订单弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" :width="isMobile ? '95%' : '460px'" :close-on-click-modal="false">
      <el-form>
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="3"
            placeholder="请输入取消原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.detail-page {
  max-width: 900px;
}

.order-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 14px;
  color: #909399;
  font-family: monospace;
}

.order-amount {
  font-size: 22px;
  font-weight: 700;
  color: #F56C6C;
}

.info-block {
  margin-bottom: 16px;
}

.amount-highlight {
  font-weight: 700;
  color: #F56C6C;
  font-size: 15px;
}

.section-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.action-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.log-remark {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #303133;
}

.log-meta {
  margin: 0;
  display: flex;
  align-items: center;
}

@media (max-width: 768px) {
  .order-status-bar {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
    padding: 16px;
  }

  .action-bar {
    flex-direction: column;
    width: 100%;
  }

  .action-bar .el-button,
  .action-bar :deep(.el-button) {
    width: 100%;
  }
}
</style>
