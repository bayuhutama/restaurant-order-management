<template>
  <div class="flex min-h-screen flex-col">
    <!-- Admin Header -->
    <header class="bg-gray-950 text-white px-6 py-3 flex items-center justify-between sticky top-0 z-50 shadow-lg flex-shrink-0">
      <div class="flex items-center gap-3">
        <div class="w-6 h-6 bg-orange-500 rounded-md flex items-center justify-center flex-shrink-0">
            <PhForkKnife class="w-3.5 h-3.5 text-white" weight="bold" />
          </div>
        <span class="font-bold text-lg">Admin Portal</span>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-sm text-gray-300">{{ auth.user?.name }}</span>
        <button @click="handleLogout" class="btn-secondary btn-sm text-sm">Logout</button>
      </div>
    </header>

    <div class="flex flex-1 overflow-hidden">
      <!-- Sidebar -->
      <aside class="w-56 bg-gray-900 text-white flex-shrink-0">
        <nav class="p-4 space-y-1">
          <p class="text-xs text-gray-500 uppercase tracking-wider px-3 mb-3">Admin Panel</p>
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-gray-300 hover:text-white hover:bg-gray-800 transition-colors"
            active-class="bg-orange-600 text-white hover:bg-orange-600"
          >
            {{ link.label }}
          </RouterLink>
        </nav>
      </aside>

      <!-- Main content -->
      <main class="flex-1 bg-gray-50 overflow-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PhForkKnife } from '@phosphor-icons/vue'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/admin/login')
}

const navLinks = [
  { to: '/admin/menu', label: 'Menu Items' },
  { to: '/admin/categories', label: 'Categories' },
  { to: '/admin/orders', label: 'All Orders' },
  { to: '/admin/tables', label: 'Table QR Codes' }
]
</script>
