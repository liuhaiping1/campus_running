<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, submitEvaluation } from '@/api/order'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const orderId = String(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const order = ref(null)
const formRef = ref(null)

const form = reactive({
  orderId: orderId,
  score: undefined,
  content: ''
})

const rules = {
  score: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [{ max: 500, message: '评价内容不超过500字', trigger: 'blur' }]
}

async function fetchOrder() {
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId)
    if (order.value.orderStatus !== 7) {
      ElMessage.warning('该订单状态不支持评价')
      router.back()
      return
    }
    if (order.value.evaluationId) {
      ElMessage.warning('该订单已评价')
      router.back()
      return
    }
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await submitEvaluation({ ...form })
    ElMessage.success('评价提交成功')
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
  <PageContainer title="订单评价" show-back @back="router.push(`/order/${orderId}`)">
    <div v-loading="loading" class="eval-page">
      <!-- 订单摘要 -->
      <div v-if="order" class="order-summary">
        <h3>{{ order.title }}</h3>
        <p class="order-meta">
          <StatusTag type="order_status" :value="order.orderStatus" size="small" />
          <span style="margin-left:8px">订单号: {{ order.orderNo }}</span>
        </p>
      </div>

      <!-- 评价表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="eval-form">
        <el-form-item label="评分" prop="score">
          <el-rate v-model="form.score" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="分享您的体验（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交评价
          </el-button>
          <el-button @click="router.push(`/order/${orderId}`)">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </PageContainer>
</template>

<style scoped>
.eval-page {
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

.eval-form {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

@media (max-width: 768px) {
  .eval-form {
    padding: 16px;
  }
}
</style>
