<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/adminCategory'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const error = ref(false)
const submitting = ref(false)

const query = reactive({
  categoryStatus: undefined,
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
  categoryName: '',
  categoryCode: '',
  baseFee: 0,
  distanceFeeRule: '',
  weightFeeRule: '',
  timeFeeRule: '',
  urgentFee: 0,
  feeRuleVersion: 'v1',
  sortNo: 0,
  categoryStatus: 1
})

const form = reactive(defaultForm())

function validateJson(rule, value, callback) {
  if (!value) return callback()
  try {
    JSON.parse(value)
    callback()
  } catch (e) {
    callback(new Error('JSON 格式不正确: ' + e.message))
  }
}

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  baseFee: [{ required: true, message: '请输入基础费用', trigger: 'blur' }],
  distanceFeeRule: [{ validator: validateJson, trigger: 'blur' }],
  weightFeeRule: [{ validator: validateJson, trigger: 'blur' }],
  timeFeeRule: [{ validator: validateJson, trigger: 'blur' }]
}

function formatJson(field) {
  try {
    form[field] = JSON.stringify(JSON.parse(form[field]), null, 2)
  } catch {}
}

// 示例 JSON
const distExample = JSON.stringify([{ min: 0, max: 3, fee: 0 }, { min: 3, max: null, fee: 1 }])
const weightExample = JSON.stringify([{ min: 0, max: 5, fee: 0 }, { min: 5, max: null, fee: 2 }])
const timeExample = JSON.stringify([{ code: 'normal', start: '08:00', end: '22:00', fee: 0 }, { code: 'night', start: '22:00', end: '08:00', fee: 2 }])

/** 加载分类列表 */
async function fetchRecords() {
  loading.value = true
  error.value = false
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.categoryStatus !== undefined && query.categoryStatus !== '') params.categoryStatus = query.categoryStatus
    if (query.keyword) params.keyword = query.keyword
    const result = await getAdminCategoryList(params)
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

function handlePageChange(page) { query.pageNum = page; fetchRecords() }
function handleSearch() { query.pageNum = 1; fetchRecords() }
function resetFilter() { query.categoryStatus = undefined; query.keyword = ''; query.pageNum = 1; fetchRecords() }

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
  form.categoryName = row.categoryName || ''
  form.categoryCode = row.categoryCode || ''
  form.baseFee = row.baseFee ?? 0
  // 后端返回的是 JSON 结构，需要转成字符串才能放入 textarea
  form.distanceFeeRule = typeof row.distanceFeeRule === 'string' ? row.distanceFeeRule : JSON.stringify(row.distanceFeeRule ?? [], null, 2)
  form.weightFeeRule = typeof row.weightFeeRule === 'string' ? row.weightFeeRule : JSON.stringify(row.weightFeeRule ?? [], null, 2)
  form.timeFeeRule = typeof row.timeFeeRule === 'string' ? row.timeFeeRule : JSON.stringify(row.timeFeeRule ?? [], null, 2)
  form.urgentFee = row.urgentFee ?? 0
  form.feeRuleVersion = row.feeRuleVersion || 'v1'
  form.sortNo = row.sortNo ?? 0
  form.categoryStatus = row.categoryStatus ?? 1
  dialogVisible.value = true
}

/** 校验 JSON 字符串 */
function validateJsonField(val, fieldName) {
  if (val == null || (typeof val === 'string' && !val.trim())) return true
  // 防御：如果 value 不是字符串（如后端直接返回的对象），先序列化
  const str = typeof val === 'string' ? val : JSON.stringify(val)
  if (!str.trim()) return true
  try {
    const parsed = JSON.parse(str)
    if (!Array.isArray(parsed)) {
      ElMessage.warning(`${fieldName} 必须是 JSON 数组格式`)
      return false
    }
    return true
  } catch {
    ElMessage.warning(`${fieldName} JSON 格式不正确`)
    return false
  }
}

