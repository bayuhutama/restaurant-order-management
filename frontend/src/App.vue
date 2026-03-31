<template>
  <div class="min-h-screen flex flex-col">
    <Navbar v-if="showNavbar" />
    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '@/components/Navbar.vue'

const route = useRoute()

// Hide the customer navbar on staff and admin routes — they have their own headers
const showNavbar = computed(() =>
  !route.path.startsWith('/staff') && !route.path.startsWith('/admin')
)
</script>
