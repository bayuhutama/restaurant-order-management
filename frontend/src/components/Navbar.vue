<template>
  <nav class="bg-gray-900 text-white shadow-lg sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 text-white font-bold text-xl">
          <div class="w-7 h-7 bg-orange-500 rounded-sm flex-shrink-0"></div>
          <span class="tracking-wide">Savoria</span>
        </RouterLink>

        <!-- Nav links -->
        <div class="flex items-center gap-4">
          <RouterLink to="/" class="nav-link">Menu</RouterLink>
        </div>

        <!-- Right side -->
        <div class="flex items-center gap-3">
          <button
            @click="cartOpen = true"
            class="relative p-2 rounded-lg hover:bg-gray-800 transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <span
              v-if="cart.itemCount > 0"
              class="absolute -top-1 -right-1 bg-orange-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold"
            >
              {{ cart.itemCount }}
            </span>
          </button>
        </div>
      </div>
    </div>
  </nav>

  <CartDrawer v-if="cartOpen" @close="cartOpen = false" />
</template>

<script setup>
import { ref } from 'vue'
import { useCartStore } from '@/stores/cart'
import CartDrawer from '@/components/CartDrawer.vue'

const cart = useCartStore()
const cartOpen = ref(false)
</script>

<style scoped>
.nav-link {
  @apply text-gray-300 hover:text-white transition-colors text-sm font-medium;
}
</style>
