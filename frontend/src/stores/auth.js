import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isStaff = computed(() => user.value?.role === 'STAFF' || user.value?.role === 'ADMIN')
  const isCustomer = computed(() => user.value?.role === 'CUSTOMER')

  async function login(username, password) {
    const res = await authApi.login({ username, password })
    setSession(res.data)
    return res.data
  }

  async function register(name, username, email, password, phone) {
    const res = await authApi.register({ name, username, email, password, phone })
    setSession(res.data)
    return res.data
  }

  function setSession(data) {
    token.value = data.token
    user.value = { id: data.id, name: data.name, username: data.username, role: data.role }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isAuthenticated, isAdmin, isStaff, isCustomer, login, register, logout }
})
