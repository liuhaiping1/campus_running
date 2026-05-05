<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Upload } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/system'
import { listAddress } from '@/api/address'
import { createOrder } from '@/api/order'
import { estimateRoute } from '@/api/map'
import { useFileUpload } from '@/composables/useFileUpload'
import { formatMoney } from '@/utils/format'
import { addressToLocation } from '@/utils/address'
import PageContainer from '@/components/PageContainer.vue'
import MapLocationPicker from '@/components/MapLocationPicker.vue'

const router = useRouter()
const route = useRoute()

const categories = ref([])
const addresses = ref([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const pickupLocation = ref(null)
const deliveryLocation = ref(null)
const estimateResult = ref(null)
const estimateLoading = ref(false)

/** 已上传成功的附件 URL 列表 */
const attachmentUrls = ref([])

/** 附件上传（composable 封装） */
const { uploading: attachmentUploading, handleUpload: handleAttachmentUpload } = useFileUpload({
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'],
  typeErrorMsg: '附件仅支持 JPG、PNG、WebP、PDF 格式',
  sizeErrorMsg: '附件大小不能超过 5MB',
  successMsg: '附件上传成功',
  onSuccess: (result) => { attachmentUrls.value.push(result.fileUrl) }
})

const form = reactive({
  categoryId: undefined,
  title: '',
  orderDesc: '',
  pickupAddress: '',
  deliveryAddress: '',
  pickupLng: undefined,
  pickupLat: undefined,
  deliveryLng: undefined,
  deliveryLat: undefined,
  distanceKm: undefined,
  tipFee: 0,
  deadlineTime: ''
})

const rules = {
  categoryId: [{ required: true, message: '请选择任务分类', trigger: 'change' }],
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { max: 100, message: '标题不超过100字', trigger: 'blur' }
  ],
  orderDesc: [
    { required: true, message: '请输入任务描述', trigger: 'blur' },
    { max: 500, message: '描述不超过500字', trigger: 'blur' }
  ],
  pickupAddress: [{
    required: true,
    validator: (rule, value, callback) => {
      if (!pickupLocation.value) callback(new Error('请选择取件地址'))
      else callback()
    },
    trigger: 'change'
  }],
  deliveryAddress: [{
    required: true,
    validator: (rule, value, callback) => {
      if (!deliveryLocation.value) callback(new Error('请选择送达地址'))
      else callback()
    },
    trigger: 'change'
  }],
  deadlineTime: [{ required: true, message: '请选择期望完成时间', trigger: 'change' }]
}

function syncLocation(locationRef, prefix) {
  watch(locationRef, (val) => {
    form[`${prefix}Address`] = val?.name ?? ''
    form[`${prefix}Lng`] = val?.lng ?? undefined
    form[`${prefix}Lat`] = val?.lat ?? undefined
  })
}
syncLocation(pickupLocation, 'pickup')
syncLocation(deliveryLocation, 'delivery')

const showEstimate = computed(() =>
  form.pickupLng && form.pickupLat && form.deliveryLng && form.deliveryLat && form.categoryId
)

let estimateTimer = null
onBeforeUnmount(() => clearTimeout(estimateTimer))

async function handleEstimate() {
  if (!showEstimate.value) return
  estimateLoading.value = true
  try {
    estimateResult.value = await estimateRoute({
      categoryId: form.categoryId,
      originLng: form.pickupLng,
      originLat: form.pickupLat,
      destinationLng: form.deliveryLng,
      destinationLat: form.deliveryLat,
      tipFee: form.tipFee || 0
    })
    if (estimateResult.value?.routeDistanceKm) {
      form.distanceKm = estimateResult.value.routeDistanceKm
    }
  } catch {
    estimateResult.value = null
  } finally {
    estimateLoading.value = false
  }
}

/** 移除已上传的附件 */
function removeAttachment(index) {
  attachmentUrls.value.splice(index, 1)
}

// 分类变化时自动填充标题（仅当标题为空或上次也是自动填充时）
let titleAutoFilled = false
watch(() => form.categoryId, (newId) => {
  const matched = categories.value.find(c => c.id == newId)
  if (matched) {
    if (!form.title || titleAutoFilled) {
      form.title = `帮${matched.categoryName}`
      titleAutoFilled = true
    }
  }
})

watch(
  () => [form.pickupLng, form.pickupLat, form.deliveryLng, form.deliveryLat, form.categoryId, form.tipFee],
  () => {
    clearTimeout(estimateTimer)
    if (showEstimate.value) estimateTimer = setTimeout(handleEstimate, 300)
  }
)


async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      ...form,
      categoryId: Number(form.categoryId),
      attachmentUrls: attachmentUrls.value.length > 0 ? attachmentUrls.value.join(',') : undefined
    }
    const orderId = await createOrder(payload)
    ElMessage.success('订单创建成功')
    router.push(`/order/${orderId}`)
  } catch {
    // 错误由请求层统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [cats, addrs] = await Promise.all([
      getCategoryList(),
      listAddress()
    ])
    categories.value = (cats || []).filter(c => c.categoryStatus === 1)
    addresses.value = addrs || []

    if (route.query.categoryId) {
      // 保持与 categories 中 id 相同的类型
      const matched = categories.value.find(c => c.id == route.query.categoryId)
      if (matched) form.categoryId = matched.id
    }
  } catch {
    categories.value = []
    addresses.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <PageContainer title="发布订单" subtitle="填写信息创建跑腿任务">
    <div v-loading="loading" class="create-page">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
        class="create-form"
      >
        <!-- 分类选择 -->
        <el-form-item label="任务分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择任务分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="`${cat.categoryName}（起步价 ${cat.baseFee} 元）`"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>

        <!-- 标题 -->
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="简要描述任务，如：帮取快递" maxlength="100" />
        </el-form-item>

        <!-- 描述 -->
        <el-form-item label="任务描述" prop="orderDesc">
          <el-input
            v-model="form.orderDesc"
            type="textarea"
            :rows="3"
            placeholder="详细描述任务要求、物品信息等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 已有的地址快捷选择 -->
        <el-form-item v-if="addresses.length > 0" label="已有地址">
          <div class="address-quick-list">
            <el-button
              v-for="addr in addresses"
              :key="addr.id"
              size="small"
              style="margin-right: 8px; margin-bottom: 8px"
              @click="pickupLocation = addressToLocation(addr)"
            >
              取: {{ addr.contactName }} {{ addr.buildingName }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 取件地址 -->
        <el-form-item label="取件地址" prop="pickupAddress">
          <MapLocationPicker v-model="pickupLocation" placeholder="请选择取件地址" :addresses="addresses" />
        </el-form-item>

        <!-- 送达地址 -->
        <el-form-item label="送达地址" prop="deliveryAddress">
          <div class="delivery-row">
            <MapLocationPicker v-model="deliveryLocation" placeholder="请选择送达地址" style="flex:1" :addresses="addresses" />
            <el-dropdown v-if="addresses.length > 0">
              <el-button size="small">从地址簿选择</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="addr in addresses"
                    :key="addr.id"
                    @click="deliveryLocation = addressToLocation(addr)"
                  >
                    {{ addr.contactName }} {{ addr.buildingName }} {{ addr.detailAddress }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-form-item>

        <!-- 路费预估 -->
        <el-form-item v-if="showEstimate" label="费用预估">
          <div v-if="estimateLoading" class="estimate-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>计算中...</span>
          </div>
          <div v-else-if="estimateResult" class="estimate-result">
            <div class="estimate-amount">
              预估费用：<strong>¥{{ formatMoney(estimateResult.orderAmount) }}</strong>
            </div>
            <div class="estimate-detail">
              <span>距离 {{ estimateResult.routeDistanceKm || '—' }}km</span>
              <span>·</span>
              <span>基础 ¥{{ formatMoney(estimateResult.baseFee) }}</span>
              <span>·</span>
              <span>距离费 ¥{{ formatMoney(estimateResult.distanceFee) }}</span>
              <span v-if="estimateResult.tipFee > 0">· 小费 ¥{{ formatMoney(estimateResult.tipFee) }}</span>
            </div>
          </div>
        </el-form-item>

        <!-- 期望完成时间 -->
        <el-form-item label="期望完成" prop="deadlineTime">
          <el-date-picker
            v-model="form.deadlineTime"
            type="datetime"
            placeholder="选择时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="(date) => date < Date.now() - 86400000"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 小费 -->
        <el-form-item label="小费">
          <el-input-number v-model="form.tipFee" :min="0" :precision="2" :step="1" />
          <span class="tip-hint">元（选填，给跑腿员的额外奖励）</span>
        </el-form-item>

        <!-- 附件上传 -->
        <el-form-item label="附件">
          <div class="attachment-area">
            <el-upload
              :show-file-list="false"
              :http-request="handleAttachmentUpload"

              accept="image/jpeg,image/png,image/webp,.pdf"
              :disabled="attachmentUploading"
            >
              <el-button :loading="attachmentUploading" :icon="Upload" size="small">
                上传附件
              </el-button>
            </el-upload>
            <span class="tip-hint">支持 JPG、PNG、WebP、PDF，单文件不超过 5MB</span>
          </div>
          <!-- 已上传的附件列表 -->
          <div v-if="attachmentUrls.length > 0" class="attachment-list">
            <div v-for="(url, index) in attachmentUrls" :key="index" class="attachment-item">
              <el-image
                v-if="/\.(jpg|jpeg|png|webp)$/i.test(url)"
                :src="url"
                fit="cover"
                class="attachment-thumb"
                :preview-src-list="attachmentUrls"
                preview-teleported
              />
              <el-link v-else :href="url" target="_blank" type="primary" :underline="false">
                {{ decodeURIComponent(url.split('/').pop()) }}
              </el-link>
              <el-button type="danger" link size="small" @click="removeAttachment(index)">移除</el-button>
            </div>
          </div>
        </el-form-item>

        <!-- 提交 -->
        <el-form-item>
          <el-button type="primary" :loading="submitting" size="large" @click="handleSubmit">
            提交订单
          </el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </PageContainer>
</template>

<style scoped>
.create-page {
  max-width: 680px;
}

.create-form {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 24px;
}

.delivery-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.address-quick-list {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.tip-hint {
  margin-left: 8px;
  color: var(--text-secondary);
  font-size: var(--font-sm);
}

.estimate-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: var(--font-sm);
}

.estimate-result {
  background: var(--bg-page);
  border-radius: var(--radius-button);
  padding: 10px 14px;
  width: 100%;
}

.estimate-amount {
  font-size: var(--font-body);
  color: var(--text-primary);
  margin-bottom: 4px;
}

.estimate-amount strong {
  color: var(--color-primary);
  font-size: var(--font-title);
}

.estimate-detail {
  display: flex;
  gap: 6px;
  font-size: var(--font-sm);
  color: var(--text-secondary);
  flex-wrap: wrap;
}

/* 附件上传区域 */
.attachment-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.attachment-list {
  width: 100%;
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 6px 10px;
}

.attachment-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  object-fit: cover;
  cursor: pointer;
}

@media (max-width: 768px) {
  .create-form {
    padding: 16px;
  }
  .create-form :deep(.el-form-item__label) {
    width: 80px !important;
  }
}
</style>
