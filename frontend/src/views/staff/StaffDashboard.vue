<template>
  <!-- Staff Header -->
  <header class="bg-gray-900 text-white px-4 sm:px-6 py-3 flex items-center justify-between sticky top-0 z-50 shadow-lg">
    <div class="flex items-center gap-3">
      <div class="w-7 h-7 bg-orange-500 rounded-lg flex items-center justify-center flex-shrink-0">
        <PhForkKnife class="w-4 h-4 text-white" weight="bold" />
      </div>
      <span class="font-bold text-lg hidden sm:inline">Staff Portal</span>
      <span class="flex items-center gap-1.5 text-xs" :class="wsConnected ? 'text-green-400' : 'text-red-400'">
        <span class="w-2 h-2 rounded-full flex-shrink-0" :class="wsConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'"></span>
        <span class="hidden sm:inline">{{ wsConnected ? 'Live' : 'Connecting...' }}</span>
      </span>
    </div>

    <div class="flex items-center gap-2">
      <!-- Search -->
      <div class="relative">
        <PhMagnifyingGlass class="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400 pointer-events-none" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search..."
          class="bg-gray-800 border border-gray-700 text-gray-100 placeholder-gray-500 rounded-lg pl-8 pr-8 py-1.5 text-sm w-36 sm:w-48 focus:outline-none focus:ring-1 focus:ring-orange-500"
        />
        <button v-if="searchQuery" @click="searchQuery = ''" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-200">
          <PhX class="h-3.5 w-3.5" />
        </button>
      </div>
      <button @click="loadOrders(); loadSessions()" class="p-1.5 rounded-lg hover:bg-gray-700 text-gray-300 transition-colors" title="Refresh">
        <PhArrowCounterClockwise class="h-4 w-4" />
      </button>
      <span class="text-sm text-gray-400 hidden sm:inline">{{ auth.user?.name }}</span>
      <button @click="handleLogout" class="btn-secondary btn-sm text-xs">Logout</button>
    </div>
  </header>

  <div class="max-w-7xl mx-auto px-3 sm:px-4 py-5">

    <!-- Active Tables -->
    <div v-if="openSessions.length > 0" class="mb-5">
      <div class="flex items-center gap-2 mb-2.5">
        <PhTable class="h-4 w-4 text-gray-400" />
        <span class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Active Tables</span>
        <span class="bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 text-xs font-bold px-1.5 rounded-full">{{ openSessions.length }}</span>
      </div>
      <div class="flex gap-3 overflow-x-auto pb-1 scrollbar-hide">
        <div
          v-for="session in openSessions"
          :key="session.id"
          class="flex-shrink-0 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-xl p-3 flex flex-col gap-1 w-36"
        >
          <div class="flex items-center justify-between">
            <span class="font-bold text-base">T{{ session.tableNumber }}</span>
            <span class="text-xs text-gray-400">{{ session.orderCount }}×</span>
          </div>
          <p class="text-xs font-semibold text-orange-600">{{ formatRupiah(session.totalAmount) }}</p>
          <button
            @click="endSession(session.tableNumber)"
            :disabled="closingTable === session.tableNumber"
            class="mt-1 text-xs text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 border border-red-200 dark:border-red-800 rounded-lg py-1 transition-colors disabled:opacity-50"
          >
            {{ closingTable === session.tableNumber ? '...' : 'End Session' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Status Tabs -->
    <div class="flex gap-1 overflow-x-auto pb-0 mb-5 scrollbar-hide">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        @click="activeTab = tab.value"
        :class="[
          'flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-colors flex-shrink-0',
          activeTab === tab.value
            ? `${tab.activeClass} shadow-sm`
            : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
        ]"
      >
        {{ tab.label }}
        <span
          v-if="tab.count > 0"
          :class="['text-xs font-bold px-1.5 py-0.5 rounded-full min-w-[20px] text-center', activeTab === tab.value ? tab.badgeActiveClass : tab.badgeClass]"
        >
          {{ tab.count }}
        </span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="n in 6" :key="n" class="card p-5 animate-pulse space-y-3">
        <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24"></div>
        <div class="h-3 bg-gray-100 dark:bg-gray-800 rounded w-36"></div>
        <div class="h-16 bg-gray-100 dark:bg-gray-800 rounded"></div>
        <div class="h-9 bg-gray-200 dark:bg-gray-700 rounded"></div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="displayedOrders.length === 0" class="text-center py-20 text-gray-400">
      <PhCheckCircle class="h-10 w-10 mx-auto mb-3 text-gray-300 dark:text-gray-600" />
      <p>{{ searchQuery ? 'No orders match your search' : 'No orders here' }}</p>
    </div>

    <!-- Orders Grid -->
    <div v-else class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="order in displayedOrders"
        :key="order.id"
        class="card flex flex-col border-t-4"
        :class="borderTopClass(order.status)"
      >
        <!-- Card header -->
        <div class="px-4 pt-4 pb-3 flex items-start justify-between gap-2">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm flex-shrink-0"
              :class="tableBgClass(order.status)">
              T{{ order.tableNumber || '?' }}
            </div>
            <div>
              <p class="font-semibold text-sm text-gray-900 dark:text-gray-100">{{ order.customerName || 'Guest' }}</p>
              <p class="text-xs text-gray-400">{{ formatTime(order.createdAt) }} &middot; {{ timeAgo(order.createdAt) }}</p>
            </div>
          </div>
          <OrderStatusBadge :status="order.status" />
        </div>

        <!-- Items -->
        <div class="mx-4 mb-3 bg-gray-50 dark:bg-gray-700/60 rounded-xl px-3 py-2.5 space-y-1.5">
          <div v-for="item in order.items" :key="item.id" class="flex justify-between text-xs">
            <span class="text-gray-700 dark:text-gray-300">
              <span class="font-semibold text-gray-900 dark:text-white">{{ item.quantity }}×</span>
              {{ item.menuItemName }}
            </span>
            <span class="text-gray-400 ml-2 flex-shrink-0">{{ formatRupiah(item.subtotal) }}</span>
          </div>
          <div class="border-t dark:border-gray-600 pt-1.5 flex justify-between text-xs font-bold">
            <span>Total</span>
            <span class="text-orange-600">{{ formatRupiah(order.totalAmount) }}</span>
          </div>
        </div>

        <!-- Notes -->
        <div v-if="order.notes" class="mx-4 mb-3 flex items-start gap-1.5 text-xs text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 rounded-lg px-2.5 py-1.5">
          <PhNote class="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
          <span>{{ order.notes }}</span>
        </div>

        <!-- Footer -->
        <div class="mt-auto px-4 pb-4">
          <!-- Payment -->
          <div class="flex items-center gap-1.5 mb-3">
            <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">{{ order.payment?.method }}</span>
            <span
              class="text-xs px-2 py-0.5 rounded-full font-medium"
              :class="order.payment?.status === 'PAID'
                ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300'
                : 'bg-yellow-100 dark:bg-yellow-900/40 text-yellow-700 dark:text-yellow-300'"
            >
              {{ order.payment?.status === 'PAID' ? 'Paid' : 'Unpaid' }}
            </span>
            <span class="text-xs text-gray-400 font-mono ml-auto">#{{ order.orderNumber.slice(-6) }}</span>
          </div>

          <!-- Action -->
          <div v-if="order.status !== 'DELIVERED' && order.status !== 'CANCELLED'">
            <div
              v-if="order.status === 'PENDING' && order.payment?.status !== 'PAID'"
              class="flex items-center justify-center gap-2 text-xs text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl px-3 py-2.5 font-medium"
            >
              <span class="w-2 h-2 rounded-full bg-amber-400 animate-pulse flex-shrink-0"></span>
              Awaiting payment
            </div>
            <button
              v-else-if="nextAction(order.status)"
              @click="updateStatus(order.id, nextAction(order.status).status)"
              :class="['w-full py-2.5 rounded-xl text-sm font-semibold transition-colors', nextAction(order.status).btnClass]"
              :disabled="updating === order.id"
            >
              <span v-if="updating === order.id" class="flex items-center justify-center gap-2">
                <span class="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                Updating...
              </span>
              <span v-else>{{ nextAction(order.status).label }}</span>
            </button>
          </div>
          <div v-else-if="order.status === 'DELIVERED'" class="flex items-center justify-center gap-1.5 text-xs text-green-600 dark:text-green-400 font-medium py-1">
            <PhCheckCircle class="h-4 w-4" />
            Served
          </div>
          <div v-else class="flex items-center justify-center gap-1.5 text-xs text-red-500 font-medium py-1">
            <PhXCircle class="h-4 w-4" />
            Cancelled
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Staff dashboard — the main working view for kitchen/service staff.
 *
 * Sections:
 * 1. Active Tables strip: horizontal-scrolling cards showing OPEN sessions
 *    with order count, total, and an "End Session" button.
 * 2. Status tabs: Active | Pending | Confirmed | Preparing | Ready | All
 *    Each tab shows a live count badge. "Active" combines all in-progress statuses.
 * 3. Order cards: color-coded by status with a full-width action button to
 *    advance to the next stage. PENDING orders with unpaid payment show
 *    "Awaiting payment" instead of an action button.
 *
 * Real-time: subscribes to /topic/orders via WebSocket.
 * On each update, the order list is refreshed in-place and sessions are reloaded
 * (new orders may have created a new session).
 *
 * Search: client-side filter by order number, customer name, phone, or table number.
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog } from '@/composables/useDialog'
import { staffApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { PhForkKnife, PhMagnifyingGlass, PhX, PhArrowCounterClockwise, PhTable, PhNote, PhCheckCircle, PhXCircle } from '@phosphor-icons/vue'

const auth = useAuthStore()
const { showConfirm, showAlert } = useDialog()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/staff/login')
}

