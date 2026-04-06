<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">Order Tracking</h1>

    <div v-if="loading" class="card p-8 text-center text-gray-400">
      Loading order...
    </div>

    <div v-else-if="error" class="card p-8 text-center text-red-500">
      {{ error }}
    </div>

    <div v-else-if="order" class="space-y-5">

      <!-- Awaiting payment banner -->
      <div v-if="order.status === 'AWAITING_PAYMENT'" class="card p-5 border-l-4 border-orange-500 bg-orange-50 dark:bg-orange-900/20">
        <p class="font-semibold text-orange-900 dark:text-orange-300 mb-1">Payment Required</p>
        <p class="text-sm text-orange-700 dark:text-orange-400 mb-3">Your order is waiting for payment before it is sent to the kitchen.</p>
        <RouterLink :to="`/payment/${order.orderNumber}`" class="btn-primary block text-center py-2.5">
          Complete Payment — {{ formatRupiah(order.totalAmount) }}
        </RouterLink>
      </div>

      <!-- Cancelled banner -->
      <div v-if="order.status === 'CANCELLED'" class="card p-5 border-l-4 border-red-500 bg-red-50 dark:bg-red-900/20">
        <p class="font-semibold text-red-900 dark:text-red-300 mb-1">Order Cancelled</p>
        <p class="text-sm text-red-700 dark:text-red-400">This order has been cancelled.</p>
      </div>

      <!-- Order header -->
      <div class="card p-5">
        <div class="flex items-start justify-between flex-wrap gap-3">
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wider mb-0.5">Order Number</p>
            <p class="text-xl font-bold font-mono text-gray-900 dark:text-gray-100">{{ order.orderNumber }}</p>
          </div>
          <OrderStatusBadge :status="order.status" />
        </div>
        <div class="flex items-center gap-1.5 text-xs text-green-600 mt-3">
          <span class="w-2 h-2 bg-green-500 rounded-full animate-pulse inline-block"></span>
          Live updates active
        </div>
      </div>

      <!-- Progress stepper -->
      <div v-if="order.status !== 'AWAITING_PAYMENT' && order.status !== 'CANCELLED'" class="card p-5">
        <h2 class="font-semibold mb-5">Order Progress</h2>
        <div class="relative">
          <!-- Vertical connector line -->
          <div class="absolute left-4 top-4 bottom-4 w-0.5 bg-gray-200 dark:bg-gray-600"></div>

          <div class="space-y-0">
            <div v-for="(step, index) in steps" :key="step.status" class="relative flex gap-4 pb-6 last:pb-0">
              <!-- Step circle -->
              <div class="relative z-10 flex-shrink-0">
                <!-- Completed -->
                <div v-if="isCompleted(step.status)" class="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center">
                  <PhCheck class="w-4 h-4 text-white" />
                </div>
                <!-- Active -->
                <div v-else-if="isActive(step.status)" class="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center ring-4 ring-orange-100">
                  <span class="w-2.5 h-2.5 bg-white rounded-full animate-pulse"></span>
                </div>
                <!-- Upcoming -->
                <div v-else class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 border-2 border-gray-200 dark:border-gray-600 flex items-center justify-center">
                  <span class="w-2 h-2 bg-gray-300 dark:bg-gray-500 rounded-full"></span>
                </div>
              </div>

              <!-- Step content -->
              <div class="flex-1 pt-1">
                <p :class="['text-sm font-semibold', isActive(step.status) ? 'text-orange-600' : isCompleted(step.status) ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400 dark:text-gray-500']">
                  {{ step.label }}
                  <span v-if="isActive(step.status)" class="ml-2 text-xs font-normal bg-orange-100 dark:bg-orange-900/40 text-orange-600 dark:text-orange-300 px-2 py-0.5 rounded-full">In progress</span>
                </p>
                <p :class="['text-xs mt-0.5', isActive(step.status) ? 'text-orange-500' : isCompleted(step.status) ? 'text-gray-500 dark:text-gray-400' : 'text-gray-300 dark:text-gray-600']">
                  {{ step.description }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer info -->
      <div class="card p-5">
        <h2 class="font-semibold mb-3">Customer Details</h2>
        <div class="text-sm text-gray-700 dark:text-gray-300 space-y-1.5">
          <div class="flex gap-2">
            <span class="text-gray-400 w-16 shrink-0">Name</span>
            <span>{{ order.customerName || '—' }}</span>
          </div>
          <div class="flex gap-2">
            <span class="text-gray-400 w-16 shrink-0">Phone</span>
            <span>{{ order.customerPhone || '—' }}</span>
          </div>
          <div v-if="order.tableNumber" class="flex gap-2">
            <span class="text-gray-400 w-16 shrink-0">Table</span>
            <span>{{ order.tableNumber }}</span>
          </div>
          <div v-if="order.notes" class="flex gap-2">
            <span class="text-gray-400 w-16 shrink-0">Notes</span>
            <span>{{ order.notes }}</span>
          </div>
        </div>
      </div>

      <!-- Order items -->
      <div class="card p-5">
        <h2 class="font-semibold mb-3">Order Items</h2>
        <div class="space-y-3">
          <div v-for="item in order.items" :key="item.id" class="flex items-center gap-3">
            <img
              :src="item.menuItemImage || 'https://via.placeholder.com/48'"
              :alt="item.menuItemName"
              class="w-12 h-12 object-cover rounded-lg flex-shrink-0"
              @error="e => e.target.src = 'https://via.placeholder.com/48'"
            />
            <div class="flex-1">
              <p class="text-sm font-medium">{{ item.menuItemName }}</p>
              <p class="text-xs text-gray-400">{{ item.quantity }} × {{ formatRupiah(item.unitPrice) }}</p>
            </div>
            <p class="text-sm font-semibold">{{ formatRupiah(item.subtotal) }}</p>
          </div>
        </div>
        <div class="border-t dark:border-gray-700 mt-4 pt-4 flex justify-between font-bold">
          <span>Total</span>
          <span class="text-orange-600">{{ formatRupiah(order.totalAmount) }}</span>
        </div>
      </div>

      <!-- Payment -->
      <div v-if="order.payment" class="card p-5">
        <h2 class="font-semibold mb-3">Payment</h2>
        <div class="text-sm space-y-2">
          <div class="flex justify-between">
            <span class="text-gray-400">Method</span>
            <span class="font-medium">{{ order.payment.method === 'CARD' ? 'Card' : 'Cash' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">Status</span>
            <span :class="order.payment.status === 'PAID' ? 'text-green-600 font-semibold' : 'text-yellow-600 font-semibold'">
              {{ order.payment.status === 'PAID' ? 'Paid' : 'Pending' }}
            </span>
          </div>
          <div v-if="order.payment.transactionId" class="flex justify-between">
            <span class="text-gray-400">Transaction ID</span>
            <span class="font-mono text-xs">{{ order.payment.transactionId }}</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { useWebSocket } from '@/composables/useWebSocket'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { PhCheck } from '@phosphor-icons/vue'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const error = ref('')

const steps = [
  { status: 'PENDING',   label: 'Order Received',   description: 'Your order has been received and is awaiting confirmation from the kitchen.' },
  { status: 'CONFIRMED', label: 'Order Confirmed',   description: 'The kitchen has confirmed your order and will begin preparing it soon.' },
  { status: 'PREPARING', label: 'Being Prepared',    description: 'The kitchen is currently preparing your order.' },
  { status: 'READY',     label: 'Ready to Serve',    description: 'Your order is ready and our staff will bring it to your table shortly.' },
  { status: 'DELIVERED', label: 'Served',              description: 'Your order has been served at your table. Enjoy your meal!' },
]

const statusOrder = steps.map(s => s.status)

function isCompleted(status) {
  if (!order.value) return false
  const current = statusOrder.indexOf(order.value.status)
  const target = statusOrder.indexOf(status)
  return target < current
}

function isActive(status) {
  return order.value?.status === status
}

const { connect } = useWebSocket()

async function loadOrder() {
  loading.value = true
  error.value = ''
  try {
    const res = await orderApi.trackOrder(route.params.orderNumber)
    order.value = res.data
  } catch {
    error.value = 'Order not found. Please check your order number.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOrder()
  connect((client) => {
    client.subscribe(`/topic/orders/${route.params.orderNumber}`, (message) => {
      order.value = JSON.parse(message.body)
    })
  })
})
</script>