/** 提交表单 */
async function handleSubmit() {
  if (submitting.value) return
  try {
    await formRef.value.validate()
  } catch { return }

  // JSON 规则字段基础校验
  if (!validateJsonField(form.distanceFeeRule, '距离计费规则')) return
  if (!validateJsonField(form.weightFeeRule, '重量计费规则')) return
  if (!validateJsonField(form.timeFeeRule, '时段计费规则')) return

  // 构建请求体，JSON 字段直接传字符串
  const data = {
    categoryName: form.categoryName,
    categoryCode: form.categoryCode,
    baseFee: form.baseFee,
    distanceFeeRule: form.distanceFeeRule || null,
    weightFeeRule: form.weightFeeRule || null,
    timeFeeRule: form.timeFeeRule || null,
    urgentFee: form.urgentFee,
    feeRuleVersion: form.feeRuleVersion,
    sortNo: form.sortNo,
    categoryStatus: form.categoryStatus
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(currentId.value, data)
      ElMessage.success('修改成功')
    } else {
      await createCategory(data)
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

/** 删除分类 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.categoryName}」吗？删除后将隐藏该分类，已有关联订单不受影响。`,
      '确认删除',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch {
    // 错误已在请求拦截器统一处理
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<template>
  <div class="admin-categories-page">
    <div class="page-header">
      <h2 class="page-title">分类管理</h2>
      <el-button type="primary" @click="openCreate">新增分类</el-button>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select v-model="query.categoryStatus" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="关键词搜索" clearable style="width:180px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 加载/错误/空 -->
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" animated /></div>
    <el-card v-else-if="error" class="error-card"><el-empty description="数据加载失败"><el-button type="primary" @click="fetchRecords">重新加载</el-button></el-empty></el-card>
    <el-empty v-else-if="records.length === 0" description="暂无分类数据" />

    <template v-else>
      <!-- PC：表格 -->
      <div v-if="!isMobile" class="table-wrap">
        <el-table :data="records" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="categoryName" label="分类名称" width="130" />
          <el-table-column prop="categoryCode" label="编码" width="110" />
          <el-table-column prop="baseFee" label="基础费用" width="100" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.baseFee) }}</template>
          </el-table-column>
          <el-table-column prop="categoryStatus" label="状态" width="80">
            <template #default="{ row }"><StatusTag type="category_status" :value="row.categoryStatus" /></template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序" width="70" />
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端：卡片 -->
      <div v-else class="card-list">
        <div v-for="r in records" :key="r.id" class="cat-card">
          <div class="card-row"><span class="card-label">名称</span><span class="card-value">{{ r.categoryName }}</span><StatusTag type="category_status" :value="r.categoryStatus" size="small" /></div>
          <div class="card-row"><span class="card-label">编码</span><span class="card-value">{{ r.categoryCode }}</span></div>
          <div class="card-row"><span class="card-label">费用</span><span class="card-value amount">¥{{ formatMoney(r.baseFee) }}</span></div>
          <div class="card-row"><span class="card-label">排序</span><span class="card-value">{{ r.sortNo }}</span></div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="openEdit(r)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(r)">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > query.pageSize">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="handlePageChange" />
      </div>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" :width="isMobile ? '95%' : '650px'" :fullscreen="isMobile && windowWidth <= 480" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="form.categoryName" placeholder="如：快递代取" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input v-model="form.categoryCode" placeholder="如：EXPRESS" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="基础费用" prop="baseFee">
              <el-input-number v-model="form.baseFee" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="加急费用">
              <el-input-number v-model="form.urgentFee" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortNo" :min="0" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.categoryStatus">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计费版本">
              <el-input v-model="form.feeRuleVersion" placeholder="如：v1" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 距离计费规则 -->
        <el-form-item label="距离计费规则">
          <div class="json-header">
            <el-button size="small" @click="form.distanceFeeRule = distExample">填入示例</el-button>
            <el-button size="small" @click="formatJson('distanceFeeRule')">格式化</el-button>
          </div>
          <el-input v-model="form.distanceFeeRule" type="textarea" :rows="3" placeholder='[{"min":0,"max":3,"fee":0}]' style="font-family:monospace" />
        </el-form-item>

        <!-- 重量计费规则 -->
        <el-form-item label="重量计费规则">
          <div class="json-header">
            <el-button size="small" @click="form.weightFeeRule = weightExample">填入示例</el-button>
            <el-button size="small" @click="formatJson('weightFeeRule')">格式化</el-button>
          </div>
          <el-input v-model="form.weightFeeRule" type="textarea" :rows="3" placeholder='[{"min":0,"max":5,"fee":0}]' style="font-family:monospace" />
        </el-form-item>

        <!-- 时段计费规则 -->
        <el-form-item label="时段计费规则">
          <div class="json-header">
            <el-button size="small" @click="form.timeFeeRule = timeExample">填入示例</el-button>
            <el-button size="small" @click="formatJson('timeFeeRule')">格式化</el-button>
          </div>
          <el-input v-model="form.timeFeeRule" type="textarea" :rows="3" placeholder='[{"code":"normal","start":"08:00","end":"22:00","fee":0}]' style="font-family:monospace" />
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
.cat-card { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid #e4e7ed; display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.card-label { color: #909399; flex-shrink: 0; width: 40px; }
.card-value { color: #303133; flex: 1; }
.card-value.amount { font-weight: 600; color: #E6A23C; }
.card-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }

/* 弹窗 */
.json-header { display: flex; justify-content: flex-end; margin-bottom: 4px; }
</style>