const orders = ref([])
const loading = ref(true)
const updating = ref(null)       // holds the ID of the order currently being updated
const wsConnected = ref(false)
const searchQuery = ref('')
const openSessions = ref([])
const closingTable = ref(null)   // holds the table number whose session is being closed
const activeTab = ref('ACTIVE')

// ── Status tabs ────────────────────────────────────────────────────────────────

const ACTIVE_STATUSES = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY']

function countActive() {
  return orders.value.filter(o => ACTIVE_STATUSES.includes(o.status)).length
}

const statusTabs = computed(() => [
  {
    value: 'ACTIVE',
    label: 'Active',
    count: countActive(),
    activeClass: 'bg-orange-500 text-white',
    badgeClass: 'bg-orange-100 text-orange-700',
    badgeActiveClass: 'bg-white/30 text-white',
  },
  {
    value: 'PENDING',
    label: 'Pending',
    count: orders.value.filter(o => o.status === 'PENDING').length,
    activeClass: 'bg-yellow-500 text-white',
    badgeClass: 'bg-yellow-100 text-yellow-700',
    badgeActiveClass: 'bg-white/30 text-white',
  },
  {
    value: 'CONFIRMED',
    label: 'Confirmed',
    count: orders.value.filter(o => o.status === 'CONFIRMED').length,
    activeClass: 'bg-blue-500 text-white',
    badgeClass: 'bg-blue-100 text-blue-700',
    badgeActiveClass: 'bg-white/30 text-white',
  },
  {
    value: 'PREPARING',
    label: 'Preparing',
    count: orders.value.filter(o => o.status === 'PREPARING').length,
    activeClass: 'bg-purple-500 text-white',
    badgeClass: 'bg-purple-100 text-purple-700',
    badgeActiveClass: 'bg-white/30 text-white',
  },
  {
    value: 'READY',
    label: 'Ready',
    count: orders.value.filter(o => o.status === 'READY').length,
    activeClass: 'bg-green-500 text-white',
    badgeClass: 'bg-green-100 text-green-700',
    badgeActiveClass: 'bg-white/30 text-white',
  },
  {
    value: 'ALL',
    label: 'All',
    count: orders.value.length,
    activeClass: 'bg-gray-700 text-white dark:bg-gray-600',
    badgeClass: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
    badgeActiveClass: 'bg-white/20 text-white',
  },
])

