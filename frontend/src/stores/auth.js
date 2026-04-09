import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

/**
 * Pinia store for authentication state.
 * Token and user profile are persisted to localStorage so sessions survive page refreshes.
 *
 * Role helpers:
 * - isAdmin: only ADMIN role
 * - isStaff: STAFF or ADMIN (admins can access staff features too)
 * - isCustomer: only CUSTOMER role
 */
export const useAuthStore = defineStore('auth', () => {
  // Restore session from localStorage on initialisation
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isStaff = computed(() => user.value?.role === 'STAFF' || user.value?.role === 'ADMIN')
  const isCustomer = computed(() => user.value?.role === 'CUSTOMER')

  /** Calls the login API and saves the returned session. */
  async function login(username, password) {
    const res = await authApi.login({ username, password })
    setSession(res.data)
    return res.data
  }

  /** Calls the register API and saves the returned session. */
  async function register(name, username, email, password, phone) {
    const res = await authApi.register({ name, username, email, password, phone })
    setSession(res.data)
    return res.data
  }

  /** Stores token and user profile in state and localStorage. */
  function setSession(data) {
    token.value = data.token
    user.value = { id: data.id, name: data.name, username: data.username, role: data.role }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  /** Clears session state and removes persisted data. */
  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isAuthenticated, isAdmin, isStaff, isCustomer, login, register, logout }
})
