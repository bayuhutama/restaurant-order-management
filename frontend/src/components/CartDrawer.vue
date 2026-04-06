<template>
  <!-- Overlay -->
  <div class="fixed inset-0 bg-black/50 z-50 flex justify-end" @click.self="$emit('close')">
    <div class="bg-white dark:bg-gray-800 w-full max-w-md h-full flex flex-col shadow-2xl">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b dark:border-gray-700">
        <h2 class="text-lg font-bold">Your Cart ({{ cart.itemCount }})</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"><PhX class="h-5 w-5" /></button>
      </div>

      <!-- Items -->
      <div class="flex-1 overflow-y-auto p-4 space-y-3">
        <div v-if="cart.items.length === 0" class="text-center text-gray-400 py-12">
          <p>Your cart is empty</p>
        </div>

        <div
          v-for="item in cart.items"
          :key="item.id"
          class="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
        >
          <img
            :src="item.imageUrl || '/placeholder.jpg'"
            :alt="item.name"
            class="w-14 h-14 object-cover rounded-lg flex-shrink-0"
            @error="e => e.target.src = 'https://via.placeholder.com/56'"
          />
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm truncate">{{ item.name }}</p>
            <p class="text-orange-600 text-sm font-semibold">{{ formatRupiah(item.price) }}</p>
          </div>
          <div class="flex items-center gap-1">
            <button
              @click="cart.updateQuantity(item.id, item.quantity - 1)"
              class="w-9 h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center text-sm font-bold"
            >−</button>
            <span class="w-7 text-center text-sm font-medium">{{ item.quantity }}</span>
            <button
              @click="cart.updateQuantity(item.id, item.quantity + 1)"
              class="w-9 h-9 rounded-full bg-orange-100 hover:bg-orange-200 text-orange-700 flex items-center justify-center text-sm font-bold"
            >+</button>
          </div>
          <button
            @click="cart.removeItem(item.id)"
            class="text-red-400 hover:text-red-600 ml-1"
          >
            <PhTrash class="h-4 w-4" />
          </button>
        </div>
      </div>

      <!-- Footer -->
      <div class="border-t dark:border-gray-700 p-4 space-y-3">
        <div class="flex justify-between text-lg font-bold">
          <span>Total</span>
          <span class="text-orange-600">{{ formatRupiah(cart.total) }}</span>
        </div>
        <button
          @click="goToCheckout"
          :disabled="cart.items.length === 0"
          class="btn-primary w-full py-3"
        >
          Proceed to Checkout
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useCartStore } from '@/stores/cart'
import { useRouter } from 'vue-router'
import { formatRupiah } from '@/utils/format'
import { PhX, PhTrash } from '@phosphor-icons/vue'

const cart = useCartStore()
const router = useRouter()
const emit = defineEmits(['close'])

function goToCheckout() {
  emit('close')
  router.push('/checkout')
}
</script>
