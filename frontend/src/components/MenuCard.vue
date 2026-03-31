<template>
  <div class="card overflow-hidden hover:shadow-md transition-shadow group flex flex-col h-full">
    <div class="relative overflow-hidden">
      <img
        :src="item.imageUrl || 'https://via.placeholder.com/400x200'"
        :alt="item.name"
        class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
        @error="e => e.target.src = 'https://via.placeholder.com/400x200'"
      />
      <div v-if="!item.available" class="absolute inset-0 bg-black/50 flex items-center justify-center">
        <span class="bg-red-500 text-white px-3 py-1 rounded-full text-sm font-medium">Unavailable</span>
      </div>
    </div>

    <div class="p-4 flex flex-col flex-1">
      <div class="flex items-start justify-between gap-2 mb-1">
        <h3 class="font-semibold text-gray-900 leading-tight">{{ item.name }}</h3>
        <span class="text-orange-600 font-bold text-lg whitespace-nowrap">{{ formatRupiah(item.price) }}</span>
      </div>

      <div class="relative mb-3 flex-1">
        <p v-if="item.description" class="text-gray-500 text-sm line-clamp-2 peer">{{ item.description }}</p>
        <div class="absolute bottom-full left-0 z-10 hidden peer-hover:block w-64 bg-gray-900 text-white text-xs rounded-lg px-3 py-2 shadow-xl leading-relaxed pointer-events-none mb-1">
          {{ item.description }}
          <div class="absolute top-full left-4 border-4 border-transparent border-t-gray-900"></div>
        </div>
      </div>

      <div v-if="quantityInCart === 0">
        <button
          @click="cart.addItem(item)"
          :disabled="!item.available"
          class="btn-primary w-full"
        >
          Add to Cart
        </button>
      </div>
      <div v-else class="flex items-center justify-between bg-orange-50 rounded-lg p-2">
        <button
          @click="cart.updateQuantity(item.id, quantityInCart - 1)"
          class="w-8 h-8 rounded-full bg-white border border-orange-300 hover:bg-orange-100 font-bold text-orange-600"
        >−</button>
        <span class="font-semibold text-orange-700">{{ quantityInCart }} in cart</span>
        <button
          @click="cart.addItem(item)"
          class="w-8 h-8 rounded-full bg-orange-500 hover:bg-orange-600 text-white font-bold"
        >+</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import { formatRupiah } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true }
})

const cart = useCartStore()

const quantityInCart = computed(() => {
  const found = cart.items.find(i => i.id === props.item.id)
  return found ? found.quantity : 0
})
</script>
