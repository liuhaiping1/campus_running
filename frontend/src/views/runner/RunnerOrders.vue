<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyOrders } from '@/api/order'
import OrderCard from '@/components/OrderCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { parseTotal } from '@/utils/format'

const loading = ref(false)
const orders = ref([])
const total = ref(0)

const query = reactive({
  orderStatus: '',
  payStatus: undefined,
  pageNum: 1,
  pageSize: 10
})

// 订单状态筛选选项
const statusOptions = [
  { value: '', label: '全部' },
  { value: 2, label: '已接单' },
  { value: 3, label: '已联系' },
  { value: 4, label: '已取件' },
  { value: 5, label: '配送中' },
  { value: 6, label: '已送达' },
  { value: 7, label: '已完成' },
  { value: 8, label: '已取消' }
]

async function loadOrders() {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.orderStatus !== undefined && query.orderStatus !== '') {
      params.orderStatus = query.orderStatus
    }
    if (query.payStatus !== undefined && query.payStatus !== '') {
      params.payStatus = query.payStatus
    }
    const data = await getMyOrders(params)
    // 后端 GET /api/order 已按当前用户过滤
    orders.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  query.pageNum = 1
  loadOrders()
}

function handlePageChange(page) {
  query.pageNum = page
  loadOrders()
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <div class="runner-orders-page">
    <div class="page-header">
      <h2 class="page-title">我的接单</h2>
      <p class="page-subtitle">查看已接订单和履约进度</p>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <div class="filter-row">
        <span class="filter-label">订单状态</span>
        <el-select
          v-model="query.orderStatus"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="handleStatusChange"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.label"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton v-for="i in 10" :key="i" :rows="3" animated style="margin-bottom: 16px" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && orders.length === 0"
      description="暂无接单记录"
      action-text="去任务大厅接单"
      @action="$router.push('/runner/hall')"
    />

    <!-- 订单卡片列表 -->
    <div v-else class="order-list">
      <OrderCard
        v-for="order in orders"
        :key="order.id"
        :order="order"
        mode="runner"
        :clickable="true"
      />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > query.pageSize">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.runner-orders-page {
  
}

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

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.loading-wrap {
  padding: 24px 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
