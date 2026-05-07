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
/**
 * Customer-facing top navigation bar (hidden on staff/admin routes).
 *
 * Features:
 * - Cart icon with item-count badge; opens CartDrawer on click
 * - My Orders icon with active-order badge (count of PENDING/CONFIRMED/PREPARING/READY orders)
 *
 * Polling: every 30 seconds, refreshStatuses() fetches the current status of
 * all orders for the active table. If the table session is gone (404), it clears
 * the stored orders and resets the table store so the UI reflects the ended session.
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCartStore } from '@/stores/cart'
import { useOrdersStore } from '@/stores/orders'
import { useTableStore } from '@/stores/table'
import { orderApi } from '@/api'
import { useSessionCleanup } from '@/composables/useSessionCleanup'
import CartDrawer from '@/components/CartDrawer.vue'
import { PhShoppingCart, PhForkKnife, PhReceipt } from '@phosphor-icons/vue'

const cart = useCartStore()
const ordersStore = useOrdersStore()
const tableStore = useTableStore()
const { checkAndClean } = useSessionCleanup()
const cartOpen = ref(false)

// Statuses that count as "active" for the orders badge
const ACTIVE_STATUSES = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY']

// Order numbers for the current table (scoped to avoid showing other tables' orders)
const tableOrderNumbers = computed(() =>
  tableStore.tableNumber ? ordersStore.getNumbersForTable(tableStore.tableNumber) : []
)

// Map of orderNumber → current status, refreshed by polling
const orderStatuses = ref({})

/** Number of orders currently in an active (in-progress) state — shown as the badge. */
const activeOrderCount = computed(() =>
  Object.values(orderStatuses.value).filter(s => ACTIVE_STATUSES.includes(s)).length
)

let pollInterval = null

/**
 * Fetches the latest status for each order at the current table.
 * Also checks the session is still OPEN; clears everything if it ended.
 */
async function refreshStatuses() {
  if (!tableStore.tableNumber) { orderStatuses.value = {}; return }
  if (tableOrderNumbers.value.length === 0) { orderStatuses.value = {}; return }

  await checkAndClean(tableStore.tableNumber)
  if (!tableStore.tableNumber) {
    orderStatuses.value = {}
    return
  }

  // Fetch all order statuses in parallel; ignore any individual failures
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
  // Poll every 30 seconds so the badge stays up to date without WebSocket on this component
  pollInterval = setInterval(refreshStatuses, 30_000)
})

onUnmounted(() => clearInterval(pollInterval))
</script>

<style scoped>
.nav-link {
  @apply text-gray-300 hover:text-white transition-colors text-sm font-medium;
}
</style>
