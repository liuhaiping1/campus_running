<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getOrderDetail, submitAppeal } from '@/api/order'
import { useFileUpload } from '@/composables/useFileUpload'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const orderId = String(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const order = ref(null)
const formRef = ref(null)

/** 证据上传（composable 封装，追加到逗号分隔的 evidenceUrls 字符串） */
const { uploading: evidenceUploading, handleUpload: handleEvidenceUpload } = useFileUpload({
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'],
  typeErrorMsg: '证据文件仅支持 JPG、PNG、WebP、PDF 格式',
  sizeErrorMsg: '证据文件大小不能超过 5MB',
  successMsg: '证据上传成功',
  onSuccess: (result) => {
    const newUrl = result.fileUrl
    form.evidenceUrls = form.evidenceUrls
      ? (form.evidenceUrls + (form.evidenceUrls.endsWith(',') ? '' : ',') + newUrl)
      : newUrl
  }
})

const form = reactive({
  orderId: orderId,
  appealType: undefined,
  appealContent: '',
  evidenceUrls: ''
})

// 申诉类型：1取消争议 2履约争议 3退款争议
const appealTypeOptions = [
  { label: '取消争议', value: 1 },
  { label: '履约争议', value: 2 },
  { label: '退款争议', value: 3 }
]

const rules = {
  appealType: [{ required: true, message: '请选择申诉类型', trigger: 'change' }],
  appealContent: [
    { required: true, message: '请输入申诉内容', trigger: 'blur' },
    { max: 1000, message: '申诉内容不超过1000字', trigger: 'blur' }
  ],
  evidenceUrls: [{ max: 1000, message: '证据链接不超过1000字', trigger: 'blur' }]
}

async function fetchOrder() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId)
    if (order.value.orderStatus < 2 || order.value.orderStatus > 7) {
      ElMessage.warning('该订单状态不支持申诉')
      router.back()
      return
    }
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

/** 移除已上传的证据 */
function removeEvidence(index) {
  const urls = form.evidenceUrls.split(',').filter(u => u.trim())
  urls.splice(index, 1)
  form.evidenceUrls = urls.join(',')
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await submitAppeal({ ...form })
    ElMessage.success('申诉提交成功')
    router.push(`/order/${orderId}`)
  } catch {
    // 错误由请求层提示
  } finally {
    submitting.value = false
  }
}

onMounted(fetchOrder)
</script>

<template>
  <PageContainer title="提交申诉" show-back @back="router.push(`/order/${orderId}`)">
    <div v-loading="loading" class="appeal-page">
      <!-- 订单摘要 -->
      <div v-if="order" class="order-summary">
        <h3>{{ order.title }}</h3>
        <p class="order-meta">
          <StatusTag type="order_status" :value="order.orderStatus" size="small" />
          <span style="margin-left:8px">订单号: {{ order.orderNo }}</span>
        </p>
      </div>

      <!-- 申诉表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="appeal-form">
        <el-form-item label="申诉类型" prop="appealType">
          <el-select v-model="form.appealType" placeholder="请选择申诉类型" style="width:100%">
            <el-option
              v-for="opt in appealTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉内容" prop="appealContent">
          <el-input
            v-model="form.appealContent"
            type="textarea"
            :rows="5"
            placeholder="请详细描述申诉原因"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="证据材料" prop="evidenceUrls">
          <div class="evidence-upload-area">
            <el-upload
              :show-file-list="false"
              :http-request="handleEvidenceUpload"

              accept="image/jpeg,image/png,image/webp,.pdf"
              :disabled="evidenceUploading"
            >
              <el-button :loading="evidenceUploading" :icon="Upload" size="small">
                上传证据
              </el-button>
            </el-upload>
            <span class="evidence-tip">支持 JPG、PNG、WebP、PDF，单文件不超过 5MB</span>
          </div>
          <!-- 已上传证据预览 -->
          <div v-if="form.evidenceUrls" class="evidence-preview-list">
            <div
              v-for="(url, index) in form.evidenceUrls.split(',').filter(u => u.trim())"
              :key="index"
              class="evidence-preview-item"
            >
              <el-image
                v-if="/\.(jpg|jpeg|png|webp)$/i.test(url.trim())"
                :src="url.trim()"
                fit="cover"
                class="evidence-thumb"
                :preview-src-list="[url.trim()]"
                preview-teleported
              />
              <el-link v-else :href="url.trim()" target="_blank" type="primary" :underline="false">
                {{ decodeURIComponent(url.trim().split('/').pop()) }}
              </el-link>
              <el-button type="danger" link size="small" @click="removeEvidence(index)">移除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交申诉
          </el-button>
          <el-button @click="router.push(`/order/${orderId}`)">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </PageContainer>
</template>

<style scoped>
.appeal-page {
  max-width: 600px;
}

.order-summary {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  border: 1px solid #f0f0f0;
}

.order-summary h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.order-meta {
  margin: 0;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
}

.appeal-form {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

/* 证据上传区域 */
.evidence-upload-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.evidence-tip {
  font-size: 12px;
  color: #909399;
}

.evidence-preview-list {
  width: 100%;
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.evidence-preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 6px 10px;
}

.evidence-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  object-fit: cover;
  cursor: pointer;
}

@media (max-width: 768px) {
  .appeal-form {
    padding: 16px;
  }
}
</style>
