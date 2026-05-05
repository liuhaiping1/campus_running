<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHallOrders, acceptOrder } from '@/api/order'
import { getCategoryList } from '@/api/system'
import OrderCard from '@/components/OrderCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { parseTotal } from '@/utils/format'

const loading = ref(false)
const accepting = ref(false)
const orders = ref([])
const categories = ref([])
const total = ref(0)

const query = reactive({
  categoryId: undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadCategories() {
  try {
    categories.value = await getCategoryList()
  } catch {
    categories.value = []
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.categoryId !== undefined) {
      params.categoryId = query.categoryId
    }
    const data = await getHallOrders(params)
    orders.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleCategoryChange(categoryId) {
  query.categoryId = categoryId
  query.pageNum = 1
  loadOrders()
}

// 接单（二次确认）
async function handleAccept(orderId) {
  try {
    await ElMessageBox.confirm(
      '确认接取该订单吗？接单后请及时联系发布人并完成配送。',
      '确认接单',
      { confirmButtonText: '确认接单', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  accepting.value = true
  try {
    await acceptOrder(orderId)
    ElMessage.success('接单成功，请及时联系发布人')
    await loadOrders()
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    accepting.value = false
  }
}

function handlePageChange(page) {
  query.pageNum = page
  loadOrders()
}

onMounted(() => {
  loadCategories()
  loadOrders()
})
</script>

<template>
  <div class="task-hall-page">
    <div class="page-header">
      <h2 class="page-title">任务大厅</h2>
      <p class="page-subtitle">查看可接订单，选择合适任务接单</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-row">
      <div class="category-filter" v-if="categories.length > 0">
        <el-button
          :type="!query.categoryId ? 'primary' : 'default'"
          size="default"
          @click="handleCategoryChange(undefined)"
        >
          全部
        </el-button>
        <el-button
          v-for="cat in categories"
          :key="cat.id"
          :type="query.categoryId === cat.id ? 'primary' : 'default'"
          size="default"
          @click="handleCategoryChange(cat.id)"
        >
          {{ cat.categoryName }}
        </el-button>
      </div>
      <div class="filter-right">
        <span class="total-count" v-if="total > 0">共 {{ total }} 条</span>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton v-for="i in 10" :key="i" :rows="3" animated style="margin-bottom: 16px" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && orders.length === 0"
      description="暂无可接订单，请稍后再来"
    />

    <!-- 订单卡片列表 -->
    <div v-else class="order-list">
      <OrderCard
        v-for="order in orders"
        :key="order.id"
        :order="order"
        mode="hall"
        :clickable="true"
        @accept="handleAccept"
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

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.total-count {
  font-size: 13px;
  color: #909399;
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
  .filter-row {
    flex-direction: column;
    gap: 10px;
  }

  .category-filter {
    gap: 6px;
  }

  .category-filter .el-button {
    font-size: 13px;
    padding: 6px 12px;
  }

  .filter-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
