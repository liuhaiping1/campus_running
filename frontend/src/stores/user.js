import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const USER_INFO_KEY = 'userInfo'

function normalizeUserId(userId) {
  return userId == null ? null : String(userId)
}

function loadUserInfo() {
  try {
    const stored = localStorage.getItem(USER_INFO_KEY)
    if (!stored) return { userId: null, username: '', realName: '', roles: [] }
    const data = JSON.parse(stored)
    return { ...data, userId: normalizeUserId(data.userId) }
  } catch {
    return { userId: null, username: '', realName: '', roles: [] }
  }
}

function saveUserInfo(data) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify({
    userId: normalizeUserId(data.userId),
    username: data.username,
    realName: data.realName,
    roles: data.roles || []
  }))
}

function clearUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(loadUserInfo())

  const userId = computed(() => userInfo.value.userId)
  const username = computed(() => userInfo.value.username)
  const realName = computed(() => userInfo.value.realName)
  const roles = computed(() => userInfo.value.roles || [])

  const isLoggedIn = computed(() => !!token.value)
  const isRunner = computed(() => roles.value.includes('RUNNER'))
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  // 学生角色：只要不是 ADMIN/RUNNER 即为学生，或显式包含 STUDENT
  const isStudent = computed(() => roles.value.includes('STUDENT') || (!isAdmin.value && !isRunner.value))

  // 登录成功后保存完整信息（token + 用户信息）
  function setLogin(data) {
    token.value = data.token
    userInfo.value = {
      userId: normalizeUserId(data.userId),
      username: data.username,
      realName: data.realName,
      roles: data.roles || []
    }
    localStorage.setItem('token', data.token)
    saveUserInfo(data)
  }

  // 仅设置 token（用于手动恢复或外部设置）
  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  }

  // 兼容旧方法名
  function login(data) {
    setLogin(data)
  }

  // 清除所有用户状态（退出登录），同步清理 localStorage
  function clearUser() {
    token.value = ''
    userInfo.value = { userId: null, username: '', realName: '', roles: [] }
    localStorage.removeItem('token')
    clearUserInfo()
  }

  // 兼容旧方法名
  function logout() {
    clearUser()
  }

  function setUserInfo(data) {
    userInfo.value = {
      userId: normalizeUserId(data.userId),
      username: data.username,
      realName: data.realName,
      roles: data.roles || []
    }
    saveUserInfo(data)
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  return { token, userId, username, realName, roles, isLoggedIn, isRunner, isAdmin, isStudent, setLogin, setToken, clearUser, login, logout, setUserInfo, hasRole }
})
