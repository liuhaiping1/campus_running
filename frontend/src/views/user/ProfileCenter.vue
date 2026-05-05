<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Edit, Lock, List, MapLocation, Bell, Tickets, Document, Money, Star, Medal, Camera } from '@element-plus/icons-vue'
import { getUserProfile, updateUserProfile, changePassword, getUserCenterOverview, getRunnerCenterOverview } from '@/api/userProfile'
import { useUserStore } from '@/stores/user'
import { useFileUpload } from '@/composables/useFileUpload'
import { useResponsive } from '@/composables/useResponsive'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatMoney, formatTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const AUTH_STATUS_REJECTED = 2

const pageLoading = ref(false)
const profile = ref(null)
const studentOverview = ref(null)
const runnerOverview = ref(null)

const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)

/** 头像上传（composable 封装校验、上传、loading 状态） */
const { uploading: avatarUploading, handleUpload: handleAvatarUpload } = useFileUpload({
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  typeErrorMsg: '头像仅支持 JPG、PNG、WebP 格式',
  sizeErrorMsg: '头像文件大小不能超过 5MB',
  onSuccess: (result) => { editForm.avatarUrl = result.fileUrl }
})

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
  avatarUrl: [{ pattern: /^(https?:\/\/.+|\/uploads\/.+)$/, message: '请输入有效头像地址', trigger: 'blur' }]
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 是否显示学生概览：只要不是纯RUNNER就显示 */
const showStudentOverview = computed(() => {
  return !userStore.isRunner || userStore.roles.includes('STUDENT')
})

/** 是否显示跑腿员概览 */
const showRunnerOverview = computed(() => userStore.isRunner)

/** 性别文本 */
const genderText = computed(() => {
  if (!profile.value) return '-'
  const map = { 0: '未知', 1: '男', 2: '女' }
  return map[profile.value.gender] ?? '未知'
})

/** 角色文本 */
function roleText(code) {
  const map = { STUDENT: '学生', RUNNER: '跑腿员', ADMIN: '管理员' }
  return map[code] ?? code
}

/** 加载用户资料 */
async function loadProfile() {
  try {
    profile.value = await getUserProfile()
    if (profile.value) {
      userStore.setUserInfo({
        userId: userStore.userId,
        username: profile.value.username,
        realName: profile.value.realName || profile.value.username,
        roles: userStore.roles
      })
    }
  } catch {
    profile.value = null
  }
}

/** 加载学生概览 */
async function loadStudentOverview() {
  try {
    studentOverview.value = await getUserCenterOverview()
  } catch {
    studentOverview.value = null
  }
}

/** 加载跑腿员概览 */
async function loadRunnerOverview() {
  try {
    runnerOverview.value = await getRunnerCenterOverview()
  } catch {
    runnerOverview.value = null
  }
}

/** 打开编辑资料弹窗 */
function openEditDialog() {
  if (!profile.value) return
  editForm.realName = profile.value.realName || ''
  editForm.nickName = profile.value.nickName || ''
  editForm.phone = profile.value.phone || ''
  editForm.avatarUrl = profile.value.avatarUrl || ''
  editForm.gender = profile.value.gender ?? 0
  editVisible.value = true
}

/** 提交编辑资料 */
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
    // 空字符串不提交，避免覆盖后端空值
    if (!data.avatarUrl) delete data.avatarUrl
    await updateUserProfile(data)
    ElMessage.success('资料修改成功')
    editVisible.value = false
    await loadProfile()
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    editLoading.value = false
  }
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

function openPwdDialog() {
  resetPwdForm()
  pwdVisible.value = true
}

