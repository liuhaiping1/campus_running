<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

// 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  if (route.query.username) {
    form.username = route.query.username
  }
})

/** 登录后跳转：ADMIN → 后台，其它 → 个人中心 */
function getRedirectPath(roles) {
  if (roles.includes('ADMIN')) return '/admin/dashboard'
  return '/profile'
}

async function handleLogin() {
  // 先校验表单，validate 失败抛异常不进入 try
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login(form)
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    // 优先使用 URL 中的 redirect 参数，否则按角色跳转
    const redirect = route.query.redirect
    router.push(redirect ? decodeURIComponent(redirect) : getRedirectPath(data.roles))
  } catch {
    // 错误由请求层统一 ElMessage.error 提示，这里只确保 loading 关闭
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <!-- 背景装饰圆 -->
    <div class="deco-circle deco-top-right"></div>
    <div class="deco-circle deco-bottom-left"></div>

    <el-card class="auth-card" shadow="never">
      <h2 class="auth-title">校园万能帮</h2>
      <p class="auth-subtitle">让校园生活更便捷 · 跑腿服务平台</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-btn" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-link">
        <router-link to="/register">没有账号？立即注册</router-link>
        <span class="forgot-link" @click="ElMessage.info('请联系管理员重置密码')">忘记密码？</span>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* ===== 页面背景 ===== */
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10vw;
  background:
    linear-gradient(135deg, rgba(240, 244, 250, 0.35) 0%, rgba(228, 239, 255, 0.35) 100%),
    url('/login.png') center / cover no-repeat;
  position: relative;
  overflow: hidden;
}

/* ===== 背景装饰圆 ===== */
.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  pointer-events: none;
}

.deco-top-right {
  width: 520px;
  height: 520px;
  top: -180px;
  right: -120px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
}

.deco-bottom-left {
  width: 400px;
  height: 400px;
  bottom: -140px;
  left: -100px;
  background: linear-gradient(135deg, #67C23A, #95d475);
}

/* ===== 核心卡片（毛玻璃质感） ===== */
.auth-card {
  width: 420px;
  text-align: center;
  border: none;
  border-radius: 16px;
  padding: 40px 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.08);
  position: relative;
  z-index: 1;
}

/* ===== 标题与排版 ===== */
.auth-title {
  margin: 0 0 8px 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #303133;
}

.auth-subtitle {
  margin: 0 0 32px 0;
  font-size: 13px;
  color: #909399;
  letter-spacing: 0.5px;
}

/* ===== 输入框优化（极简风） ===== */
:deep(.el-input__wrapper) {
  border: none;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  background: #f8fafc;
  border-radius: 8px;
  transition: background 0.2s, box-shadow 0.2s;
}

:deep(.el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c6d0db inset;
}

:deep(.el-input.is-focus .el-input__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px #409EFF inset;
}

/* ===== 提交按钮 ===== */
.auth-btn {
  width: 100%;
  border-radius: 8px;
  letter-spacing: 2px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  margin-top: 4px;
}

/* ===== 底部链接 ===== */
.auth-link {
  margin-top: 8px;
  font-size: 14px;
}

.auth-link a {
  color: #409EFF;
  text-decoration: none;
}

.auth-link a:hover {
  color: #66b1ff;
}

.forgot-link {
  margin-left: 16px;
  color: #909399;
  font-size: 14px;
  cursor: pointer;
}

.forgot-link:hover {
  color: #409EFF;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .deco-circle {
    display: none;
  }

  .auth-wrapper {
    padding-right: 0;
    justify-content: center;
  }

  .auth-card {
    width: 100%;
    min-height: 100vh;
    border-radius: 0;
    box-shadow: none;
    padding: 48px 28px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    background:
      linear-gradient(135deg, rgba(240, 244, 250, 0.35) 0%, rgba(228, 239, 255, 0.35) 100%),
      url('/login.png') center / cover no-repeat;
  }
}
</style>
