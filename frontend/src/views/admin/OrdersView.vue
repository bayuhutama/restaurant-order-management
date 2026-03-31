<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold">All Orders</h1>
      <button @click="load" class="btn-secondary btn-sm">↺ Refresh</button>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">Loading...</div>

    <div v-else class="bg-white rounded-xl border overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Order #</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Customer</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Items</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Total</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Payment</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Status</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Date</th>
              <th class="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y">
            <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 font-mono text-xs">{{ order.orderNumber }}</td>
              <td class="px-4 py-3">
                <p class="font-medium">{{ order.customerName || 'Guest' }}</p>
                <p class="text-xs text-gray-400">{{ order.customerPhone || '' }}</p>
              </td>
              <td class="px-4 py-3 text-gray-600">
                {{ order.items.length }} item(s)
              </td>
              <td class="px-4 py-3 font-semibold text-orange-600">{{ formatRupiah(order.totalAmount) }}</td>
              <td class="px-4 py-3">
                <div class="text-xs space-y-1">
                  <span class="bg-gray-100 px-2 py-0.5 rounded-full">{{ order.payment?.method }}</span>
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
                  class="text-xs border rounded px-2 py-1"
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
            <tr v-if="orders.length === 0">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400">No orders found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminOrderApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'

const orders = ref([])
const loading = ref(true)

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
