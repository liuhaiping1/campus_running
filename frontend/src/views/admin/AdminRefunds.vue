<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefundList, approveRefund } from '@/api/adminRefund'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  refundStatus: undefined,
  page: 1,
  size: 10
})

const { windowWidth, isMobile } = useResponsive()

// 审核弹窗
const reviewVisible = ref(false)
const reviewForm = reactive({ refundStatus: null, approveResult: '' })
const reviewing = ref(false)
const currentRecord = ref(null)

/** 加载退款列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = { page: query.page, size: query.size }
    if (query.refundStatus !== undefined && query.refundStatus !== '') params.refundStatus = query.refundStatus
    const result = await getRefundList(params)
    records.value = result.records || []
    total.value = parseTotal(result.total)
  } catch {
    error.value = true
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) { query.page = page; fetchRecords() }
function handleFilterChange() { query.page = 1; fetchRecords() }
function resetFilter() { query.refundStatus = undefined; query.page = 1; fetchRecords() }

/** 打开审核弹窗 */
function openReview(record) {
  currentRecord.value = record
  reviewForm.refundStatus = 2
  reviewForm.approveResult = ''
  reviewVisible.value = true
}

/** 提交退款审核 */
async function submitReview() {
  if (reviewForm.refundStatus === null) {
    ElMessage.warning('请选择审核结果')
    return
  }
  // 后端 RefundApproveRequest.approveResult 标了 @NotBlank，通过和拒绝都必填
  if (!reviewForm.approveResult.trim()) {
    ElMessage.warning('请填写处理备注')
    return
  }
  const actionText = reviewForm.refundStatus === 2 ? '同意退款' : '拒绝退款'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}吗？此操作不可撤销。`,
      `确认${actionText}`,
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: reviewForm.refundStatus === 2 ? 'success' : 'warning' }
    )
  } catch { return }

  reviewing.value = true
  try {
    await approveRefund(currentRecord.value.id, {
      refundStatus: reviewForm.refundStatus,
      approveResult: reviewForm.approveResult || undefined
    })
    ElMessage.success(`${actionText}成功`)
    reviewVisible.value = false
    fetchRecords()
  } catch {
    // 错误已在请求拦截器统一处理
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-refunds-page">
    <div class="page-header"><h2 class="page-title">退款处理</h2></div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="query.refundStatus" placeholder="退款状态" clearable style="width:140px" @change="handleFilterChange">
        <el-option label="待审核" :value="0" />
        <el-option label="审核中" :value="1" />
        <el-option label="已退款" :value="2" />
        <el-option label="已驳回" :value="3" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 状态 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card"><el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty></el-card>
    <el-empty v-else-if="records.length === 0" description="暂无退款记录" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="orderId" label="订单ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" width="150" show-overflow-tooltip />
          <el-table-column prop="refundAmount" label="退款金额" width="110" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column prop="refundStatus" label="状态" width="100">
            <template #default="{ row }"><StatusTag type="refund_status" :value="row.refundStatus" /></template>
          </el-table-column>
          <el-table-column prop="refundReason" label="退款原因" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.refundStatus === 0 || row.refundStatus === 1" type="primary" size="small" @click="openReview(row)">处理</el-button>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="refund-card">
          <div class="card-row"><span class="card-label">订单号</span><span class="card-value">{{ r.orderNo }}</span><StatusTag type="refund_status" :value="r.refundStatus" size="small" /></div>
          <div class="card-row"><span class="card-label">退款金额</span><span class="card-value amount">¥{{ formatMoney(r.refundAmount) }}</span></div>
          <div class="card-row" v-if="r.refundReason"><span class="card-label">原因</span><span class="card-value">{{ r.refundReason }}</span></div>
          <div class="card-row"><span class="card-label">时间</span><span class="card-value">{{ formatTime(r.createTime) }}</span></div>
          <div class="card-actions" v-if="r.refundStatus === 0 || r.refundStatus === 1">
            <el-button type="primary" size="small" @click="openReview(r)">处理</el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.size">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewVisible" title="退款审核" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <div v-if="currentRecord" class="review-info">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ currentRecord.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="原订单金额">¥{{ formatMoney(currentRecord.orderAmount ?? currentRecord.amount ?? 0) }}</el-descriptions-item>
          <el-descriptions-item label="申请退款金额"><span class="amount-text">¥{{ formatMoney(currentRecord.refundAmount ?? currentRecord.amount ?? 0) }}</span></el-descriptions-item>
          <el-descriptions-item label="退款比例">
            {{ (currentRecord.orderAmount ?? currentRecord.amount) ? Math.round((currentRecord.refundAmount ?? currentRecord.amount) / (currentRecord.orderAmount ?? currentRecord.amount) * 100) + '%' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ currentRecord.refundReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前状态"><StatusTag type="refund_status" :value="currentRecord.refundStatus" /></el-descriptions-item>
        </el-descriptions>
      </div>
      <el-divider />
      <el-form label-width="80px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.refundStatus">
            <el-radio :value="2">同意退款</el-radio>
            <el-radio :value="3">拒绝退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注" required>
          <el-input v-model="reviewForm.approveResult" type="textarea" :rows="3" placeholder="请输入处理备注（必填）" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false" :disabled="reviewing">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.filter-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.loading-wrap { padding: 24px 0; }
.error-card { border-radius: 8px; }
.table-wrap { overflow-x: auto; }
.no-action { color: #C0C4CC; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

.card-list { display: flex; flex-direction: column; gap: 12px; }
.refund-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.card-label { color: #909399; flex-shrink: 0; width: 56px; }
.card-value { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-value.amount { font-weight: 600; color: #F56C6C; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 4px; }

.amount-text { font-weight: 600; color: #F56C6C; }
</style>
