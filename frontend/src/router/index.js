import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Customer (always guest, no login) ────────────────────────────────
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue') },
    { path: '/payment/:orderNumber', name: 'payment', component: () => import('@/views/PaymentView.vue') },
    { path: '/track/:orderNumber', name: 'track', component: () => import('@/views/OrderTrackingView.vue') },
    { path: '/table/:tableNumber/bill', name: 'table-bill', component: () => import('@/views/TableBillView.vue') },

    // ── Staff ──────────────────────────────────────────────────────────────
    { path: '/staff/login', name: 'staff-login', component: () => import('@/views/staff/StaffLoginView.vue') },
    {
      path: '/staff',
      name: 'staff',
      component: () => import('@/views/staff/StaffDashboard.vue'),
      meta: { requiresRole: 'STAFF' }
    },

    // ── Admin ──────────────────────────────────────────────────────────────
    { path: '/admin/login', name: 'admin-login', component: () => import('@/views/admin/AdminLoginView.vue') },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresRole: 'ADMIN' },
      children: [
        { path: '', redirect: '/admin/menu' },
        { path: 'menu', name: 'admin-menu', component: () => import('@/views/admin/MenuManagement.vue') },
        { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryManagement.vue') },
        { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/OrdersView.vue') },
        { path: 'tables', name: 'admin-tables', component: () => import('@/views/admin/TablesView.vue') }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresRole) {
    if (!auth.isAuthenticated) {
      // Redirect to the matching portal login
      if (to.meta.requiresRole === 'ADMIN') return next({ name: 'admin-login' })
      if (to.meta.requiresRole === 'STAFF') return next({ name: 'staff-login' })
    }
    if (to.meta.requiresRole === 'ADMIN' && !auth.isAdmin) {
      return next({ name: 'admin-login' })
    }
    if (to.meta.requiresRole === 'STAFF' && !auth.isStaff) {
      return next({ name: 'staff-login' })
    }
  }

  next()
})

export default router
