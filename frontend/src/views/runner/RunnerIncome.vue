<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getIncomeOverview, getIncomeList } from '@/api/runnerIncome'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatMoney, formatTime, parseTotal } from '@/utils/format'

const loading = ref(false)
const overview = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  settlementStatus: ''
})

async function loadOverview() {
  try {
    overview.value = await getIncomeOverview()
  } catch {
    overview.value = null
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.settlementStatus !== undefined && query.settlementStatus !== '') {
      params.settlementStatus = query.settlementStatus
    }
    const data = await getIncomeList(params)
    records.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSettlementChange() {
  query.page = 1
  loadList()
}

function handlePageChange(page) {
  query.page = page
  loadList()
}

onMounted(() => {
  loadOverview()
  loadList()
})
</script>

<template>
  <div class="runner-income-page">
    <div class="page-header">
      <h2 class="page-title">收益统计</h2>
      <p class="page-subtitle">查看跑腿收入概览和明细</p>
    </div>

    <!-- 收益概览卡片 -->
    <div class="overview-grid" v-if="overview">
      <div class="overview-card">
        <span class="overview-label">累计收益</span>
        <span class="overview-value primary">¥{{ formatMoney(overview.totalIncome) }}</span>
      </div>
      <div class="overview-card overview-card--clickable" @click="query.settlementStatus = 0; loadList()">
        <span class="overview-label">待结算</span>
        <span class="overview-value warning">¥{{ formatMoney(overview.pendingIncome) }}</span>
      </div>
      <div class="overview-card overview-card--clickable" @click="query.settlementStatus = 2; loadList()">
        <span class="overview-label">已结算</span>
        <span class="overview-value success">¥{{ formatMoney(overview.settledIncome) }}</span>
      </div>
      <div class="overview-card">
        <span class="overview-label">完成订单</span>
        <span class="overview-value">{{ overview.totalOrderCount ?? 0 }}单</span>
      </div>
    </div>

    <!-- 概览加载失败 -->
    <el-card v-else class="overview-empty">
      <el-empty description="收益概览加载失败" :image-size="80" />
    </el-card>

    <!-- 收益明细 -->
    <div class="list-header">
      <h3 class="section-title">收益明细</h3>
      <el-select
        v-model="query.settlementStatus"
        placeholder="全部状态"
        clearable
        style="width: 130px"
        @change="handleSettlementChange"
      >
        <el-option label="全部" value="" />
        <el-option label="待结算" :value="0" />
        <el-option label="结算中" :value="1" />
        <el-option label="已结算" :value="2" />
      </el-select>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && records.length === 0"
      description="暂无收益记录"
    />

    <!-- 收益列表 -->
    <div v-else class="income-list">
      <div
        v-for="item in records"
        :key="item.id"
        class="income-card"
      >
        <div class="income-row">
          <div class="income-info">
            <span class="income-order-no">订单 {{ item.orderNo || ('#' + item.orderId) }}</span>
            <StatusTag type="settlement_status" :value="item.settlementStatus" size="small" />
          </div>
          <span class="income-amount" :class="item.settlementStatus === 3 ? 'amount-deduct' : 'amount-income'">
            {{ item.settlementStatus === 3 ? '-' : '+' }}¥{{ formatMoney(item.incomeAmount) }}
          </span>
        </div>
        <div class="income-meta">
          <span>{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > query.size">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 概览卡片网格 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 16px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  text-align: center;
}

.overview-card--clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.overview-card--clickable:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.overview-label {
  font-size: 13px;
  color: #909399;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.overview-value.primary {
  color: #409EFF;
}

.overview-value.warning {
  color: #E6A23C;
}

.overview-value.success {
  color: #67C23A;
}

.overview-empty {
  margin-bottom: 24px;
}

/* 明细区域 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.loading-wrap {
  padding: 24px 0;
}

.income-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.income-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
}

.income-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.income-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.income-order-no {
  font-size: 14px;
  color: #606266;
}

.income-amount {
  font-size: 16px;
  font-weight: 600;
}

.amount-income {
  color: #67C23A;
}

.amount-deduct {
  color: #F56C6C;
}

.income-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .overview-card {
    padding: 16px 12px;
  }

  .overview-value {
    font-size: 18px;
  }
}
</style>
