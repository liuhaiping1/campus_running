<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { User, Lock, Phone, Postcard } from '@element-plus/icons-vue'

const router = useRouter()

const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: ''
})
const loading = ref(false)

// 自定义校验：确认密码必须与密码一致
function validateConfirmPassword(_rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单校验规则，与后端 RegisterRequest 字段限制保持一致
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度为 4-32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // confirmPassword 仅前端校验使用，不传给后端
    const { confirmPassword, ...submitData } = form
    await register(submitData)
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { username: form.username } })
  } catch {
    // 错误由请求层统一 ElMessage.error 提示
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
      <h2 class="auth-title">注册账号</h2>
      <p class="auth-subtitle">加入校园万能帮 · 开启便捷跑腿生活</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（4-32位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（6-32位）" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" type="tel" placeholder="手机号（11位）" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-btn" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-link">
        <router-link to="/login">已有账号？去登录</router-link>
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
  padding: 40px 10vw 40px 0;
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
  width: 440px;
  text-align: center;
  border: none;
  border-radius: 16px;
  padding: 36px 32px;
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
  margin: 0 0 28px 0;
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
