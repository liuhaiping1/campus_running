import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const roles = ref([])

  const isLoggedIn = computed(() => !!token.value)
  const isRunner = computed(() => roles.value.includes('RUNNER'))
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  function login(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    roles.value = data.roles || []
    localStorage.setItem('token', data.token)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    roles.value = []
    localStorage.removeItem('token')
  }

  return { token, userId, username, realName, roles, isLoggedIn, isRunner, isAdmin, login, logout }
})
