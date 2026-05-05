<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMessageList, readMessage, readAllMessage } from '@/api/message'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const unreadCount = ref(0)
const detailVisible = ref(false)
const currentMessage = ref(null)
const { isMobile } = useResponsive()

const query = reactive({
  bizType: undefined,
  isRead: undefined,
  pageNum: 1,
  pageSize: 10
})

const bizTypeMap = {
  ORDER: '订单消息',
  SYSTEM: '系统通知',
  REVIEW: '审核消息',
  PAYMENT: '支付消息',
  EVALUATION: '评价消息'
}

async function fetchMessages() {
  loading.value = true
  try {
    const params = {}
    if (query.bizType) params.bizType = query.bizType
    if (query.isRead !== undefined && query.isRead !== '') params.isRead = query.isRead
    params.pageNum = query.pageNum
    params.pageSize = query.pageSize
    const data = await getMessageList(params)
    records.value = data.records || []
    total.value = parseTotal(data.total)
    unreadCount.value = data.unreadCount || 0
  } catch {
    records.value = []
    total.value = 0
    unreadCount.value = 0
  } finally {
    loading.value = false
  }
}

// 点击消息：打开详情并标记已读
async function handleMessageClick(msg) {
  currentMessage.value = msg
  detailVisible.value = true
  if (msg.isRead === 0) {
    try {
      await readMessage(msg.id)
      msg.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // 静默失败
    }
  }
}

async function handleReadAll() {
  if (unreadCount.value === 0) {
    ElMessage.info('没有未读消息')
    return
  }
  try {
    await readAllMessage()
    ElMessage.success('已全部标记为已读')
    unreadCount.value = 0
    records.value.forEach(r => { r.isRead = 1 })
  } catch {
    // 错误由请求层提示
  }
}

function handleFilterChange() {
  query.pageNum = 1
  fetchMessages()
}

function handlePageChange(page) {
  query.pageNum = page
  fetchMessages()
}

onMounted(fetchMessages)
</script>

<template>
  <PageContainer title="消息中心" subtitle="查看系统通知和订单消息">
    <template #extra>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button :disabled="unreadCount === 0" @click="handleReadAll">全部已读</el-button>
      </el-badge>
    </template>

    <div class="message-page">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="query.bizType" placeholder="消息类型" clearable @change="handleFilterChange" style="width:140px">
          <el-option label="订单消息" value="ORDER" />
          <el-option label="系统消息" value="SYSTEM" />
          <el-option label="审核消息" value="REVIEW" />
        </el-select>
        <el-select v-model="query.isRead" placeholder="阅读状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="message-list">
        <el-skeleton v-for="i in 3" :key="i" animated />
      </div>

      <!-- 空状态 -->
      <EmptyState v-else-if="records.length === 0" description="暂无消息" />

      <!-- 消息列表 -->
      <div v-else class="message-list">
        <div
          v-for="msg in records"
          :key="msg.id"
          class="message-item"
          :class="{ 'is-unread': msg.isRead === 0 }"
          @click="handleMessageClick(msg)"
        >
          <div class="msg-header">
            <span class="msg-title">
              <span v-if="msg.isRead === 0" class="unread-dot" />
              {{ msg.title }}
            </span>
            <StatusTag type="message_read_status" :value="msg.isRead" size="small" />
          </div>
          <p class="msg-summary">{{ msg.content }}</p>
          <div class="msg-footer">
            <el-tag size="small" type="info">{{ bizTypeMap[msg.bizType] || msg.bizType }}</el-tag>
            <span class="msg-time">{{ formatTime(msg.sendTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentMessage?.title" :width="isMobile ? '95%' : '560px'" :close-on-click-modal="false">
      <div v-if="currentMessage" class="msg-detail">
        <div class="msg-meta">
          <el-tag size="small">{{ currentMessage.bizType }}</el-tag>
          <span>{{ formatTime(currentMessage.sendTime) }}</span>
        </div>
        <el-divider />
        <p class="msg-content">{{ currentMessage.content }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: box-shadow 0.3s;
}
.message-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.message-item.is-unread {
  background: #f0f9ff;
  border-color: #b3d8ff;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.msg-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #F56C6C;
  display: inline-block;
  flex-shrink: 0;
}

.msg-summary {
  color: #606266;
  font-size: 14px;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.msg-time {
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

.msg-detail .msg-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  color: #909399;
  font-size: 13px;
}

.msg-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .message-item {
    padding: 12px 16px;
  }
  .pagination {
    justify-content: center;
  }
}
</style>
