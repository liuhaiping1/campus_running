<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAppealList, handleAppeal } from '@/api/adminAppeal'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  appealStatus: undefined,
  page: 1,
  size: 10
})

const { windowWidth, isMobile } = useResponsive()

// 处理弹窗
const handleVisible = ref(false)
const handleForm = reactive({
  appealStatus: null,
  resultOrderStatus: undefined,
  responsibilityType: undefined,
  refundDecision: undefined,
  handleResult: ''
})
const handling = ref(false)
const currentRecord = ref(null)
const formRef = ref(null)

// 表单校验规则（成立时责任归属和退款决定必填）
const handleRules = {
  responsibilityType: [{ required: true, message: '请选择责任归属', trigger: 'change' }],
  refundDecision: [{ required: true, message: '请选择退款决定', trigger: 'change' }]
}

// 申诉类型映射
const appealTypeMap = { 1: '取消争议', 2: '履约争议', 3: '退款争议' }
function appealTypeLabel(val) { return appealTypeMap[val] || val || '-' }

/** 加载申诉列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = { page: query.page, size: query.size }
    if (query.appealStatus !== undefined && query.appealStatus !== '') params.appealStatus = query.appealStatus
    const result = await getAppealList(params)
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
function resetFilter() { query.appealStatus = undefined; query.page = 1; fetchRecords() }

/** 打开处理弹窗 */
function openHandle(record) {
  currentRecord.value = record
  handleForm.appealStatus = 2
  handleForm.resultOrderStatus = undefined
  handleForm.responsibilityType = undefined
  handleForm.refundDecision = undefined
  handleForm.handleResult = ''
  handleVisible.value = true
}

/** 提交申诉处理 */
async function submitHandle() {
  if (handleForm.appealStatus === null) {
    ElMessage.warning('请选择处理结果')
    return
  }
  if (!handleForm.handleResult.trim()) {
    ElMessage.warning('处理结果说明不能为空')
    return
  }
  const statusMap = { 2: '申诉成立', 3: '驳回申诉', 4: '关闭申诉' }
  const actionText = statusMap[handleForm.appealStatus] || '处理'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}吗？此操作不可撤销。`,
      `确认${actionText}`,
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: handleForm.appealStatus === 2 ? 'success' : 'warning' }
    )
  } catch { return }

  handling.value = true
  try {
    await handleAppeal(currentRecord.value.id, {
      appealStatus: handleForm.appealStatus,
      resultOrderStatus: handleForm.resultOrderStatus || undefined,
      responsibilityType: handleForm.responsibilityType || undefined,
      refundDecision: handleForm.refundDecision || undefined,
      handleResult: handleForm.handleResult
    })
    ElMessage.success(`${actionText}成功`)
    handleVisible.value = false
    fetchRecords()
  } catch {
    // 错误已在请求拦截器统一处理
  } finally {
    handling.value = false
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-appeals-page">
    <div class="page-header"><h2 class="page-title">申诉处理</h2></div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="query.appealStatus" placeholder="申诉状态" clearable style="width:140px" @change="handleFilterChange">
        <el-option label="待处理" :value="0" />
        <el-option label="处理中" :value="1" />
        <el-option label="已成立" :value="2" />
        <el-option label="已驳回" :value="3" />
        <el-option label="已关闭" :value="4" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 状态 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card"><el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty></el-card>
    <el-empty v-else-if="records.length === 0" description="暂无申诉记录" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="orderId" label="订单ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" width="150" show-overflow-tooltip />
          <el-table-column prop="appealType" label="申诉类型" width="110">
            <template #default="{ row }">{{ appealTypeLabel(row.appealType) }}</template>
          </el-table-column>
          <el-table-column prop="appealStatus" label="状态" width="100">
            <template #default="{ row }"><StatusTag type="appeal_status" :value="row.appealStatus" /></template>
          </el-table-column>
          <el-table-column prop="appealContent" label="申诉内容" min-width="160" show-overflow-tooltip />
          <el-table-column prop="createTime" label="提交时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.appealStatus === 0 || row.appealStatus === 1" type="primary" size="small" @click="openHandle(row)">处理</el-button>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="appeal-card">
          <div class="card-row"><span class="card-label">订单号</span><span class="card-value">{{ r.orderNo }}</span><StatusTag type="appeal_status" :value="r.appealStatus" size="small" /></div>
          <div class="card-row"><span class="card-label">类型</span><span class="card-value">{{ appealTypeLabel(r.appealType) }}</span></div>
          <div class="card-row" v-if="r.appealContent"><span class="card-label">内容</span><span class="card-value">{{ r.appealContent }}</span></div>
          <div class="card-row"><span class="card-label">时间</span><span class="card-value">{{ formatTime(r.createTime) }}</span></div>
          <div class="card-actions" v-if="r.appealStatus === 0 || r.appealStatus === 1">
            <el-button type="primary" size="small" @click="openHandle(r)">处理</el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.size">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理申诉" :width="isMobile ? '95%' : '550px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <div v-if="currentRecord" class="handle-info">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ currentRecord.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="申诉类型">{{ appealTypeLabel(currentRecord.appealType) }}</el-descriptions-item>
          <el-descriptions-item label="申诉内容">{{ currentRecord.appealContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="证据链接" v-if="currentRecord.evidenceUrls">{{ currentRecord.evidenceUrls }}</el-descriptions-item>
          <el-descriptions-item label="当前状态"><StatusTag type="appeal_status" :value="currentRecord.appealStatus" /></el-descriptions-item>
        </el-descriptions>
      </div>
      <el-divider />
      <el-form ref="formRef" :model="handleForm" :rules="handleRules" label-width="100px">
        <el-form-item label="处理结果" required>
          <el-radio-group v-model="handleForm.appealStatus">
            <el-radio :value="2">申诉成立</el-radio>
            <el-radio :value="3">驳回申诉</el-radio>
            <el-radio :value="4">关闭申诉</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结果订单状态">
          <el-select v-model="handleForm.resultOrderStatus" placeholder="可选（不改变原状态）" clearable style="width:100%">
            <el-option label="已完成" :value="7" />
            <el-option label="已取消" :value="8" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="handleForm.appealStatus === 2" label="责任归属" prop="responsibilityType">
          <el-select v-model="handleForm.responsibilityType" placeholder="请选择责任归属" clearable style="width:100%">
            <el-option label="发布人" :value="1" />
            <el-option label="跑腿员" :value="2" />
            <el-option label="平台" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="handleForm.appealStatus === 2" label="退款决定" prop="refundDecision">
          <el-select v-model="handleForm.refundDecision" placeholder="请选择退款决定" clearable style="width:100%">
            <el-option label="不退款" :value="0" />
            <el-option label="全额退款" :value="1" />
            <el-option label="部分退款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明" required>
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="4" placeholder="请输入处理结果说明（必填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false" :disabled="handling">取消</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">确认提交</el-button>
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
.appeal-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.card-label { color: #909399; flex-shrink: 0; width: 48px; }
.card-value { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 4px; }
</style>
