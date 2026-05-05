<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getNoticeList } from '@/api/system'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime, parseTotal } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const loading = ref(false)
const notices = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentNotice = ref(null)
const { isMobile } = useResponsive()

const query = reactive({
  noticeType: undefined,
  pageNum: 1,
  pageSize: 10
})

// 公告类型选项：全部、普通(1)、重要(2)、维护(3)
const typeOptions = [
  { label: '全部', value: undefined },
  { label: '普通公告', value: 1 },
  { label: '重要公告', value: 2 },
  { label: '维护公告', value: 3 }
]

// 截取公告内容摘要
function getSummary(content) {
  if (!content) return ''
  return content.length > 100 ? content.slice(0, 100) + '...' : content
}

async function fetchNotices() {
  loading.value = true
  try {
    // 构造请求参数，过滤掉 undefined 值
    const params = {}
    if (query.noticeType !== undefined && query.noticeType !== '') {
      params.noticeType = query.noticeType
    }
    params.pageNum = query.pageNum
    params.pageSize = query.pageSize
    const result = await getNoticeList(params)
    notices.value = result.records || []
    total.value = parseTotal(result.total)
  } catch {
    notices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleNoticeClick(notice) {
  currentNotice.value = notice
  detailVisible.value = true
}

function handlePageChange(page) {
  query.pageNum = page
  fetchNotices()
}

function handleFilterChange() {
  query.pageNum = 1
  fetchNotices()
}

function resetFilter() {
  query.noticeType = undefined
  query.pageNum = 1
  fetchNotices()
}

onMounted(() => {
  fetchNotices()
})
</script>

<template>
  <PageContainer title="公告列表" subtitle="系统公告信息">
    <div class="notice-page">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select
          v-model="query.noticeType"
          placeholder="公告类型"
          clearable
          style="width: 160px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="opt in typeOptions"
            :key="opt.label"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <!-- 列表加载态 -->
      <div v-if="loading" class="notice-list">
        <el-skeleton v-for="i in 3" :key="i" animated />
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="notices.length === 0"
        description="暂无公告"
      />

      <!-- 公告卡片列表 -->
      <div v-else class="notice-list">
        <div
          v-for="notice in notices"
          :key="notice.id"
          class="notice-card"
          @click="handleNoticeClick(notice)"
        >
          <div class="notice-header">
            <StatusTag type="notice_type" :value="notice.noticeType" size="small" />
            <span class="notice-title">{{ notice.noticeTitle }}</span>
          </div>
          <p class="notice-summary">{{ getSummary(notice.noticeContent) }}</p>
          <span class="notice-time">{{ formatTime(notice.publishTime) }}</span>
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

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" title="公告详情" :width="isMobile ? '95%' : '600px'" :close-on-click-modal="false">
      <div v-if="currentNotice" class="notice-detail">
        <h3>{{ currentNotice.noticeTitle }}</h3>
        <div class="notice-meta">
          <StatusTag type="notice_type" :value="currentNotice.noticeType" size="small" />
          <span>发布于 {{ formatTime(currentNotice.publishTime) }}</span>
        </div>
        <el-divider />
        <p class="notice-content">{{ currentNotice.noticeContent }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.notice-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 公告列表 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-card {
  background: #fff;
  border-radius: var(--radius-card, 8px);
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.3s;
  border: 1px solid #f0f0f0;
}
.notice-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #409EFF;
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.notice-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.notice-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.notice-time {
  color: #909399;
  font-size: 13px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* 详情弹窗 */
.notice-detail h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #303133;
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}
.notice-content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .notice-card {
    padding: 16px;
  }
  .notice-title {
    font-size: 15px;
  }
  .pagination {
    justify-content: center;
  }
}
</style>
