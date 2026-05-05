<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminNoticeList, createNotice, updateNotice, changeNoticeStatus } from '@/api/adminNotice'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)
const submitting = ref(false)

const query = reactive({
  noticeStatus: undefined,
  noticeType: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 响应式
const { windowWidth, isMobile } = useResponsive()

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  noticeTitle: '',
  noticeContent: '',
  noticeType: 1,
  noticeStatus: 0
})

const form = reactive(defaultForm())

const rules = {
  noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeContent: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  noticeStatus: [{ required: true, message: '请选择公告状态', trigger: 'change' }]
}

/** 加载公告列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.noticeStatus !== undefined && query.noticeStatus !== '') params.noticeStatus = query.noticeStatus
    if (query.noticeType !== undefined && query.noticeType !== '') params.noticeType = query.noticeType
    if (query.keyword) params.keyword = query.keyword
    const data = await getAdminNoticeList(params)
    // NoticePageVO 不是标准 IPage，兼容 records / list
    records.value = data.records || data.list || []
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
function resetFilter() { query.noticeStatus = undefined; query.noticeType = undefined; query.keyword = ''; query.pageNum = 1; fetchRecords() }

/** 打开新增弹窗 */
function openCreate() {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(row) {
  isEdit.value = true
  currentId.value = row.id
  form.noticeTitle = row.noticeTitle || ''
  form.noticeContent = row.noticeContent || ''
  form.noticeType = row.noticeType ?? 1
  form.noticeStatus = row.noticeStatus ?? 0
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  if (submitting.value) return
  try { await formRef.value.validate() } catch { return }
  // 校验类型和状态值合法
  if (![1, 2, 3].includes(form.noticeType)) { ElMessage.warning('公告类型不合法'); return }
  if (![0, 1, 2].includes(form.noticeStatus)) { ElMessage.warning('公告状态不合法'); return }

  const data = {
    noticeTitle: form.noticeTitle,
    noticeContent: form.noticeContent,
    noticeType: form.noticeType,
    noticeStatus: form.noticeStatus
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateNotice(currentId.value, data)
      ElMessage.success('修改成功')
    } else {
      await createNotice(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchRecords()
  } catch {
    // 错误已在请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/** 变更公告状态（发布/下架/改草稿） */
async function handleStatusChange(row, newStatus) {
  const statusMap = { 0: '改为草稿', 1: '发布公告', 2: '下架公告' }
  const actionText = statusMap[newStatus] || '变更状态'
  const confirmType = newStatus === 2 ? 'warning' : newStatus === 1 ? 'success' : 'info'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}「${row.noticeTitle}」吗？`,
      `确认${actionText}`,
      { confirmButtonText: '确认', cancelButtonText: '取消', type: confirmType }
    )
  } catch { return }

  try {
    await changeNoticeStatus(row.id, { noticeStatus: newStatus })
    ElMessage.success(`${actionText}成功`)
    fetchRecords()
  } catch {
    // 错误已在请求拦截器统一处理
  }
}

/** 根据当前状态渲染操作按钮 */
function statusActions(row) {
  const actions = []
  if (row.noticeStatus === 0) {
    // 草稿 → 可发布
    actions.push({ label: '发布', status: 1, type: 'success' })
  } else if (row.noticeStatus === 1) {
    // 已发布 → 可下架、改草稿
    actions.push({ label: '下架', status: 2, type: 'warning' })
    actions.push({ label: '改草稿', status: 0, type: 'info' })
  } else if (row.noticeStatus === 2) {
    // 已下架 → 可重新发布
    actions.push({ label: '重新发布', status: 1, type: 'success' })
  }
  return actions
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-notices-page">
    <div class="page-header">
      <h2 class="page-title">公告管理</h2>
      <el-button type="primary" @click="openCreate">新增公告</el-button>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select v-model="query.noticeStatus" placeholder="公告状态" clearable style="width:130px" @change="handleSearch">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已下架" :value="2" />
      </el-select>
      <el-select v-model="query.noticeType" placeholder="公告类型" clearable style="width:130px" @change="handleSearch">
        <el-option label="普通" :value="1" />
        <el-option label="重要" :value="2" />
        <el-option label="维护" :value="3" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="关键词搜索" clearable style="width:180px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载/错误/空 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card"><el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty></el-card>
    <el-empty v-else-if="records.length === 0" description="暂无公告数据" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="noticeTitle" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="noticeType" label="类型" width="80">
            <template #default="{ row }"><StatusTag type="notice_type" :value="row.noticeType" /></template>
          </el-table-column>
          <el-table-column prop="noticeStatus" label="状态" width="90">
            <template #default="{ row }"><StatusTag type="notice_status" :value="row.noticeStatus" /></template>
          </el-table-column>
          <el-table-column prop="publishTime" label="发布时间" width="170">
            <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                v-for="act in statusActions(row)"
                :key="act.status"
                :type="act.type"
                link
                size="small"
                @click="handleStatusChange(row, act.status)"
              >
                {{ act.label }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="notice-card">
          <div class="card-row">
            <span class="card-value title">{{ r.noticeTitle }}</span>
            <StatusTag type="notice_status" :value="r.noticeStatus" size="small" />
          </div>
          <div class="card-row">
            <span class="card-label">类型</span>
            <StatusTag type="notice_type" :value="r.noticeType" size="small" />
          </div>
          <div class="card-row" v-if="r.publishTime"><span class="card-label">发布</span><span class="card-value">{{ formatTime(r.publishTime) }}</span></div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="openEdit(r)">编辑</el-button>
            <el-button
              v-for="act in statusActions(r)"
              :key="act.status"
              :type="act.type"
              size="small"
              @click="handleStatusChange(r, act.status)"
            >
              {{ act.label }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.pageSize">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" :width="isMobile ? '95%' : '600px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="top">
        <el-form-item label="公告标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" maxlength="100" />
        </el-form-item>

        <el-form-item label="公告类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio :value="1">普通</el-radio>
            <el-radio :value="2">重要</el-radio>
            <el-radio :value="3">维护</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="公告状态" prop="noticeStatus">
          <el-radio-group v-model="form.noticeStatus">
            <el-radio :value="0">草稿</el-radio>
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="2">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="公告内容" prop="noticeContent">
          <el-input v-model="form.noticeContent" type="textarea" :rows="6" placeholder="请输入公告内容" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false" :disabled="submitting">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.loading-wrap { padding: 24px 0; }
.error-card { border-radius: 8px; }
.table-wrap { overflow-x: auto; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 卡片 */
.card-list { display: flex; flex-direction: column; gap: 12px; }
.notice-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.card-label { color: #909399; flex-shrink: 0; width: 40px; }
.card-value { color: #303133; flex: 1; }
.card-value.title { font-weight: 600; font-size: 14px; }
.card-actions { display: flex; justify-content: flex-end; gap: 6px; margin-top: 4px; flex-wrap: wrap; }
</style>
