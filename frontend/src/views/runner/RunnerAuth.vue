<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { applyAuth } from '@/api/runnerAuth'
import { getRunnerAuthProfile } from '@/api/userProfile'
import { useUserStore } from '@/stores/user'
import { useFileUpload } from '@/composables/useFileUpload'
import StatusTag from '@/components/StatusTag.vue'

/** 认证状态常量，与后端 AuthStatusEnum 保持一致 */
const AUTH_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2
}

const userStore = useUserStore()

const loading = ref(true)
const loadError = ref(false)         // 加载认证状态失败
const submitting = ref(false)
const authInfo = ref(null)

/** 证件正面上传（composable 封装） */
const { uploading: frontUploading, handleUpload: handleFrontUpload } = useFileUpload({
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  typeErrorMsg: '证件照片仅支持 JPG、PNG、WebP 格式',
  sizeErrorMsg: '证件照片大小不能超过 5MB',
  successMsg: '证件正面上传成功',
  onSuccess: (result) => { form.certFrontUrl = result.fileUrl }
})

/** 证件背面上传 */
const { uploading: backUploading, handleUpload: handleBackUpload } = useFileUpload({
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  typeErrorMsg: '证件照片仅支持 JPG、PNG、WebP 格式',
  sizeErrorMsg: '证件照片大小不能超过 5MB',
  successMsg: '证件背面上传成功',
  onSuccess: (result) => { form.certBackUrl = result.fileUrl }
})

const formRef = ref(null)

const form = reactive({
  schoolName: '',
  campusName: '',
  certType: undefined,
  certNo: '',
  certFrontUrl: '',
  certBackUrl: ''
})

const rules = {
  schoolName: [{ required: true, message: '请输入学校名称', trigger: 'blur' }],
  campusName: [{ required: true, message: '请输入校区名称', trigger: 'blur' }],
  certType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  certNo: [{ required: true, message: '请输入证件编号', trigger: 'blur' }],
  certFrontUrl: [{ required: true, message: '请输入证件正面URL', trigger: 'blur' }],
  certBackUrl: [{ required: true, message: '请输入证件背面URL', trigger: 'blur' }]
}

// 证件类型选项
const certTypeOptions = [
  { value: 1, label: '学生证' },
  { value: 2, label: '身份证' }
]

