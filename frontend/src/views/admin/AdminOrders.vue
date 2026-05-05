<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAllOrders, getAdminOrderDetail } from '@/api/adminOrder'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'
import { actionLabel, orderStatusLabels } from '@/utils/constants'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  orderStatus: undefined,
  payStatus: undefined,
  settlementStatus: undefined,
  keyword: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 10
})

// 响应式
const { windowWidth, isMobile } = useResponsive()

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref(null)

async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.orderStatus !== undefined && query.orderStatus !== '') params.orderStatus = query.orderStatus
    if (query.payStatus !== undefined && query.payStatus !== '') params.payStatus = query.payStatus
    if (query.settlementStatus !== undefined && query.settlementStatus !== '') params.settlementStatus = query.settlementStatus
    if (query.keyword) params.keyword = query.keyword
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    const result = await getAllOrders(params)
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

function handlePageChange(page) {
  query.pageNum = page
  fetchRecords()
}

function handleFilterChange() {
  query.pageNum = 1
  fetchRecords()
}

function resetFilter() {
  query.orderStatus = undefined
  query.payStatus = undefined
  query.settlementStatus = undefined
  query.keyword = ''
  query.startTime = ''
  query.endTime = ''
  query.pageNum = 1
  fetchRecords()
}

async function viewDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await getAdminOrderDetail(row.id)
  } catch {
    currentDetail.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-orders-page">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width:130px" @change="handleFilterChange">
        <el-option v-for="(label, code) in orderStatusLabels" :key="code" :label="label" :value="Number(code)" />
      </el-select>
      <el-select v-model="query.payStatus" placeholder="支付状态" clearable style="width:120px" @change="handleFilterChange">
        <el-option label="未支付" :value="0" />
        <el-option label="支付中" :value="1" />
        <el-option label="已支付" :value="2" />
      </el-select>
      <el-select v-model="query.settlementStatus" placeholder="结算状态" clearable style="width:120px" @change="handleFilterChange">
        <el-option label="待结算" :value="0" />
        <el-option label="结算中" :value="1" />
        <el-option label="已结算" :value="2" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="关键词搜索" clearable style="width:180px" @change="handleFilterChange" />
      <el-date-picker v-model="query.startTime" type="datetime" placeholder="开始时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:190px" @change="handleFilterChange" />
      <el-date-picker v-model="query.endTime" type="datetime" placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:190px" @change="handleFilterChange" />
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载/错误/空 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card">
      <el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty>
    </el-card>
    <el-empty v-else-if="records.length === 0" description="暂无订单记录" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="orderNo" label="订单号" width="150" show-overflow-tooltip />
          <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column prop="orderStatus" label="订单状态" width="110">
            <template #default="{ row }"><StatusTag type="order_status" :value="row.orderStatus" /></template>
          </el-table-column>
          <el-table-column prop="payStatus" label="支付状态" width="100">
            <template #default="{ row }"><StatusTag type="pay_status" :value="row.payStatus" /></template>
          </el-table-column>
          <el-table-column prop="settlementStatus" label="结算状态" width="100">
            <template #default="{ row }"><StatusTag type="settlement_status" :value="row.settlementStatus" /></template>
          </el-table-column>
          <el-table-column prop="orderAmount" label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.orderAmount) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="order-card" @click="viewDetail(r)">
          <div class="card-row"><span class="card-label">订单号</span><span class="card-value">{{ r.orderNo }}</span><StatusTag type="order_status" :value="r.orderStatus" size="small" /></div>
          <div class="card-row"><span class="card-label">标题</span><span class="card-value">{{ r.title }}</span></div>
          <div class="card-row"><span class="card-label">分类</span><span class="card-value">{{ r.categoryName }} | ¥{{ formatMoney(r.orderAmount) }}</span></div>
          <div class="card-row"><span class="card-label">支付</span><StatusTag type="pay_status" :value="r.payStatus" size="small" /></div>
          <div class="card-row"><span class="card-label">时间</span><span class="card-value">{{ formatTime(r.createTime) }}</span></div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.pageSize">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" :width="isMobile ? '95%' : '700px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <div v-if="detailLoading" class="detail-loading"><el-skeleton :rows="4" animated /></div>
      <template v-else-if="currentDetail">
        <el-descriptions :column="isMobile ? 1 : 2" border size="small">
          <el-descriptions-item label="订单编号">{{ currentDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentDetail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="订单状态"><StatusTag type="order_status" :value="currentDetail.orderStatus" /></el-descriptions-item>
          <el-descriptions-item label="支付状态"><StatusTag type="pay_status" :value="currentDetail.payStatus" /></el-descriptions-item>
          <el-descriptions-item label="结算状态"><StatusTag type="settlement_status" :value="currentDetail.settlementStatus" /></el-descriptions-item>
          <el-descriptions-item label="取件地址">{{ currentDetail.pickupAddress }}</el-descriptions-item>
          <el-descriptions-item label="送达地址">{{ currentDetail.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="距离">{{ currentDetail.distanceKm }}km</el-descriptions-item>
          <el-descriptions-item label="订单金额"><span class="amount-text">¥{{ formatMoney(currentDetail.orderAmount) }}</span></el-descriptions-item>
          <el-descriptions-item label="发布人ID">{{ currentDetail.publisherId }}</el-descriptions-item>
          <el-descriptions-item label="跑腿员ID">{{ currentDetail.runnerId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ formatTime(currentDetail.deadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 费用明细 -->
        <el-card class="fee-card" v-if="currentDetail.baseFee != null">
          <template #header><span class="fee-title">费用明细</span></template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="基础费">¥{{ formatMoney(currentDetail.baseFee) }}</el-descriptions-item>
            <el-descriptions-item label="距离费">¥{{ formatMoney(currentDetail.distanceFee) }}</el-descriptions-item>
            <el-descriptions-item label="时段费">¥{{ formatMoney(currentDetail.timeFee) }}</el-descriptions-item>
            <el-descriptions-item label="小费">¥{{ formatMoney(currentDetail.tipFee) }}</el-descriptions-item>
            <el-descriptions-item label="平台佣金">¥{{ formatMoney(currentDetail.platformCommission) }}</el-descriptions-item>
            <el-descriptions-item label="跑腿员预估">¥{{ formatMoney(currentDetail.estimatedRunnerIncome) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 状态时间线 -->
        <div class="timeline-wrap" v-if="currentDetail.statusLogs && currentDetail.statusLogs.length > 0">
          <h4 class="section-subtitle">状态日志</h4>
          <el-timeline>
            <el-timeline-item v-for="log in currentDetail.statusLogs" :key="log.id" :timestamp="formatTime(log.createTime)" placement="top">
              <div class="timeline-row">
                <span>{{ actionLabel(log.triggerAction) }}</span>
                <StatusTag type="order_status" :value="log.afterStatus" size="small" style="margin-left:8px" />
              </div>
              <p class="timeline-remark" v-if="log.remark">{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
      <el-empty v-else description="加载详情失败" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.loading-wrap { padding: 24px 0; }
.error-card { border-radius: 8px; }
.table-wrap { overflow-x: auto; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 卡片 */
.card-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; cursor: pointer; display: flex; flex-direction: column; gap: 6px; }
.order-card:active { background: #f5f7fa; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.card-label { color: #909399; flex-shrink: 0; width: 48px; }
.card-value { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 详情 */
.detail-loading { padding: 20px 0; }
.amount-text { font-weight: 600; color: #F56C6C; }
.fee-card { margin-top: 16px; border-radius: 8px; }
.fee-title { font-size: 14px; font-weight: 600; }
.section-subtitle { font-size: 14px; font-weight: 600; color: #303133; margin: 16px 0 12px; }
.timeline-row { display: flex; align-items: center; }
.timeline-remark { margin: 4px 0 0; font-size: 13px; color: #909399; }

@media (max-width: 768px) {
  .filter-bar { gap: 8px; }
  .filter-bar > * { flex: 1; min-width: 0; }
  .filter-bar .el-button { flex: none; }
}
</style>
