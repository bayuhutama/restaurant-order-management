import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

/**
 * Pinia store for staff/admin authentication state.
 *
 * Customers are not authenticated — they order as guests via QR scan, so
 * this store only ever holds STAFF or ADMIN sessions.
 *
 * Storage choice: sessionStorage (not localStorage). Sessions deliberately
 * end when the browser/tab is closed so a shared device (e.g. the kitchen
 * tablet) does not stay logged in after the user walks away. Page refreshes
 * within the same tab still restore the session.
 *
 * Role helpers:
 * - isAdmin: only ADMIN role
 * - isStaff: STAFF or ADMIN (admins can access staff features too)
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || null)
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isStaff = computed(() => user.value?.role === 'STAFF' || user.value?.role === 'ADMIN')

  /** Calls the login API and saves the returned session. */
  async function login(username, password) {
    const res = await authApi.login({ username, password })
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

  return { token, user, isAuthenticated, isAdmin, isStaff, login, logout }
})
