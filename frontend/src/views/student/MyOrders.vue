<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders } from '@/api/order'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, formatMoney, parseTotal } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  orderStatus: undefined,
  payStatus: undefined,
  pageNum: 1,
  pageSize: 10
})

async function fetchOrders() {
  loading.value = true
  try {
    const params = {}
    if (query.orderStatus !== undefined && query.orderStatus !== '') {
      params.orderStatus = query.orderStatus
    }
    if (query.payStatus !== undefined && query.payStatus !== '') {
      params.payStatus = query.payStatus
    }
    params.pageNum = query.pageNum
    params.pageSize = query.pageSize
    const data = await getMyOrders(params)
    records.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  query.pageNum = 1
  fetchOrders()
}

function resetFilter() {
  query.orderStatus = undefined
  query.payStatus = undefined
  query.pageNum = 1
  fetchOrders()
}

function handlePageChange(page) {
  query.pageNum = page
  fetchOrders()
}

function goDetail(id) {
  router.push(`/order/${id}`)
}

onMounted(fetchOrders)
</script>

<template>
  <PageContainer title="我的订单" subtitle="查看我发布和接到的订单">
    <div class="orders-page">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="query.orderStatus" placeholder="订单状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option label="待支付" :value="0" />
          <el-option label="待接单" :value="1" />
          <el-option label="已接单" :value="2" />
          <el-option label="已联系用户" :value="3" />
          <el-option label="已取件" :value="4" />
          <el-option label="配送中" :value="5" />
          <el-option label="已送达" :value="6" />
          <el-option label="已完成" :value="7" />
          <el-option label="已取消" :value="8" />
        </el-select>
        <el-select v-model="query.payStatus" placeholder="支付状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option label="未支付" :value="0" />
          <el-option label="支付中" :value="1" />
          <el-option label="已支付" :value="2" />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="loading-area">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="records.length === 0"
        description="暂无订单"
        action-text="去发布订单"
        @action="router.push('/order/create')"
      />

      <!-- PC 端表格 + 移动端卡片（CSS 控制显示） -->
      <div v-else class="list-area">
        <div class="pc-only">
          <el-table :data="records" stripe style="width:100%">
            <el-table-column prop="orderNo" label="订单号" width="160" />
            <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
            <el-table-column label="订单状态" width="110">
              <template #default="{ row }">
                <StatusTag type="order_status" :value="row.orderStatus" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="90" align="right">
              <template #default="{ row }">{{ row.orderAmount != null ? '¥' + formatMoney(row.orderAmount) : '-' }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="goDetail(row.id)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card-list mobile-only">
          <div v-for="order in records" :key="order.id" class="order-card" @click="goDetail(order.id)">
            <div class="card-row">
              <span class="order-no">{{ order.orderNo }}</span>
              <StatusTag type="order_status" :value="order.orderStatus" size="small" />
            </div>
            <div class="card-row">
              <span class="order-title">{{ order.title }}</span>
            </div>
            <div class="card-row card-meta">
              <span>金额: {{ order.orderAmount != null ? '¥' + formatMoney(order.orderAmount) : '-' }}</span>
              <span>{{ formatTime(order.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.orders-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

/* 移动端卡片 */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}
.order-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-row:last-child {
  margin-bottom: 0;
}

.order-no {
  font-size: 13px;
  color: #909399;
}

.order-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-meta {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .pagination {
    justify-content: center;
  }
}
</style>
