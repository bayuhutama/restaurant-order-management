<template>
  <!-- Staff Header -->
  <header class="bg-gray-900 text-white px-6 py-3 flex items-center justify-between sticky top-0 z-50 shadow-lg">
    <div class="flex items-center gap-3">
      <div class="w-6 h-6 bg-orange-500 rounded-md flex items-center justify-center flex-shrink-0">
        <PhForkKnife class="w-3.5 h-3.5 text-white" weight="bold" />
      </div>
      <span class="font-bold text-lg">Staff Portal</span>
      <span class="flex items-center gap-1 text-xs ml-4" :class="wsConnected ? 'text-green-400' : 'text-red-400'">
        <span class="w-2 h-2 rounded-full" :class="wsConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'"></span>
        {{ wsConnected ? 'Live' : 'Connecting...' }}
      </span>
    </div>
    <div class="flex items-center gap-3">
      <span class="text-sm text-gray-300">{{ auth.user?.name }}</span>
      <button @click="handleLogout" class="btn-secondary btn-sm text-sm">Logout</button>
    </div>
  </header>

  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">Orders</h1>
      </div>
      <div class="flex gap-2">
        <button @click="activeOnly = !activeOnly; loadOrders()" :class="activeOnly ? 'btn-primary btn-sm' : 'btn-secondary btn-sm'">
          {{ activeOnly ? 'Active Only' : 'All Orders' }}
        </button>
        <button @click="loadOrders" class="btn-secondary btn-sm">↺ Refresh</button>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
      <div class="card p-4 text-center">
        <p class="text-2xl font-bold text-yellow-600">{{ countByStatus('PENDING') }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Pending</p>
      </div>
      <div class="card p-4 text-center">
        <p class="text-2xl font-bold text-purple-600">{{ countByStatus('PREPARING') }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Preparing</p>
      </div>
      <div class="card p-4 text-center">
        <p class="text-2xl font-bold text-green-600">{{ countByStatus('READY') }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Ready</p>
      </div>
      <div class="card p-4 text-center">
        <p class="text-2xl font-bold text-gray-600 dark:text-gray-400">{{ countByStatus('DELIVERED') }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Delivered Today</p>
      </div>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">Loading orders...</div>

    <div v-else-if="orders.length === 0" class="text-center py-20 text-gray-400">
      <p>No orders to show</p>
    </div>

    <!-- Orders Grid -->
    <div v-else class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="order in orders"
        :key="order.id"
        class="card p-5 border-l-4"
        :class="borderClass(order.status)"
      >
        <!-- Header -->
        <div class="flex items-start justify-between mb-3">
          <div>
            <p class="font-mono text-sm font-bold">{{ order.orderNumber }}</p>
            <p class="text-xs text-gray-400 dark:text-gray-500">{{ formatTime(order.createdAt) }}</p>
          </div>
          <OrderStatusBadge :status="order.status" />
        </div>

        <!-- Customer -->
        <div class="text-sm text-gray-600 dark:text-gray-400 mb-3">
          <p class="font-medium text-gray-800 dark:text-gray-200">{{ order.customerName || 'Guest' }}</p>
          <p v-if="order.tableNumber" class="text-xs">Table {{ order.tableNumber }}</p>
          <p v-if="order.customerPhone" class="text-xs">{{ order.customerPhone }}</p>
        </div>

        <!-- Items -->
        <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-3 mb-3 space-y-1">
          <div v-for="item in order.items" :key="item.id" class="text-xs flex justify-between">
            <span>{{ item.menuItemName }} × {{ item.quantity }}</span>
            <span class="text-gray-500 dark:text-gray-400">{{ formatRupiah(item.subtotal) }}</span>
          </div>
          <div class="border-t dark:border-gray-600 pt-1 flex justify-between font-semibold text-xs">
            <span>Total</span>
            <span class="text-orange-600">{{ formatRupiah(order.totalAmount) }}</span>
          </div>
        </div>

        <div v-if="order.notes" class="text-xs text-gray-500 mb-3 italic">{{ order.notes }}</div>

        <!-- Payment badge -->
        <div class="flex items-center gap-2 mb-4">
          <span class="text-xs bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">{{ order.payment?.method }}</span>
          <span
            class="text-xs px-2 py-0.5 rounded-full"
            :class="order.payment?.status === 'PAID' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'"
          >
            {{ order.payment?.status }}
          </span>
        </div>

        <!-- Status Actions -->
        <div v-if="order.status !== 'DELIVERED' && order.status !== 'CANCELLED'" class="flex flex-wrap gap-2">
          <button
            v-for="action in nextActions(order.status)"
            :key="action.status"
            @click="updateStatus(order.id, action.status)"
            :class="['btn btn-sm text-xs', action.class]"
            :disabled="updating === order.id"
          >
            {{ action.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { staffApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { PhForkKnife } from '@phosphor-icons/vue'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/staff/login')
}

const orders = ref([])
const loading = ref(true)
const activeOnly = ref(true)
const updating = ref(null)
const wsConnected = ref(false)

const { connect } = useWebSocket()

const STATUS_ACTIONS = {
  PENDING:   [{ status: 'CONFIRMED', label: 'Confirm Order', class: 'btn-primary' }],
  CONFIRMED: [{ status: 'PREPARING', label: 'Start Cooking', class: 'btn-primary' }],
  PREPARING: [{ status: 'READY', label: 'Mark Ready', class: 'btn-success' }],
  READY:     [{ status: 'DELIVERED', label: 'Mark Served', class: 'btn-success' }]
}

const BORDER = {
  PENDING: 'border-yellow-400',
  CONFIRMED: 'border-blue-400',
  PREPARING: 'border-purple-400',
  READY: 'border-green-400',
  DELIVERED: 'border-gray-300',
  CANCELLED: 'border-red-300'
}

function nextActions(status) { return STATUS_ACTIONS[status] || [] }
function borderClass(status) { return BORDER[status] || 'border-gray-200' }
function countByStatus(s) { return orders.value.filter(o => o.status === s).length }
function formatTime(dt) { return new Date(dt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }

async function loadOrders() {
  loading.value = true
  try {
    const res = await staffApi.getOrders(activeOnly.value)
    orders.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function updateStatus(orderId, status) {
  updating.value = orderId
  try {
    await staffApi.updateStatus(orderId, status)
  } catch (e) {
    console.error(e)
  } finally {
    updating.value = null
  }
}

onMounted(() => {
  loadOrders()

  connect((client) => {
    wsConnected.value = true
    client.subscribe('/topic/orders', (message) => {
      const updated = JSON.parse(message.body)
      const idx = orders.value.findIndex(o => o.id === updated.id)
      if (idx !== -1) {
        orders.value[idx] = updated
        // Remove from active list if no longer active
        if (activeOnly.value && ['DELIVERED', 'CANCELLED'].includes(updated.status)) {
          orders.value.splice(idx, 1)
        }
      } else if (!activeOnly.value || !['DELIVERED', 'CANCELLED'].includes(updated.status)) {
        orders.value.unshift(updated)
      }
    })
  })
})
</script>
