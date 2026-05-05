<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { getOverview } from '@/api/adminStat'
import { formatMoney } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const error = ref(false)

/** 统计卡片配置：label + 后端字段名 + 格式化类型 */
const statCards = [
  { label: '总订单数', field: 'totalOrderCount', format: 'number' },
  { label: '今日订单数', field: 'todayOrderCount', format: 'number' },
  { label: '已支付金额', field: 'paidAmount', format: 'money' },
  { label: '退款金额', field: 'refundAmount', format: 'money' },
  { label: '活跃跑腿员', field: 'activeRunnerCount', format: 'number' },
  { label: '待审核认证', field: 'pendingRunnerAuthCount', format: 'number', link: '/admin/runner-auth' },
  { label: '待处理申诉', field: 'pendingAppealCount', format: 'number', link: '/admin/appeals' },
  { label: '待处理退款', field: 'pendingRefundCount', format: 'number', link: '/admin/refunds' }
]

const overview = ref(null)

async function loadOverview() {
  loading.value = true
  error.value = false
  try {
    overview.value = await getOverview()
  } catch {
    error.value = true
    overview.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOverview()
})
</script>

<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2 class="page-title">统计概览</h2>
      <el-button :icon="RefreshRight" @click="loadOverview" :loading="loading">刷新</el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="stat-grid">
      <div v-for="i in 8" :key="i" class="stat-card skeleton-card">
        <el-skeleton animated>
          <template #template>
            <div style="padding: 8px 0">
              <el-skeleton-item variant="text" style="width: 60%; height: 14px" />
              <el-skeleton-item variant="text" style="width: 40%; height: 28px; margin-top: 8px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 请求失败 -->
    <el-card v-else-if="error" class="error-card">
      <el-empty description="统计数据加载失败，请稍后重试">
        <el-button type="primary" @click="loadOverview">重新加载</el-button>
      </el-empty>
    </el-card>

    <!-- 正常数据：卡片网格 -->
    <div v-else class="stat-grid">
      <div v-for="card in statCards" :key="card.field" class="stat-card" :class="{ 'stat-card--clickable': card.link }" @click="card.link && router.push(card.link)">
        <span class="stat-label">{{ card.label }}</span>
        <span
          class="stat-value"
          :class="{ 'value-money': card.format === 'money' }"
        >
          <template v-if="card.format === 'money' && overview?.[card.field] != null">¥{{ formatMoney(overview?.[card.field]) }}</template>
          <template v-else-if="overview?.[card.field] != null">{{ overview?.[card.field] }}</template>
          <template v-else>-</template>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 统计卡片网格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px 20px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100px;
  justify-content: center;
}

.stat-card--clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.stat-card--clickable:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.skeleton-card {
  border-color: #f0f0f0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  /* 防止长金额溢出 */
  word-break: break-all;
}

.stat-value.value-money {
  color: #409EFF;
}

.error-card {
  border-radius: 8px;
}

/* 移动端：2列 */
@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 16px 12px;
    min-height: 80px;
    gap: 8px;
  }

  .stat-value {
    font-size: 22px;
  }
}
</style>
