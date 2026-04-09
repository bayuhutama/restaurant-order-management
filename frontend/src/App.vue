<template>
  <div class="min-h-screen flex flex-col">
    <Navbar v-if="showNavbar" />
    <main class="flex-1">
      <RouterView />
    </main>
    <AppDialog />
  </div>
</template>

<script setup>
/**
 * Root application shell.
 * Renders the customer Navbar conditionally — staff and admin views
 * have their own headers, so the shared Navbar is hidden on those routes.
 * AppDialog is always mounted here (via Teleport) so dialogs can appear
 * above all other content regardless of which view is active.
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import AppDialog from '@/components/AppDialog.vue'

const route = useRoute()

// Hide the customer navbar on staff and admin routes — they have their own headers
const showNavbar = computed(() =>
  !route.path.startsWith('/staff') && !route.path.startsWith('/admin')
)
</script>