async function handlePwdSubmit() {
  if (pwdLoading.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  pwdLoading.value = true
  try {
    await changePassword({ ...pwdForm })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
    resetPwdForm()
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    pwdLoading.value = false
  }
}

/** 快捷跳转 */
function navigate(path) {
  router.push(path)
}

onMounted(async () => {
  pageLoading.value = true
  const tasks = [loadProfile()]
  if (showStudentOverview.value) tasks.push(loadStudentOverview())
  if (showRunnerOverview.value) tasks.push(loadRunnerOverview())
  await Promise.all(tasks)
  pageLoading.value = false
})
</script>

<template>
  <PageContainer title="个人中心" :loading="pageLoading">
    <div class="profile-center">
      <!-- 个人资料卡片 -->
      <el-card class="profile-card" shadow="never">
        <div class="profile-info">
          <div class="avatar-area">
            <el-avatar :size="72" :src="profile?.avatarUrl">
              <el-icon :size="36"><User /></el-icon>
            </el-avatar>
          </div>
          <div class="detail-area">
            <div class="detail-row">
              <span class="detail-name">{{ profile?.nickName || profile?.realName || profile?.username || '-' }}</span>
              <el-tag v-for="r in (profile?.roles || [])" :key="r" size="small" style="margin-left: 8px">
                {{ roleText(r) }}
              </el-tag>
            </div>
            <div class="detail-meta">
              <span>用户名：{{ profile?.username || '-' }}</span>
              <span>真实姓名：{{ profile?.realName || '-' }}</span>
              <span>手机号：{{ profile?.phone || '-' }}</span>
              <span>性别：{{ genderText }}</span>
              <span>注册时间：{{ formatTime(profile?.createTime) }}</span>
            </div>
          </div>
          <div class="action-area">
            <el-button :icon="Edit" @click="openEditDialog">编辑资料</el-button>
            <el-button :icon="Lock" @click="openPwdDialog">修改密码</el-button>
          </div>
        </div>
      </el-card>

      <!-- 学生概览 -->
      <template v-if="showStudentOverview && studentOverview">
        <h3 class="section-title">学生概览</h3>
        <div class="overview-grid">
          <div class="overview-card">
            <span class="overview-label">总订单</span>
            <span class="overview-value">{{ studentOverview.totalOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">待支付</span>
            <span class="overview-value warning">{{ studentOverview.unpaidOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">进行中</span>
            <span class="overview-value primary">{{ studentOverview.ongoingOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">已完成</span>
            <span class="overview-value success">{{ studentOverview.completedOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">已取消</span>
            <span class="overview-value info">{{ studentOverview.cancelledOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">申诉中</span>
            <span class="overview-value danger">{{ studentOverview.appealOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">未读消息</span>
            <span class="overview-value danger">{{ studentOverview.unreadMessageCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">跑腿员认证</span>
            <StatusTag v-if="studentOverview.runnerAuthStatus != null" type="auth_status" :value="studentOverview.runnerAuthStatus" size="small" />
            <span v-else class="overview-value info" style="font-size: 14px">未申请</span>
          </div>
        </div>

        <!-- 学生快捷入口 -->
        <div class="quick-entry">
          <div class="entry-card" @click="navigate('/order/my')">
            <el-icon size="24"><Document /></el-icon>
            <span>我的订单</span>
          </div>
          <div class="entry-card" @click="navigate('/user/address')">
            <el-icon size="24"><MapLocation /></el-icon>
            <span>地址管理</span>
          </div>
          <div class="entry-card" @click="navigate('/user/messages')">
            <el-icon size="24"><Bell /></el-icon>
            <span>消息中心</span>
          </div>
          <div class="entry-card" @click="navigate('/runner/auth')">
            <el-icon size="24"><Medal /></el-icon>
            <span>跑腿员认证</span>
          </div>
        </div>
      </template>

      <!-- 跑腿员概览 -->
      <template v-if="showRunnerOverview && runnerOverview">
        <h3 class="section-title">跑腿员概览</h3>

        <!-- 接单统计 -->
        <div class="overview-grid">
          <div class="overview-card">
            <span class="overview-label">已接单</span>
            <span class="overview-value">{{ runnerOverview.acceptedOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">进行中</span>
            <span class="overview-value primary">{{ runnerOverview.ongoingOrderCount ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">已完成</span>
            <span class="overview-value success">{{ runnerOverview.completedOrderCount ?? 0 }}</span>
          </div>
        </div>

        <!-- 收益统计 -->
        <div class="overview-grid">
          <div class="overview-card">
            <span class="overview-label">总收益</span>
            <span class="overview-value primary">¥{{ formatMoney(runnerOverview.totalIncome) }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">待结算</span>
            <span class="overview-value warning">¥{{ formatMoney(runnerOverview.pendingIncome) }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">已结算</span>
            <span class="overview-value success">¥{{ formatMoney(runnerOverview.settledIncome) }}</span>
          </div>
        </div>

        <!-- 评价统计 -->
        <div class="overview-grid eval-grid">
          <div class="overview-card">
            <span class="overview-label">平均评分</span>
            <span class="overview-value warning">{{ runnerOverview.averageScore ?? 0 }}</span>
          </div>
          <div class="overview-card">
            <span class="overview-label">评价总数</span>
            <span class="overview-value">{{ runnerOverview.totalEvaluationCount ?? 0 }}</span>
          </div>
        </div>

        <!-- 认证信息 -->
        <el-card v-if="runnerOverview.authInfo" class="auth-info-card" shadow="never">
          <template #header>
            <span class="card-header-text">认证信息</span>
          </template>
          <el-descriptions :column="isMobile ? 1 : 2" border size="small">
            <el-descriptions-item label="认证状态">
              <StatusTag type="auth_status" :value="runnerOverview.authInfo.authStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="证件号码">{{ runnerOverview.authInfo.certNoMasked || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ formatTime(runnerOverview.authInfo.reviewTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="runnerOverview.authInfo.authStatus === AUTH_STATUS_REJECTED" label="驳回原因">
              <span class="reject-reason">{{ runnerOverview.authInfo.rejectReason || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 跑腿员快捷入口 -->
        <div class="quick-entry">
          <div class="entry-card" @click="navigate('/runner/hall')">
            <el-icon size="24"><Tickets /></el-icon>
            <span>任务大厅</span>
          </div>
          <div class="entry-card" @click="navigate('/runner/orders')">
            <el-icon size="24"><Document /></el-icon>
            <span>我的接单</span>
          </div>
          <div class="entry-card" @click="navigate('/runner/income')">
            <el-icon size="24"><Money /></el-icon>
            <span>收益统计</span>
          </div>
          <div class="entry-card" @click="navigate('/runner/evaluations')">
            <el-icon size="24"><Star /></el-icon>
            <span>评价反馈</span>
          </div>
          <div class="entry-card" @click="navigate('/runner/auth')">
            <el-icon size="24"><Medal /></el-icon>
            <span>认证信息</span>
          </div>
        </div>
      </template>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑资料"
      :width="isMobile ? '95%' : '500px'"
      :close-on-click-modal="false"
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
        <el-form-item label="头像" prop="avatarUrl">
          <div class="avatar-upload-row">
            <!-- 上传按钮 -->
            <el-upload
              :show-file-list="false"
              :http-request="handleAvatarUpload"

              accept="image/jpeg,image/png,image/webp"
              :disabled="avatarUploading"
            >
              <el-button :loading="avatarUploading" :icon="Camera">
                {{ editForm.avatarUrl ? '重新上传' : '点击上传' }}
              </el-button>
            </el-upload>
            <!-- 预览已上传的头像 -->
            <el-avatar v-if="editForm.avatarUrl" :src="editForm.avatarUrl" :size="48" shape="square" />
          </div>
          <!-- 显示当前 URL -->
          <div v-if="editForm.avatarUrl" class="avatar-url-text">{{ editForm.avatarUrl }}</div>
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

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="pwdVisible"
      title="修改密码"
      :width="isMobile ? '95%' : '460px'"
      :close-on-click-modal="false"
      @close="pwdFormRef?.resetFields()"
    >
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handlePwdSubmit">确认修改</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.profile-center {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 个人资料卡片 */
.profile-card {
  border-radius: 12px;
}

.profile-info {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.avatar-area {
  flex-shrink: 0;
}

.detail-area {
  flex: 1;
  min-width: 0;
}

.detail-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}

.detail-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  font-size: 14px;
  color: #606266;
}

.action-area {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 区域标题 */
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 概览卡片网格 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.eval-grid {
  grid-template-columns: repeat(2, 1fr);
}

.overview-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 16px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  text-align: center;
}

.overview-label {
  font-size: 13px;
  color: #909399;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.overview-value.primary { color: #409EFF; }
.overview-value.warning { color: #E6A23C; }
.overview-value.success { color: #67C23A; }
.overview-value.info { color: #909399; }
.overview-value.danger { color: #F56C6C; }

/* 认证信息卡片 */
.auth-info-card {
  border-radius: 8px;
}

.card-header-text {
  font-size: 16px;
  font-weight: 600;
}

.reject-reason {
  color: #F56C6C;
}

/* 头像上传区域 */
.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-url-text {
  font-size: 12px;
  color: #909399;
  word-break: break-all;
  margin-top: 4px;
}

/* 快捷入口 */
.quick-entry {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.entry-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
  color: #409EFF;
  border: 1px solid #e4e7ed;
  min-width: 120px;
}

.entry-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.entry-card span {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .profile-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 16px;
  }

  .detail-row {
    justify-content: center;
  }

  .detail-meta {
    flex-direction: column;
    gap: 4px;
    align-items: center;
  }

  .action-area {
    flex-direction: row;
    width: 100%;
  }

  .action-area .el-button {
    flex: 1;
  }

  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .eval-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .overview-card {
    padding: 16px 12px;
  }

  .overview-value {
    font-size: 18px;
  }

  .quick-entry {
    gap: 8px;
  }

  .entry-card {
    padding: 12px 16px;
    min-width: 0;
    flex: 1;
    justify-content: center;
  }
}
</style>
