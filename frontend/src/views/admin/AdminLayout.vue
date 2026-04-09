<template>
  <div class="flex min-h-screen flex-col">
    <!-- Admin Header -->
    <header class="bg-gray-950 text-white px-4 sm:px-6 py-3 flex items-center justify-between sticky top-0 z-50 shadow-lg flex-shrink-0">
      <div class="flex items-center gap-3">
        <!-- Hamburger (mobile only) -->
        <button @click="sidebarOpen = !sidebarOpen" class="lg:hidden p-1 rounded hover:bg-gray-800 transition-colors">
          <PhList class="w-5 h-5" />
        </button>
        <div class="w-6 h-6 bg-orange-500 rounded-md flex items-center justify-center flex-shrink-0">
          <PhForkKnife class="w-3.5 h-3.5 text-white" weight="bold" />
        </div>
        <span class="font-bold text-lg">Admin Portal</span>
      </div>
      <div class="flex items-center gap-3">
        <span class="hidden sm:inline text-sm text-gray-300">{{ auth.user?.name }}</span>
        <button @click="handleLogout" class="btn-secondary btn-sm text-sm">Logout</button>
      </div>
    </header>

    <div class="flex flex-1 overflow-hidden">
      <!-- Mobile overlay -->
      <div
        v-if="sidebarOpen"
        class="fixed inset-0 bg-black/50 z-30 lg:hidden"
        @click="sidebarOpen = false"
      ></div>

      <!-- Sidebar -->
      <aside
        :class="[
          'bg-gray-900 text-white flex-shrink-0 z-40 transition-transform duration-200',
          'fixed lg:static inset-y-0 left-0 w-56 top-0 pt-14 lg:pt-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        ]"
      >
        <nav class="p-4 space-y-1">
          <p class="text-xs text-gray-500 uppercase tracking-wider px-3 mb-3">Admin Panel</p>
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-gray-300 hover:text-white hover:bg-gray-800 transition-colors"
            active-class="bg-orange-600 text-white hover:bg-orange-600"
            @click="sidebarOpen = false"
          >
            {{ link.label }}
          </RouterLink>
        </nav>
      </aside>

      <!-- Main content -->
      <main class="flex-1 bg-gray-50 dark:bg-gray-900 overflow-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
/**
 * Admin portal shell layout.
 * Provides a sticky header with the logout button and a collapsible sidebar.
 * On desktop (lg+) the sidebar is always visible.
 * On mobile, a hamburger button toggles the sidebar with a backdrop overlay.
 * All admin child views are rendered via RouterView in the main content area.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PhForkKnife, PhList } from '@phosphor-icons/vue'

const auth = useAuthStore()
const router = useRouter()
const sidebarOpen = ref(false)  // controls mobile sidebar visibility

function handleLogout() {
  auth.logout()
  router.push('/admin/login')
}

// Sidebar navigation links — order determines display order
const navLinks = [
  { to: '/admin/menu', label: 'Menu Items' },
  { to: '/admin/categories', label: 'Categories' },
  { to: '/admin/orders', label: 'All Orders' },
  { to: '/admin/tables', label: 'Table QR Codes' }
]
</script>
