<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

/** 支付宝同步跳回时携带 orderId */
const orderId = computed(() => route.query.orderId || '')

// 阿里支付同步跳回默认视为支付已提交，实际结果以后端异步通知为准
const status = computed(() => route.query.status || 'success')

const resultConfig = computed(() => {
  const map = {
    success: {
      icon: 'success',
      title: '支付已提交',
      desc: '支付宝支付请求已提交。支付结果以订单详情中实际支付状态为准，通常 1-3 秒内到账。'
    },
    pending: {
      icon: 'info',
      title: '支付处理中',
      desc: '支付可能还在处理中，请稍后查看订单状态。如有疑问请联系管理员。'
    },
    unavailable: {
      icon: 'warning',
      title: '支付状态未知',
      desc: '未能确认支付结果。请前往我的订单查看该订单的最新状态。'
    }
  }
  return map[status.value] || map.unavailable
})

function goToOrder() {
  if (orderId.value) {
    router.push(`/order/${orderId.value}`)
  }
}
</script>

<template>
  <div class="pay-result-page">
    <el-card class="result-card">
      <el-result
        :icon="resultConfig.icon"
        :title="resultConfig.title"
        :sub-title="resultConfig.desc"
      >
        <template #extra>
          <div class="result-actions">
            <el-button
              v-if="orderId"
              type="primary"
              @click="goToOrder"
            >
              查看订单详情
            </el-button>
            <el-button @click="router.push('/order/my')">我的订单</el-button>
            <el-button @click="router.push('/')">返回首页</el-button>
          </div>
        </template>
      </el-result>

      <!-- 提示信息 -->
      <div class="info-block" v-if="orderId">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单编号">{{ orderId }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">
            <el-tag type="primary" size="small">支付宝沙箱</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="tip-block">
        <el-alert
          title="支付由支付宝异步通知确认"
          type="info"
          :closable="false"
          show-icon
          description="支付成功后，支付宝会异步通知后端更新订单状态。如订单状态未及时更新，请稍后刷新订单详情查看。"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.pay-result-page {
  display: flex;
  justify-content: center;
  padding-top: 48px;
}

.result-card {
  width: 100%;
  max-width: 560px;
  border-radius: 8px;
}

.result-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.tip-block {
  margin-top: 16px;
}

.info-block {
  margin-top: 16px;
}

@media (max-width: 768px) {
  .pay-result-page {
    padding: 24px 12px;
  }

  .result-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
