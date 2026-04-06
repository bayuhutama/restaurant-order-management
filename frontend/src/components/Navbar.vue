<template>
  <nav class="bg-gray-900 text-white shadow-lg sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 text-white font-bold text-xl">
          <div class="w-7 h-7 bg-orange-500 rounded-lg flex items-center justify-center flex-shrink-0">
            <PhForkKnife class="w-4 h-4 text-white" weight="bold" />
          </div>
          <span class="tracking-wide">Savoria</span>
        </RouterLink>

        <!-- Nav links -->
        <div class="flex items-center gap-4">
          <RouterLink to="/" class="nav-link">Menu</RouterLink>
        </div>

        <!-- Right side -->
        <div class="flex items-center gap-2">
          <!-- My Orders -->
          <RouterLink
            to="/my-orders"
            class="relative p-2 rounded-lg hover:bg-gray-800 transition-colors"
            title="My Orders"
          >
            <PhReceipt class="h-6 w-6 text-white" />
            <span
              v-if="activeOrderCount > 0"
              class="absolute -top-1 -right-1 bg-orange-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold"
            >
              {{ activeOrderCount }}
            </span>
          </RouterLink>

          <!-- Cart -->
          <button
            @click="cartOpen = true"
            class="relative p-2 rounded-lg hover:bg-gray-800 transition-colors"
          >
            <PhShoppingCart class="h-6 w-6 text-white" />
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCartStore } from '@/stores/cart'
import { useOrdersStore } from '@/stores/orders'
import { useTableStore } from '@/stores/table'
import { orderApi, tableSessionApi } from '@/api'
import CartDrawer from '@/components/CartDrawer.vue'
import { PhShoppingCart, PhForkKnife, PhReceipt } from '@phosphor-icons/vue'

const cart = useCartStore()
const ordersStore = useOrdersStore()
const tableStore = useTableStore()
const cartOpen = ref(false)

const ACTIVE_STATUSES = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY']

const tableOrderNumbers = computed(() =>
  tableStore.tableNumber ? ordersStore.getNumbersForTable(tableStore.tableNumber) : []
)

// Track active order statuses for the badge
const orderStatuses = ref({})

const activeOrderCount = computed(() =>
  Object.values(orderStatuses.value).filter(s => ACTIVE_STATUSES.includes(s)).length
)

let pollInterval = null

async function refreshStatuses() {
  if (!tableStore.tableNumber) { orderStatuses.value = {}; return }
  if (tableOrderNumbers.value.length === 0) { orderStatuses.value = {}; return }

  // Session check only needed when there are stored orders
  try {
    await tableSessionApi.getSession(tableStore.tableNumber)
  } catch {
    ordersStore.getNumbersForTable(tableStore.tableNumber).forEach(n => ordersStore.removeOrder(n))
    tableStore.clearTable()
    orderStatuses.value = {}
    return
  }
  const results = await Promise.allSettled(
    tableOrderNumbers.value.map(n => orderApi.trackOrder(n))
  )
  const map = {}
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      map[tableOrderNumbers.value[i]] = r.value.data.status
    }
  })
  orderStatuses.value = map
}

onMounted(() => {
  refreshStatuses()
  pollInterval = setInterval(refreshStatuses, 30_000)
})

onUnmounted(() => clearInterval(pollInterval))
</script>

<style scoped>
.nav-link {
  @apply text-gray-300 hover:text-white transition-colors text-sm font-medium;
}
</style>