/** 从后端加载跑腿员认证状态 */
async function loadAuthStatus() {
  loading.value = true
  loadError.value = false
  try {
    authInfo.value = await getRunnerAuthProfile()
    // 无记录时 getRunnerAuthProfile 可能返回 null，表示未申请
  } catch {
    authInfo.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 提交认证申请 */
async function handleSubmit() {
  if (submitting.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await applyAuth({ ...form })
    ElMessage.success('认证申请已提交')
    // 提交后重新加载认证状态，切换到待审核视图
    await loadAuthStatus()
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    submitting.value = false
  }
}

onMounted(loadAuthStatus)
</script>

<template>
  <div class="runner-auth-page">
    <div class="page-header">
      <h2 class="page-title">跑腿员认证</h2>
      <p class="page-subtitle">提交认证资料，审核通过后即可接单</p>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 加载失败 -->
    <el-card v-else-if="loadError" class="auth-card">
      <el-result icon="error" title="加载失败" sub-title="无法获取认证状态，请稍后重试">
        <template #extra>
          <el-button type="primary" @click="loadAuthStatus">重新加载</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 已通过审核 -->
    <el-card v-else-if="authInfo && authInfo.authStatus === AUTH_STATUS.APPROVED" class="auth-card">
      <el-result icon="success" title="您已是认证跑腿员" sub-title="您可以前往任务大厅接单赚取收益">
        <template #extra>
          <el-button type="primary" @click="$router.push('/runner/hall')">前往任务大厅</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 待审核 -->
    <el-card v-else-if="authInfo && authInfo.authStatus === AUTH_STATUS.PENDING" class="auth-card">
      <el-result icon="info" title="认证审核中" sub-title="请耐心等待管理员审核，审核结果请在消息中心或后台审核结果中查看">
        <template #extra>
          <StatusTag type="auth_status" :value="AUTH_STATUS.PENDING" />
          <el-button style="margin-top:12px" @click="$router.push('/user/messages')">查看消息中心</el-button>
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 已驳回或未认证：显示表单 -->
    <el-card v-else class="auth-card">
      <!-- 驳回提示 -->
      <el-alert
        v-if="authInfo && authInfo.authStatus === AUTH_STATUS.REJECTED"
        title="认证被驳回"
        :description="authInfo.rejectReason || '管理员驳回了您的认证申请，请修改后重新提交'"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
      <template #header>
        <span class="card-header-text">
          {{ authInfo && authInfo.authStatus === AUTH_STATUS.REJECTED ? '重新提交认证资料' : '填写认证资料' }}
        </span>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
      >
        <el-form-item prop="schoolName" label="学校名称">
          <el-input v-model="form.schoolName" placeholder="请输入就读学校" maxlength="50" />
        </el-form-item>

        <el-form-item prop="campusName" label="所在校区">
          <el-input v-model="form.campusName" placeholder="请输入所在校区" maxlength="50" />
        </el-form-item>

        <el-form-item prop="certType" label="证件类型">
          <el-select v-model="form.certType" placeholder="请选择证件类型" style="width: 100%">
            <el-option
              v-for="item in certTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item prop="certNo" label="证件编号">
          <el-input v-model="form.certNo" placeholder="请输入学号或证件编号" maxlength="30" />
        </el-form-item>

        <el-form-item prop="certFrontUrl" label="证件正面照片">
          <div class="cert-upload-row">
            <el-upload
              :show-file-list="false"
              :http-request="handleFrontUpload"

              accept="image/jpeg,image/png,image/webp"
              :disabled="frontUploading"
            >
              <el-button :loading="frontUploading" :icon="Upload">
                {{ form.certFrontUrl ? '重新上传' : '点击上传' }}
              </el-button>
            </el-upload>
            <el-image
              v-if="form.certFrontUrl"
              :src="form.certFrontUrl"
              fit="cover"
              class="cert-preview"
              :preview-src-list="[form.certFrontUrl]"
              preview-teleported
            />
          </div>
          <div v-if="form.certFrontUrl" class="cert-url-text">{{ form.certFrontUrl }}</div>
        </el-form-item>

        <el-form-item prop="certBackUrl" label="证件背面照片">
          <div class="cert-upload-row">
            <el-upload
              :show-file-list="false"
              :http-request="handleBackUpload"

              accept="image/jpeg,image/png,image/webp"
              :disabled="backUploading"
            >
              <el-button :loading="backUploading" :icon="Upload">
                {{ form.certBackUrl ? '重新上传' : '点击上传' }}
              </el-button>
            </el-upload>
            <el-image
              v-if="form.certBackUrl"
              :src="form.certBackUrl"
              fit="cover"
              class="cert-preview"
              :preview-src-list="[form.certBackUrl]"
              preview-teleported
            />
          </div>
          <div v-if="form.certBackUrl" class="cert-url-text">{{ form.certBackUrl }}</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
            style="width: 100%"
          >
            {{ submitting ? '提交中...' : '提交认证申请' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.runner-auth-page {
  max-width: 520px;
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

.auth-card {
  border-radius: 8px;
}

.card-header-text {
  font-size: 16px;
  font-weight: 600;
}

.auth-form {
  max-width: 100%;
}

/* 加载中 */
.loading-wrap {
  padding: 24px 0;
}

/* 证件上传区域 */
.cert-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cert-preview {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  object-fit: cover;
}

.cert-url-text {
  font-size: 12px;
  color: #909399;
  word-break: break-all;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .runner-auth-page {
    padding: 0;
  }
}
</style>
