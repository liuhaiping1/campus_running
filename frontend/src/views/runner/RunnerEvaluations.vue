<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRunnerEvaluations, getRunnerEvaluationSummary } from '@/api/runnerEvaluation'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime, parseTotal } from '@/utils/format'

const loading = ref(false)
const summary = ref(null)
const records = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  starScore: undefined
})

async function loadSummary() {
  try {
    summary.value = await getRunnerEvaluationSummary()
  } catch {
    summary.value = null
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.starScore !== undefined && query.starScore !== '') {
      params.starScore = query.starScore
    }
    const data = await getRunnerEvaluations(params)
    records.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleStarFilter(star) {
  query.starScore = star
  query.page = 1
  loadList()
}

function handlePageChange(page) {
  query.page = page
  loadList()
}

onMounted(() => {
  loadSummary()
  loadList()
})
</script>

<template>
  <div class="runner-evaluations-page">
    <div class="page-header">
      <h2 class="page-title">评价反馈</h2>
      <p class="page-subtitle">查看学生对我的评价</p>
    </div>

    <!-- 评价汇总卡片 -->
    <div class="summary-card" v-if="summary">
      <div class="summary-main">
        <span class="summary-score">{{ summary.averageScore ?? 0 }}</span>
        <span class="summary-unit">分</span>
      </div>
      <div class="summary-stars">
        <el-rate
          :model-value="Number(summary.averageScore ?? 0)"
          disabled
          show-score
          :texts="['', '', '', '', '']"
        />
      </div>
      <div class="summary-total">共 {{ summary.totalCount ?? 0 }} 条评价</div>
    </div>

    <!-- 汇总加载失败 -->
    <el-card v-else class="summary-empty">
      <el-empty description="评价汇总加载失败" :image-size="60" />
    </el-card>

    <!-- 星级筛选 -->
    <div class="star-filter">
      <el-button
        :type="!query.starScore ? 'primary' : 'default'"
        size="small"
        @click="handleStarFilter(undefined)"
      >
        全部
      </el-button>
      <el-button
        v-for="star in [5, 4, 3, 2, 1]"
        :key="star"
        :type="query.starScore === star ? 'primary' : 'default'"
        size="small"
        @click="handleStarFilter(star)"
      >
        {{ star }}星
      </el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && records.length === 0"
      description="暂无评价记录"
    />

    <!-- 评价列表 -->
    <div v-else class="evaluation-list">
      <div
        v-for="item in records"
        :key="item.id"
        class="evaluation-card"
      >
        <div class="eval-header">
          <el-rate :model-value="item.starScore" disabled size="small" />
          <span class="eval-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <p class="eval-content" v-if="item.content">{{ item.content }}</p>
        <p class="eval-content empty" v-else>用户未填写评价内容</p>
        <div class="eval-meta" v-if="item.orderNo">
          <router-link :to="`/runner/order/${item.orderId}`" class="order-link">
            订单 {{ item.orderNo }}
          </router-link>
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
.runner-evaluations-page {
  max-width: 680px;
  margin: 0 auto;
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

/* 汇总卡片 */
.summary-card {
  background: #fff;
  border-radius: 8px;
  padding: 32px 24px;
  border: 1px solid #e4e7ed;
  text-align: center;
  margin-bottom: 24px;
}

.summary-main {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.summary-score {
  font-size: 48px;
  font-weight: 700;
  color: #E6A23C;
  line-height: 1;
}

.summary-unit {
  font-size: 16px;
  color: #909399;
}

.summary-stars {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

.summary-total {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.summary-empty {
  margin-bottom: 24px;
}

/* 星级筛选 */
.star-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.loading-wrap {
  padding: 24px 0;
}

.evaluation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evaluation-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
}

.eval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eval-time {
  font-size: 12px;
  color: #909399;
}

.eval-content {
  margin: 10px 0 0;
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.eval-content.empty {
  color: #C0C4CC;
  font-style: italic;
}

.eval-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.order-link {
  color: #409EFF;
  text-decoration: none;
}

.order-link:hover {
  text-decoration: underline;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .runner-evaluations-page {
    padding: 0;
  }

  .summary-score {
    font-size: 40px;
  }
}
</style>
