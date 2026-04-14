import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

/**
 * Pinia store for authentication state.
 *
 * Storage choice: sessionStorage (not localStorage). Staff and admin sessions
 * deliberately end when the browser/tab is closed so a shared device (e.g. the
 * kitchen tablet) does not stay logged in after the user walks away. Page
 * refreshes within the same tab still restore the session.
 *
 * Role helpers:
 * - isAdmin: only ADMIN role
 * - isStaff: STAFF or ADMIN (admins can access staff features too)
 * - isCustomer: only CUSTOMER role
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || null)
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

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

  /** Stores token and user profile in state and sessionStorage. */
  function setSession(data) {
    token.value = data.token
    user.value = { id: data.id, name: data.name, username: data.username, role: data.role }
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('user', JSON.stringify(user.value))
  }

  /** Clears session state and removes persisted data. */
  function logout() {
    token.value = null
    user.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
  }

  return { token, user, isAuthenticated, isAdmin, isStaff, isCustomer, login, register, logout }
})