/** Applies the active tab filter and search query to the full orders list. */
const displayedOrders = computed(() => {
  let result = orders.value

  if (activeTab.value === 'ACTIVE') {
    result = result.filter(o => ACTIVE_STATUSES.includes(o.status))
  } else if (activeTab.value !== 'ALL') {
    result = result.filter(o => o.status === activeTab.value)
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    result = result.filter(o =>
      o.orderNumber.toLowerCase().includes(q) ||
      (o.customerName && o.customerName.toLowerCase().includes(q)) ||
      (o.customerPhone && o.customerPhone.includes(q)) ||
      (o.tableNumber && o.tableNumber.toString().includes(q))
    )
  }

  return result
})

// ── Styling helpers ────────────────────────────────────────────────────────────

const BORDER_TOP = {
  PENDING:   'border-yellow-400',
  CONFIRMED: 'border-blue-400',
  PREPARING: 'border-purple-500',
  READY:     'border-green-500',
  DELIVERED: 'border-gray-200 dark:border-gray-700',
  CANCELLED: 'border-red-300',
}

const TABLE_BG = {
  PENDING:   'bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-300',
  CONFIRMED: 'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300',
  PREPARING: 'bg-purple-100 dark:bg-purple-900/40 text-purple-800 dark:text-purple-300',
  READY:     'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300',
  DELIVERED: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
  CANCELLED: 'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300',
}

