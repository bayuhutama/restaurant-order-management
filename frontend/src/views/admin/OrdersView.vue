<template>
  <div class="p-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
      <h1 class="text-xl font-bold">All Orders</h1>
      <div class="flex gap-2">
        <div class="relative">
          <PhMagnifyingGlass class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search orders..."
            class="input pl-9 text-sm w-full sm:w-56 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100 dark:placeholder-gray-500"
          />
          <button v-if="searchQuery" @click="searchQuery = ''" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
            <PhX class="h-3.5 w-3.5" />
          </button>
        </div>
        <button @click="load" class="btn-secondary btn-sm inline-flex items-center gap-1"><PhArrowCounterClockwise class="h-4 w-4" />Refresh</button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">Loading...</div>

    <div v-else class="bg-white dark:bg-gray-800 rounded-xl border dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm min-w-[700px]">
          <thead class="bg-gray-50 dark:bg-gray-700/50 border-b dark:border-gray-700">
            <tr>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Order #</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Customer</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Items</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Total</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Payment</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Status</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Date</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y dark:divide-gray-700">
            <tr v-for="order in filteredOrders" :key="order.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
              <td class="px-4 py-3 font-mono text-xs">{{ order.orderNumber }}</td>
              <td class="px-4 py-3">
                <p class="font-medium">{{ order.customerName || 'Guest' }}</p>
                <p class="text-xs text-gray-400">{{ order.customerPhone || '' }}</p>
              </td>
              <td class="px-4 py-3 text-gray-600 dark:text-gray-400">
                {{ order.items.length }} item(s)
              </td>
              <td class="px-4 py-3 font-semibold text-orange-600">{{ formatRupiah(order.totalAmount) }}</td>
              <td class="px-4 py-3">
                <div class="text-xs space-y-1">
                  <span class="bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">{{ order.payment?.method }}</span>
                  <br/>
                  <span :class="order.payment?.status === 'PAID' ? 'text-green-600' : 'text-yellow-600'">
                    {{ order.payment?.status }}
                  </span>
                </div>
              </td>
              <td class="px-4 py-3"><OrderStatusBadge :status="order.status" /></td>
              <td class="px-4 py-3 text-xs text-gray-400">{{ formatDate(order.createdAt) }}</td>
              <td class="px-4 py-3">
                <select
                  v-if="order.status !== 'DELIVERED' && order.status !== 'CANCELLED'"
                  :value="order.status"
                  @change="updateStatus(order.id, $event.target.value)"
                  class="text-xs border dark:border-gray-600 rounded px-2 py-1 bg-white dark:bg-gray-700 dark:text-gray-200"
                >
                  <option value="PENDING">PENDING</option>
                  <option value="CONFIRMED">CONFIRMED</option>
                  <option value="PREPARING">PREPARING</option>
                  <option value="READY">READY</option>
                  <option value="DELIVERED">DELIVERED</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>
                <span v-else class="text-xs text-gray-400">—</span>
              </td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400">
                {{ searchQuery ? 'No orders match your search' : 'No orders found' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminOrderApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { PhArrowCounterClockwise, PhMagnifyingGlass, PhX } from '@phosphor-icons/vue'

const orders = ref([])
const loading = ref(true)
const searchQuery = ref('')

const filteredOrders = computed(() => {
  if (!searchQuery.value.trim()) return orders.value
  const q = searchQuery.value.trim().toLowerCase()
  return orders.value.filter(o =>
    o.orderNumber.toLowerCase().includes(q) ||
    (o.customerName && o.customerName.toLowerCase().includes(q)) ||
    (o.customerPhone && o.customerPhone.includes(q)) ||
    (o.tableNumber && o.tableNumber.toString().includes(q))
  )
})

async function load() {
  loading.value = true
  try {
    const res = await adminOrderApi.getAll()
    orders.value = res.data
  } finally {
    loading.value = false
  }
}

async function updateStatus(orderId, status) {
  try {
    const res = await adminOrderApi.updateStatus(orderId, status)
    const idx = orders.value.findIndex(o => o.id === orderId)
    if (idx !== -1) orders.value[idx] = res.data
  } catch (e) {
    alert('Failed to update status')
  }
}

function formatDate(dt) {
  return new Date(dt).toLocaleString()
}

onMounted(load)
</script>
