<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">My Orders</h1>
        <p v-if="tableStore.tableNumber" class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
          Table {{ tableStore.tableNumber }}
        </p>
      </div>
      <button
        v-if="orders.length > 0"
        @click="clearDelivered"
        class="text-xs text-gray-400 hover:text-red-500 dark:hover:text-red-400 transition-colors"
      >
        Clear delivered
      </button>
    </div>

    <!-- No table selected -->
    <div v-if="!tableStore.tableNumber" class="card p-12 text-center">
      <PhTable class="h-12 w-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
      <p class="text-gray-500 dark:text-gray-400 mb-2">No table selected</p>
      <p class="text-sm text-gray-400 dark:text-gray-500 mb-6">Scan the QR code at your table or enter a table number at checkout to see your orders.</p>
      <RouterLink to="/" class="btn-primary">Browse Menu</RouterLink>
    </div>

    <!-- Table selected but no orders placed -->
    <div v-else-if="!loading && orders.length === 0" class="card p-12 text-center">
      <PhReceipt class="h-12 w-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
      <p class="text-gray-500 dark:text-gray-400 mb-2">No orders have been placed</p>
      <p class="text-sm text-gray-400 dark:text-gray-500 mb-6">Table {{ tableStore.tableNumber }}</p>
      <RouterLink to="/" class="btn-primary">Browse Menu</RouterLink>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="space-y-4">
      <div v-for="n in orderNumbers.length" :key="n" class="card p-5 animate-pulse">
        <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-32 mb-3"></div>
        <div class="h-3 bg-gray-100 dark:bg-gray-800 rounded w-48"></div>
      </div>
    </div>

    <!-- Orders list -->
    <div v-else class="space-y-4">
      <div
        v-for="order in orders"
        :key="order.orderNumber"
        class="card p-5 border-l-4"
        :class="borderClass(order.status)"
      >
        <div class="flex items-start justify-between gap-3 mb-3">
          <div>
            <p class="font-mono font-bold text-sm text-gray-900 dark:text-gray-100">{{ order.orderNumber }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ formatDate(order.createdAt) }}</p>
          </div>
          <OrderStatusBadge :status="order.status" />
        </div>

        <!-- Items summary -->
        <div class="text-sm text-gray-600 dark:text-gray-400 mb-3 space-y-0.5">
          <p v-for="item in order.items" :key="item.id" class="truncate">
            {{ item.menuItemName }} × {{ item.quantity }}
          </p>
        </div>

        <div class="flex items-center justify-between">
          <span class="font-semibold text-orange-600">{{ formatRupiah(order.totalAmount) }}</span>
          <div class="flex items-center gap-2">
            <span
              v-if="isActive(order.status)"
              class="flex items-center gap-1 text-xs text-green-600 dark:text-green-400"
            >
              <span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
              Live
            </span>
            <RouterLink :to="`/track/${order.orderNumber}`" class="btn-secondary btn-sm text-xs">
              View Details
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { orderApi, tableSessionApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { useOrdersStore } from '@/stores/orders'
import { useTableStore } from '@/stores/table'
import { useWebSocket } from '@/composables/useWebSocket'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { PhReceipt, PhTable } from '@phosphor-icons/vue'

const ordersStore = useOrdersStore()
const tableStore = useTableStore()
const orders = ref([])
const loading = ref(true)

const orderNumbers = computed(() =>
  tableStore.tableNumber ? ordersStore.getNumbersForTable(tableStore.tableNumber) : []
)

const ACTIVE_STATUSES = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY']

const BORDER = {
  PENDING:   'border-yellow-400',
  CONFIRMED: 'border-blue-400',
  PREPARING: 'border-purple-400',
  READY:     'border-green-400',
  DELIVERED: 'border-gray-200 dark:border-gray-700',
  CANCELLED: 'border-red-300',
}

function borderClass(status) { return BORDER[status] || 'border-gray-200' }
function isActive(status) { return ACTIVE_STATUSES.includes(status) }

function formatDate(dt) {
  return new Date(dt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function clearDelivered() {
  const toRemove = orders.value
    .filter(o => o.status === 'DELIVERED' || o.status === 'CANCELLED')
    .map(o => o.orderNumber)
  toRemove.forEach(n => ordersStore.removeOrder(n))
  orders.value = orders.value.filter(o => !toRemove.includes(o.orderNumber))
}

const { connect } = useWebSocket()

function clearTableSession() {
  ordersStore.getNumbersForTable(tableStore.tableNumber).forEach(n => ordersStore.removeOrder(n))
  tableStore.clearTable()
}

async function loadOrders() {
  if (!tableStore.tableNumber) { loading.value = false; return }

  // Only validate the session when there are stored orders for this table.
  // If no orders exist yet (customer hasn't ordered), just show the empty state.
  if (orderNumbers.value.length > 0) {
    try {
      await tableSessionApi.getSession(tableStore.tableNumber)
    } catch {
      // Session ended by staff — clear this table's history
      clearTableSession()
      loading.value = false
      return
    }
  }

  loading.value = true
  const results = await Promise.allSettled(
    orderNumbers.value.map(n => orderApi.trackOrder(n))
  )
  orders.value = results
    .filter(r => r.status === 'fulfilled')
    .map(r => r.value.data)
  loading.value = false
}

onMounted(() => {
  loadOrders()

  connect((client) => {
    orderNumbers.value.forEach(orderNumber => {
      client.subscribe(`/topic/orders/${orderNumber}`, (message) => {
        const updated = JSON.parse(message.body)
        const idx = orders.value.findIndex(o => o.orderNumber === updated.orderNumber)
        if (idx !== -1) orders.value[idx] = updated
      })
    })
  })
})
</script>