const NEXT_ACTION = {
  PENDING:   { status: 'CONFIRMED', label: 'Confirm Order',  btnClass: 'bg-blue-500 hover:bg-blue-600 text-white' },
  CONFIRMED: { status: 'PREPARING', label: 'Start Cooking',  btnClass: 'bg-purple-500 hover:bg-purple-600 text-white' },
  PREPARING: { status: 'READY',     label: 'Mark Ready',     btnClass: 'bg-green-500 hover:bg-green-600 text-white' },
  READY:     { status: 'DELIVERED', label: 'Mark Served',    btnClass: 'bg-orange-500 hover:bg-orange-600 text-white' },
}

function borderTopClass(status) { return BORDER_TOP[status] || 'border-gray-200' }
function tableBgClass(status) { return TABLE_BG[status] || 'bg-gray-100 text-gray-600' }
function nextAction(status) { return NEXT_ACTION[status] || null }

function formatTime(dt) {
  return new Date(dt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

/** Returns a human-readable elapsed time string (e.g. "5m ago", "1h ago"). */
function timeAgo(dt) {
  const mins = Math.floor((Date.now() - new Date(dt)) / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  return `${Math.floor(mins / 60)}h ago`
}

// ── Data loading ───────────────────────────────────────────────────────────────

const { connect } = useWebSocket()

async function loadOrders() {
  loading.value = true
  try {
    const res = await staffApi.getOrders(false)
    orders.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadSessions() {
  try {
    const res = await staffApi.getOpenSessions()
    openSessions.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function endSession(tableNumber) {
  const ok = await showConfirm('The table will be freed for the next customer.', `End session for Table ${tableNumber}?`, 'danger')
  if (!ok) return
  closingTable.value = tableNumber
  try {
    await staffApi.closeSession(tableNumber)
    openSessions.value = openSessions.value.filter(s => s.tableNumber !== tableNumber)
  } catch (e) {
    showAlert(e.response?.data?.message || 'Failed to end session', 'Error')
  } finally {
    closingTable.value = null
  }
}

async function updateStatus(orderId, status) {
  updating.value = orderId
  try {
    await staffApi.updateStatus(orderId, status)
  } catch (e) {
    showAlert(e.response?.data?.message || 'Failed to update status', 'Error')
  } finally {
    updating.value = null
  }
}

onMounted(() => {
  loadOrders()
  loadSessions()

  connect((client) => {
    wsConnected.value = true
    // Subscribe to all order updates — update existing orders in-place or prepend new ones
    client.subscribe('/topic/orders', (message) => {
      const updated = JSON.parse(message.body)
      const idx = orders.value.findIndex(o => o.id === updated.id)
      if (idx !== -1) {
        orders.value[idx] = updated  // update existing order card
      } else {
        orders.value.unshift(updated)  // prepend new order
      }
      // Reload sessions in case a new order opened a new table session
      loadSessions()
    })
  })
})
</script>
