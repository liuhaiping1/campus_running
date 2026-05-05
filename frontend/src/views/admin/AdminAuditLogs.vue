<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAuditLogs } from '@/api/adminAuditLog'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  module: '',
  action: '',
  operatorUserId: undefined,
  operatorRole: '',
  traceId: '',
  keyword: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 10
})

const { windowWidth, isMobile } = useResponsive()

/** 加载审计日志 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.module) params.module = query.module
    if (query.action) params.action = query.action
    if (query.operatorUserId !== undefined && query.operatorUserId !== '') params.operatorUserId = query.operatorUserId
    if (query.operatorRole) params.operatorRole = query.operatorRole
    if (query.traceId) params.traceId = query.traceId
    if (query.keyword) params.keyword = query.keyword
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    const data = await getAuditLogs(params)
    records.value = data.records || []
    total.value = parseTotal(data.total)
  } catch {
    error.value = true
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) { query.pageNum = page; fetchRecords() }
function handleSearch() { query.pageNum = 1; fetchRecords() }
function resetFilter() {
  query.module = ''
  query.action = ''
  query.operatorUserId = undefined
  query.operatorRole = ''
  query.traceId = ''
  query.keyword = ''
  query.startTime = ''
  query.endTime = ''
  query.pageNum = 1
  fetchRecords()
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-audit-logs-page">
    <div class="page-header">
      <h2 class="page-title">审计日志</h2>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-input v-model="query.module" placeholder="模块" clearable style="width:100px" @keyup.enter="handleSearch" />
      <el-input v-model="query.action" placeholder="动作" clearable style="width:100px" @keyup.enter="handleSearch" />
      <el-input v-model="query.operatorUserId" placeholder="操作人ID" clearable style="width:110px" @keyup.enter="handleSearch" />
      <el-input v-model="query.operatorRole" placeholder="操作角色" clearable style="width:110px" @keyup.enter="handleSearch" />
      <el-input v-model="query.traceId" placeholder="Trace ID" clearable style="width:130px" @keyup.enter="handleSearch" />
      <el-input v-model="query.keyword" placeholder="关键词" clearable style="width:130px" @keyup.enter="handleSearch" />
      <el-date-picker v-model="query.startTime" type="datetime" placeholder="开始时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:190px" @change="handleSearch" />
      <el-date-picker v-model="query.endTime" type="datetime" placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:190px" @change="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载/错误/空 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card"><el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty></el-card>
    <el-empty v-else-if="records.length === 0" description="暂无审计日志" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe size="small">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="90" />
          <el-table-column prop="action" label="动作" width="110" />
          <el-table-column prop="operationDesc" label="描述" min-width="140" show-overflow-tooltip />
          <el-table-column prop="requestMethod" label="请求方法" width="90" />
          <el-table-column prop="requestUri" label="请求路径" min-width="180" show-overflow-tooltip />
          <el-table-column prop="operatorUserId" label="操作人ID" width="100" />
          <el-table-column prop="operatorRole" label="角色" width="80" />
          <el-table-column prop="ipAddress" label="IP" width="130" />
          <el-table-column prop="success" label="结果" width="70">
            <template #default="{ row }">
              <StatusTag type="audit_success" :value="row.success" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="log-card">
          <div class="card-row">
            <span class="card-value title">{{ r.module }} / {{ r.action }}</span>
            <StatusTag type="audit_success" :value="r.success" size="small" />
          </div>
          <div class="card-row" v-if="r.operationDesc"><span class="card-value desc">{{ r.operationDesc }}</span></div>
          <div class="card-row">
            <span class="card-label">请求</span>
            <span class="card-value mono">{{ r.requestMethod }} {{ r.requestUri }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">操作人</span>
            <span class="card-value">{{ r.operatorUserId }} <template v-if="r.operatorRole">({{ r.operatorRole }})</template></span>
          </div>
          <div class="card-row">
            <span class="card-label">IP</span>
            <span class="card-value">{{ r.ipAddress || '-' }}</span>
          </div>
          <div class="card-row" v-if="r.errorMessage">
            <span class="card-label">错误</span>
            <span class="card-value error">{{ r.errorMessage }}</span>
          </div>
          <div class="card-row"><span class="card-value time">{{ formatTime(r.createTime) }}</span></div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.pageSize">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.filter-bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.loading-wrap { padding: 24px 0; }
.error-card { border-radius: 8px; }
.table-wrap { overflow-x: auto; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 卡片 */
.card-list { display: flex; flex-direction: column; gap: 12px; }
.log-card { background: #fff; border-radius: 8px; padding: 12px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 4px; }
.card-row { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.card-label { color: #909399; flex-shrink: 0; width: 44px; }
.card-value { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-value.title { font-weight: 600; font-size: 13px; white-space: normal; }
.card-value.desc { color: #606266; white-space: normal; }
.card-value.mono { font-family: monospace; font-size: 11px; }
.card-value.error { color: #F56C6C; white-space: normal; }
.card-value.time { color: #909399; }
</style>
