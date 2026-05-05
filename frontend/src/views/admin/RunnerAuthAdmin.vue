<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthList, reviewAuth } from '@/api/adminRunnerAuth'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const reviewing = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  authStatus: undefined,
  page: 1,
  size: 10
})

const reviewVisible = ref(false)
const reviewForm = reactive({ authStatus: 1, rejectReason: '' })
const currentRecord = ref(null)

// 响应式：<=768px 为移动端，表格降级为卡片
const { windowWidth, isMobile } = useResponsive()

/** 加载认证审核列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.authStatus !== undefined && query.authStatus !== '') {
      params.authStatus = query.authStatus
    }
    const result = await getAuthList(params)
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

/** 分页变更 */
function handlePageChange(page) {
  query.page = page
  fetchRecords()
}

/** 筛选变更 */
function handleFilterChange() {
  query.page = 1
  fetchRecords()
}

/** 重置筛选 */
function resetFilter() {
  query.authStatus = undefined
  query.page = 1
  fetchRecords()
}

/** 打开审核弹窗 */
function openReview(record) {
  currentRecord.value = record
  reviewForm.authStatus = 1
  reviewForm.rejectReason = ''
  reviewVisible.value = true
}

/** 提交审核（二次确认） */
async function submitReview() {
  // 驳回必须填原因
  if (reviewForm.authStatus === 2 && !reviewForm.rejectReason.trim()) {
    ElMessage.warning('驳回申请必须填写驳回原因')
    return
  }

  // 二次确认
  const actionText = reviewForm.authStatus === 1 ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}该认证申请吗？${reviewForm.authStatus === 2 ? '驳回后申请人可重新提交。' : ''}`,
      `确认${actionText}`,
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: reviewForm.authStatus === 1 ? 'success' : 'warning' }
    )
  } catch {
    return
  }

  reviewing.value = true
  try {
    await reviewAuth(currentRecord.value.id, {
      authStatus: reviewForm.authStatus,
      rejectReason: reviewForm.rejectReason || undefined
    })
    ElMessage.success(reviewForm.authStatus === 1 ? '已通过审核' : '已驳回申请')
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
  <div class="runner-auth-admin">
    <div class="page-header">
      <h2 class="page-title">跑腿员认证审核</h2>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select
        v-model="query.authStatus"
        placeholder="审核状态"
        clearable
        style="width: 140px"
        @change="handleFilterChange"
      >
        <el-option label="待审核" :value="0" />
        <el-option label="审核通过" :value="1" />
        <el-option label="审核驳回" :value="2" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 请求失败 -->
    <el-card v-else-if="error" class="error-card">
      <el-empty description="数据加载失败，请稍后重试">
        <el-button type="primary" @click="fetchRecords">重新加载</el-button>
      </el-empty>
    </el-card>

    <!-- 空数据 -->
    <el-empty v-else-if="records.length === 0" description="暂无认证申请记录" />

    <!-- PC：表格 -->
    <template v-else>
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="authBatchNo" label="批次号" width="140" show-overflow-tooltip />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="schoolName" label="学校" min-width="120" show-overflow-tooltip />
          <el-table-column prop="campusName" label="校区" min-width="100" show-overflow-tooltip />
          <el-table-column prop="certNo" label="证件编号" width="140" />
          <el-table-column prop="authStatus" label="状态" width="100">
            <template #default="{ row }">
              <StatusTag type="auth_status" :value="row.authStatus" />
            </template>
          </el-table-column>
          <el-table-column prop="rejectReason" label="驳回原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.rejectReason || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.authStatus === 0"
                type="primary"
                size="small"
                @click="openReview(row)"
              >
                审核
              </el-button>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片列表 -->
      <div v-else class="card-list">
        <div v-for="record in records" :key="record.id" class="auth-card">
          <div class="card-row">
            <span class="card-label">批次号</span>
            <span class="card-value">{{ record.authBatchNo }}</span>
            <StatusTag type="auth_status" :value="record.authStatus" size="small" />
          </div>
          <div class="card-row">
            <span class="card-label">申请人</span>
            <span class="card-value">{{ record.realName }} / {{ record.phone }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">学校</span>
            <span class="card-value">{{ record.schoolName }} {{ record.campusName }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">证件编号</span>
            <span class="card-value">{{ record.certNo }}</span>
          </div>
          <div class="card-row" v-if="record.rejectReason">
            <span class="card-label">驳回原因</span>
            <span class="card-value reject">{{ record.rejectReason }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">申请时间</span>
            <span class="card-value">{{ formatTime(record.createTime) }}</span>
          </div>
          <div class="card-actions" v-if="record.authStatus === 0">
            <el-button type="primary" size="small" @click="openReview(record)">审核</el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.size">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="reviewVisible"
      title="审核认证申请"
      :width="isMobile ? '95%' : '500px'"
      :fullscreen="isMobile && windowWidth <= 480"
      destroy-on-close
    >
      <div v-if="currentRecord" class="review-info">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="申请人">{{ currentRecord.realName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRecord.phone }}</el-descriptions-item>
          <el-descriptions-item label="批次号">{{ currentRecord.authBatchNo }}</el-descriptions-item>
          <el-descriptions-item label="学校">{{ currentRecord.schoolName }} / {{ currentRecord.campusName }}</el-descriptions-item>
          <el-descriptions-item label="证件编号">{{ currentRecord.certNo }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider />

      <el-form label-width="80px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.authStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="驳回原因" v-if="reviewForm.authStatus === 2" required>
          <el-input
            v-model="reviewForm.rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回原因（如：证件照片不清晰、信息不完整等）"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewVisible = false" :disabled="reviewing">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">
          确认提交
        </el-button>
      </template>
    </el-dialog>
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
  margin: 0;
}

/* 筛选区 */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.loading-wrap {
  padding: 24px 0;
}

.error-card {
  border-radius: 8px;
}

/* 表格容器 */
.table-wrap {
  /* 表格在容器内可横向滚动，不溢出页面 */
  overflow-x: auto;
}

.no-action {
  color: #C0C4CC;
}

/* 分页：右对齐 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ---------- 移动端卡片 ---------- */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.card-label {
  color: #909399;
  flex-shrink: 0;
  width: 56px;
}

.card-value {
  color: #303133;
  flex: 1;
}

.card-value.reject {
  color: #F56C6C;
}

.card-actions {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
}

/* 审核信息区 */
.review-info {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-wrap: wrap;
  }
}
</style>
