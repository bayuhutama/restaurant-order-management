<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <!-- Loading -->
    <div v-if="loading" class="space-y-4">
      <div class="h-8 bg-gray-200 rounded w-1/3 animate-pulse"></div>
      <div class="card p-6 animate-pulse space-y-3">
        <div class="h-4 bg-gray-200 rounded w-full"></div>
        <div class="h-4 bg-gray-200 rounded w-3/4"></div>
        <div class="h-4 bg-gray-200 rounded w-1/2"></div>
      </div>
    </div>

    <!-- No active session -->
    <div v-else-if="!session" class="text-center py-20">
      <div class="flex justify-center mb-4">
        <PhClipboardText class="h-16 w-16 text-gray-300" />
      </div>
      <h2 class="text-xl font-semibold text-gray-700 dark:text-gray-300 mb-2">No active bill for this table.</h2>
      <p class="text-gray-400 dark:text-gray-500 text-sm mb-6">Place an order first to start a table session.</p>
      <RouterLink to="/" class="btn-primary">Browse Menu</RouterLink>
    </div>

    <!-- Payment success -->
    <div v-else-if="paid" class="text-center py-20">
      <div class="flex justify-center mb-4">
        <div class="h-20 w-20 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
          <PhCheck class="h-10 w-10 text-green-500" />
        </div>
      </div>
      <h2 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">Payment complete!</h2>
      <p class="text-gray-500 dark:text-gray-400">Thank you for dining with us.</p>
      <RouterLink to="/" class="mt-6 inline-block btn-primary">Back to Menu</RouterLink>
    </div>

    <!-- Active bill -->
    <div v-else>
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">Table {{ tableNumber }} Bill</h1>
          <p class="text-sm text-gray-400 mt-0.5">{{ session.orderCount }} order{{ session.orderCount !== 1 ? 's' : '' }}</p>
        </div>
        <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-700">
          Open
        </span>
      </div>

      <!-- Orders breakdown -->
      <div class="space-y-4 mb-6">
        <div
          v-for="(order, idx) in session.orders"
          :key="order.id"
          class="card p-4"
        >
          <div class="flex items-center justify-between mb-3">
            <div>
              <span class="font-semibold text-sm text-gray-800 dark:text-gray-200">Order #{{ idx + 1 }}</span>
              <span class="ml-2 text-xs text-gray-400">{{ order.orderNumber }}</span>
            </div>
            <span class="text-xs font-medium px-2 py-0.5 rounded-full"
              :class="{
                'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300': order.status === 'PENDING',
                'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300': order.status === 'CONFIRMED' || order.status === 'PREPARING',
                'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300': order.status === 'READY',
                'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300': order.status === 'DELIVERED',
              }"
            >{{ order.status }}</span>
          </div>

          <div class="space-y-1.5">
            <div v-for="item in order.items" :key="item.id" class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">{{ item.menuItemName }} &times; {{ item.quantity }}</span>
              <span class="text-gray-800 dark:text-gray-200 font-medium">{{ formatRupiah(item.subtotal) }}</span>
            </div>
          </div>

          <div class="border-t dark:border-gray-700 mt-3 pt-3 flex justify-between text-sm font-semibold">
            <span class="text-gray-600 dark:text-gray-400">Subtotal</span>
            <span class="text-gray-900 dark:text-gray-100">{{ formatRupiah(order.totalAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- Grand total -->
      <div class="card p-4 mb-6 bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800">
        <div class="flex justify-between items-center">
          <span class="text-lg font-bold text-gray-900 dark:text-gray-100">Grand Total</span>
          <span class="text-2xl font-bold text-orange-600">{{ formatRupiah(session.totalAmount) }}</span>
        </div>
      </div>

      <!-- Payment method selection -->
      <div class="card p-6 mb-6">
        <h2 class="text-lg font-semibold mb-4">Payment Method</h2>
        <div class="grid grid-cols-2 gap-3">
          <!-- Cash -->
          <button
            @click="paymentMethod = 'CASH'"
            :class="['p-4 rounded-xl border-2 text-center transition-all', paymentMethod === 'CASH' ? 'border-orange-500 bg-orange-50 dark:bg-orange-900/20' : 'border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500']"
          >
            <div class="flex justify-center mb-1">
              <PhMoney class="h-7 w-7 text-gray-600 dark:text-gray-400" />
            </div>
            <div class="font-medium">Cash</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">Pay at counter</div>
          </button>

          <!-- Card -->
          <button
            @click="paymentMethod = 'CARD'"
            :class="['p-4 rounded-xl border-2 text-center transition-all', paymentMethod === 'CARD' ? 'border-orange-500 bg-orange-50 dark:bg-orange-900/20' : 'border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500']"
          >
            <div class="flex justify-center mb-1">
              <PhCreditCard class="h-7 w-7 text-gray-600 dark:text-gray-400" />
            </div>
            <div class="font-medium">Card</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">Pay now (simulated)</div>
          </button>
        </div>

        <!-- Card form -->
        <div v-if="paymentMethod === 'CARD'" class="mt-5 space-y-4">
          <div>
            <label class="label">Cardholder Name</label>
            <input
              v-model="card.name"
              class="input"
              placeholder="Full name on card"
              :class="cardErrors.name ? 'border-red-400 focus:ring-red-400' : ''"
            />
            <p v-if="cardErrors.name" class="text-xs text-red-500 mt-1">{{ cardErrors.name }}</p>
          </div>
          <div>
            <label class="label">Card Number</label>
            <input
              v-model="card.number"
              class="input font-mono"
              placeholder="XXXX XXXX XXXX XXXX"
              maxlength="19"
              @input="formatCardNumber"
              :class="cardErrors.number ? 'border-red-400 focus:ring-red-400' : ''"
            />
            <p v-if="cardErrors.number" class="text-xs text-red-500 mt-1">{{ cardErrors.number }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Expiry (MM/YY)</label>
              <input
                v-model="card.expiry"
                class="input"
                placeholder="MM/YY"
                maxlength="5"
                @input="formatExpiry"
                :class="cardErrors.expiry ? 'border-red-400 focus:ring-red-400' : ''"
              />
              <p v-if="cardErrors.expiry" class="text-xs text-red-500 mt-1">{{ cardErrors.expiry }}</p>
            </div>
            <div>
              <label class="label">CVV</label>
              <input
                v-model="card.cvv"
                class="input"
                placeholder="123"
                maxlength="3"
                @input="card.cvv = card.cvv.replace(/\D/g, '')"
                :class="cardErrors.cvv ? 'border-red-400 focus:ring-red-400' : ''"
              />
              <p v-if="cardErrors.cvv" class="text-xs text-red-500 mt-1">{{ cardErrors.cvv }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Error -->
      <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
        {{ error }}
      </div>

      <!-- Pay button -->
      <button
        @click="submitPayment"
        :disabled="paying"
        class="btn-primary w-full py-3 text-base"
      >
        <span v-if="paying">Processing...</span>
        <span v-else>Pay {{ formatRupiah(session.totalAmount) }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { tableSessionApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { PhClipboardText, PhCheck, PhMoney, PhCreditCard } from '@phosphor-icons/vue'

const route = useRoute()
const tableNumber = route.params.tableNumber

const loading = ref(true)
const session = ref(null)
const paid = ref(false)
const paying = ref(false)
const error = ref('')
const paymentMethod = ref('CASH')

const card = ref({
  name: '',
  number: '',
  expiry: '',
  cvv: ''
})
const cardErrors = ref({})

async function fetchSession() {
  loading.value = true
  try {
    const res = await tableSessionApi.getSession(tableNumber)
    session.value = res.data
  } catch {
    session.value = null
  } finally {
    loading.value = false
  }
}

function formatCardNumber(e) {
  let val = e.target.value.replace(/\D/g, '').slice(0, 16)
  card.value.number = val.replace(/(.{4})/g, '$1 ').trim()
}

function formatExpiry(e) {
  let val = e.target.value.replace(/\D/g, '').slice(0, 4)
  if (val.length >= 3) {
    card.value.expiry = val.slice(0, 2) + '/' + val.slice(2)
  } else {
    card.value.expiry = val
  }
}

function luhnCheck(num) {
  const digits = num.replace(/\s/g, '')
  let sum = 0
  let alt = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let n = parseInt(digits[i], 10)
    if (alt) {
      n *= 2
      if (n > 9) n -= 9
    }
    sum += n
    alt = !alt
  }
  return sum % 10 === 0
}

function validateCard() {
  const errs = {}
  const { name, number, expiry, cvv } = card.value

  if (!name || name.trim().length < 2) {
    errs.name = 'Cardholder name must be at least 2 characters.'
  }

  const rawNumber = number.replace(/\s/g, '')
  if (rawNumber.length !== 16 || !luhnCheck(rawNumber)) {
    errs.number = 'Invalid card number.'
  }

  const expiryMatch = expiry.match(/^(\d{2})\/(\d{2})$/)
  if (!expiryMatch) {
    errs.expiry = 'Enter expiry as MM/YY.'
  } else {
    const month = parseInt(expiryMatch[1], 10)
    const year = parseInt('20' + expiryMatch[2], 10)
    const now = new Date()
    const cardDate = new Date(year, month - 1, 1)
    const firstOfThisMonth = new Date(now.getFullYear(), now.getMonth(), 1)
    if (month < 1 || month > 12) {
      errs.expiry = 'Month must be 01–12.'
    } else if (cardDate < firstOfThisMonth) {
      errs.expiry = 'Card has expired.'
    }
  }

  if (!/^\d{3}$/.test(cvv)) {
    errs.cvv = 'CVV must be 3 digits.'
  }

  cardErrors.value = errs
  return Object.keys(errs).length === 0
}

async function submitPayment() {
  error.value = ''
  cardErrors.value = {}

  if (paymentMethod.value === 'CARD' && !validateCard()) {
    return
  }

  paying.value = true
  try {
    await tableSessionApi.paySession(tableNumber, { paymentMethod: paymentMethod.value })
    paid.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Payment failed. Please try again.'
  } finally {
    paying.value = false
  }
}

onMounted(fetchSession)
</script>
