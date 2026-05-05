<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUserList, getAdminUserDetail, updateAdminUser, updateAdminUserStatus } from '@/api/adminUser'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)

const query = reactive({
  keyword: '',
  userStatus: undefined,
  roleCode: undefined,
  pageNum: 1,
  pageSize: 10
})

const { windowWidth, isMobile } = useResponsive()

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref(null)

// 编辑弹窗
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editUserId = ref(null)
const editForm = reactive({
  realName: '',
  nickName: '',
  phone: '',
  avatarUrl: '',
  gender: 0
})
const editRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  avatarUrl: [{ pattern: /^https?:\/\/.+/, message: '请输入http/https开头的URL', trigger: 'blur' }]
}

/** 加载用户列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.userStatus !== undefined && query.userStatus !== '') params.userStatus = query.userStatus
    if (query.roleCode) params.roleCode = query.roleCode
    const result = await getAdminUserList(params)
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

function handlePageChange(page) {
  query.pageNum = page
  fetchRecords()
}

function handleFilterChange() {
  query.pageNum = 1
  fetchRecords()
}

function resetFilter() {
  query.keyword = ''
  query.userStatus = undefined
  query.roleCode = undefined
  query.pageNum = 1
  fetchRecords()
}

/** 查看详情 */
async function viewDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await getAdminUserDetail(row.id)
  } catch {
    currentDetail.value = null
  } finally {
    detailLoading.value = false
  }
}

/** 打开编辑弹窗 */
async function openEdit(row) {
  editUserId.value = row.id
  editForm.realName = row.realName || ''
  editForm.nickName = row.nickName || ''
  editForm.phone = row.phone || ''
  editForm.avatarUrl = row.avatarUrl || ''
  editForm.gender = row.gender ?? 0
  editVisible.value = true
}

/** 提交编辑 */
async function handleEditSubmit() {
  if (editLoading.value) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  editLoading.value = true
  try {
    const data = { ...editForm }
    if (!data.avatarUrl) delete data.avatarUrl
    await updateAdminUser(editUserId.value, data)
    ElMessage.success('用户资料修改成功')
    editVisible.value = false
    fetchRecords()
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    editLoading.value = false
  }
}

/** 启用/禁用用户 */
async function toggleStatus(row) {
  const newStatus = row.userStatus === 1 ? 2 : 1
  const action = newStatus === 2 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.realName || row.username}」？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await updateAdminUserStatus(row.id, { userStatus: newStatus })
    ElMessage.success(`用户已${action}`)
    fetchRecords()
  } catch {
    // 错误已在 request 拦截器中统一处理
  }
}

/** 角色文本 */
function roleText(code) {
  const map = { STUDENT: '学生', RUNNER: '跑腿员', ADMIN: '管理员' }
  return map[code] ?? code
}

/** 性别文本 */
function genderText(val) {
  const map = { 0: '未知', 1: '男', 2: '女' }
  return map[val] ?? '未知'
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-users-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="用户名/姓名/手机" clearable style="width:180px" @change="handleFilterChange" />
      <el-select v-model="query.userStatus" placeholder="用户状态" clearable style="width:120px" @change="handleFilterChange">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="2" />
      </el-select>
      <el-select v-model="query.roleCode" placeholder="用户角色" clearable style="width:120px" @change="handleFilterChange">
        <el-option label="学生" value="STUDENT" />
        <el-option label="跑腿员" value="RUNNER" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载/错误/空 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card">
      <el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty>
    </el-card>
    <el-empty v-else-if="records.length === 0" description="暂无用户记录" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
          <el-table-column label="姓名/昵称" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.realName || row.nickName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag v-for="r in (row.roles || [])" :key="r" size="small" style="margin-right:4px">{{ roleText(r) }}</el-tag>
              <span v-if="!row.roles || row.roles.length === 0">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <StatusTag type="user_status" :value="row.userStatus" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="最近登录" width="170">
            <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                :type="row.userStatus === 1 ? 'danger' : 'success'"
                link size="small"
                @click="toggleStatus(row)"
              >
                {{ row.userStatus === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="user-card">
          <div class="card-row" @click="viewDetail(r)">
            <span class="card-label">用户名</span>
            <span class="card-value">{{ r.username }}</span>
            <StatusTag type="user_status" :value="r.userStatus" size="small" />
          </div>
          <div class="card-row">
            <span class="card-label">姓名</span>
            <span class="card-value">{{ r.realName || r.nickName || '-' }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">手机</span>
            <span class="card-value">{{ r.phone || '-' }}</span>
          </div>
          <div class="card-row">
            <span class="card-label">角色</span>
            <span class="card-value">
              <el-tag v-for="role in (r.roles || [])" :key="role" size="small" style="margin-right:4px">{{ roleText(role) }}</el-tag>
              <span v-if="!r.roles || r.roles.length === 0">-</span>
            </span>
          </div>
          <div class="card-row">
            <span class="card-label">注册</span>
            <span class="card-value">{{ formatTime(r.createTime) }}</span>
          </div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="viewDetail(r)">查看</el-button>
            <el-button type="primary" size="small" @click="openEdit(r)">编辑</el-button>
            <el-button :type="r.userStatus === 1 ? 'danger' : 'success'" size="small" @click="toggleStatus(r)">
              {{ r.userStatus === 1 ? '禁用' : '启用' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.pageSize">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" :width="isMobile ? '95%' : '700px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <div v-if="detailLoading" class="detail-loading"><el-skeleton :rows="4" animated /></div>
      <template v-else-if="currentDetail">
        <el-descriptions :column="isMobile ? 1 : 2" border size="small">
          <el-descriptions-item label="用户ID">{{ currentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ currentDetail.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentDetail.nickName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentDetail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(currentDetail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="头像地址">{{ currentDetail.avatarUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户状态">
            <StatusTag type="user_status" :value="currentDetail.userStatus" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag v-for="r in (currentDetail.roles || [])" :key="r" size="small" style="margin-right:4px">{{ roleText(r) }}</el-tag>
            <span v-if="!currentDetail.roles || currentDetail.roles.length === 0">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ formatTime(currentDetail.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(currentDetail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="加载详情失败" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑用户资料"
      :width="isMobile ? '95%' : '500px'"
      :close-on-click-modal="false"
      destroy-on-close
      @close="editFormRef?.resetFields()"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="editForm.nickName" placeholder="请输入昵称" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="头像地址" prop="avatarUrl">
          <el-input v-model="editForm.avatarUrl" placeholder="请输入头像图片URL" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.loading-wrap { padding: 24px 0; }
.error-card { border-radius: 8px; }
.table-wrap { overflow-x: auto; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 卡片 */
.card-list { display: flex; flex-direction: column; gap: 12px; }
.user-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 6px; }
.user-card:active { background: #f5f7fa; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
.card-label { color: #909399; flex-shrink: 0; width: 48px; }
.card-value { color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-actions { display: flex; gap: 8px; margin-top: 4px; }

/* 详情 */
.detail-loading { padding: 20px 0; }

@media (max-width: 768px) {
  .filter-bar { gap: 8px; }
  .filter-bar > * { flex: 1; min-width: 0; }
  .filter-bar .el-button { flex: none; }
}
</style>
