<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Tickets, Medal, Message, Goods, ArrowRight, User, List, MapLocation } from '@element-plus/icons-vue'
import { getNoticeList, getCategoryList } from '@/api/system'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatTime } from '@/utils/format'
import { useResponsive } from '@/composables/useResponsive'

const router = useRouter()
const userStore = useUserStore()

const notices = ref([])
const categories = ref([])
const loading = ref(false)
const noticeDetailVisible = ref(false)
const currentNotice = ref(null)
const { isMobile } = useResponsive()

async function fetchNotices() {
  try {
    const data = await getNoticeList({ pageNum: 1, pageSize: 5 })
    notices.value = data.records || []
  } catch {
    notices.value = []
  }
}

async function fetchCategories() {
  try {
    const data = await getCategoryList()
    // 只展示启用状态的分类
    categories.value = (data || []).filter(c => c.categoryStatus === 1)
  } catch {
    categories.value = []
  }
}

function handleNoticeClick(notice) {
  currentNotice.value = notice
  noticeDetailVisible.value = true
}

function handleCategoryClick(categoryId) {
  router.push(`/order/create?categoryId=${categoryId}`)
}

function navigate(path) {
  router.push(path)
}

// 跑腿员点击任务大厅：已认证直接进入，未认证引导先去认证
function handleTaskHall() {
  if (userStore.isRunner) {
    router.push('/runner/hall')
  } else {
    ElMessage.warning('请先完成跑腿员认证')
    router.push('/runner/auth')
  }
}

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchNotices(), fetchCategories()])
  loading.value = false
})
</script>

<template>
  <PageContainer title="首页" subtitle="校园跑腿服务平台">
    <div class="home-content">
      <!-- 欢迎区域 -->
      <div class="welcome-banner">
        <div class="welcome-text">
          <h2>欢迎回来，{{ userStore.realName || userStore.username }}</h2>
          <p>校园跑腿，让校园生活更便捷</p>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" @click="router.push('/order/create')">发布订单</el-button>
          <el-button v-if="userStore.isRunner" type="success" plain @click="router.push('/runner/hall')">任务大厅</el-button>
        </div>
      </div>

      <!-- 快捷入口：根据角色展示 -->
      <div class="quick-entry">
        <div class="entry-card" @click="router.push('/order/create')">
          <el-icon size="28"><Edit /></el-icon>
          <span>发布订单</span>
        </div>
        <div class="entry-card" @click="handleTaskHall">
          <el-icon size="28"><Tickets /></el-icon>
          <span>任务大厅</span>
        </div>
        <div class="entry-card" @click="navigate('/order/my')">
          <el-icon size="28"><List /></el-icon>
          <span>我的订单</span>
        </div>
        <div class="entry-card" @click="navigate('/user/address')">
          <el-icon size="28"><MapLocation /></el-icon>
          <span>地址管理</span>
        </div>
      </div>

      <!-- 分类网格 -->
      <div class="section">
        <h3 class="section-title">任务分类</h3>
        <div v-if="loading" class="category-grid">
          <el-skeleton v-for="i in 4" :key="i" animated class="category-skeleton" />
        </div>
        <EmptyState v-else-if="categories.length === 0" description="暂无任务分类" />
        <div v-else class="category-grid">
          <div
            v-for="category in categories"
            :key="category.id"
            class="category-card"
            @click="handleCategoryClick(category.id)"
          >
            <div class="category-icon">
              <el-icon size="32"><Goods /></el-icon>
            </div>
            <div class="category-info">
              <span class="category-name">{{ category.categoryName }}</span>
              <span class="category-fee">起步价 {{ category.baseFee }} 元</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 公告摘要 -->
      <div class="section">
        <div class="section-header">
          <h3 class="section-title">系统公告</h3>
          <el-button text type="primary" @click="router.push('/notices')">查看全部</el-button>
        </div>
        <div v-if="loading" class="notice-list">
          <el-skeleton v-for="i in 3" :key="i" animated />
        </div>
        <EmptyState v-else-if="notices.length === 0" description="暂无公告" />
        <div v-else class="notice-list">
          <div
            v-for="notice in notices"
            :key="notice.id"
            class="notice-item"
            @click="handleNoticeClick(notice)"
          >
            <StatusTag type="notice_type" :value="notice.noticeType" size="small" />
            <div class="notice-content">
              <span class="notice-title">{{ notice.noticeTitle }}</span>
              <span class="notice-time">{{ formatTime(notice.publishTime) }}</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="公告详情" :width="isMobile ? '95%' : '560px'" :close-on-click-modal="false">
      <div v-if="currentNotice" class="notice-detail">
        <h3>{{ currentNotice.noticeTitle }}</h3>
        <div class="notice-meta">
          <StatusTag type="notice_type" :value="currentNotice.noticeType" size="small" />
          <span>发布于 {{ formatTime(currentNotice.publishTime) }}</span>
        </div>
        <el-divider />
        <p class="notice-body">{{ currentNotice.noticeContent }}</p>
      </div>
      <template #footer>
        <el-button @click="noticeDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped>
.home-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 欢迎 Banner */
.welcome-banner {
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 12px;
  padding: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}
.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}
.welcome-text p {
  margin: 0;
  opacity: 0.9;
}
.welcome-actions {
  display: flex;
  gap: 12px;
}
.welcome-actions .el-button--success.is-plain {
  color: #fff;
  border-color: rgba(255,255,255,0.6);
  background: transparent;
}
.welcome-actions .el-button--success.is-plain:hover {
  background: rgba(255,255,255,0.15);
  border-color: #fff;
}

/* 快捷入口 */
.quick-entry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.entry-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
  color: #409EFF;
  border: 1px solid #f0f0f0;
}
.entry-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.entry-card span {
  font-size: 14px;
  color: #303133;
}

/* 区域标题 */
.section {
  margin-bottom: 8px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

/* 分类网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.category-skeleton {
  height: 100px;
  border-radius: 12px;
}
.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
  border: 1px solid #f0f0f0;
}
.category-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: #409EFF;
}
.category-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409EFF;
  flex-shrink: 0;
}
.category-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.category-fee {
  font-size: 13px;
  color: #909399;
}

/* 公告列表 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.notice-item {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: box-shadow 0.3s;
  border: 1px solid #f0f0f0;
}
.notice-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.notice-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 0;
}
.notice-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.notice-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  margin-left: 12px;
}

/* 公告详情弹窗 */
.notice-detail h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}
.notice-body {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    text-align: center;
    padding: 24px 16px;
    gap: 16px;
  }
  .welcome-text h2 {
    font-size: 20px;
  }
  .welcome-actions {
    width: 100%;
    justify-content: center;
  }
  .quick-entry {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .entry-card {
    padding: 16px 12px;
  }
  .entry-card span {
    font-size: 12px;
  }
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .category-card {
    padding: 16px;
  }
  .category-icon {
    width: 48px;
    height: 48px;
  }
  .notice-item {
    padding: 12px 16px;
  }
  .notice-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .notice-time {
    margin-left: 0;
  }
}
</style>
