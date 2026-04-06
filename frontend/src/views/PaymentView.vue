<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-8">
    <div class="w-full max-w-lg">

      <!-- Loading -->
      <div v-if="loading" class="card p-10 text-center text-gray-400">
        <div class="w-8 h-8 border-4 border-gray-200 border-t-orange-500 rounded-full animate-spin mx-auto mb-3"></div>
        Loading order...
      </div>

      <!-- Error -->
      <div v-else-if="loadError" class="card p-10 text-center">
        <PhWarningCircle class="h-12 w-12 text-red-400 mx-auto mb-3" />
        <p class="text-red-600 mb-4">{{ loadError }}</p>
        <RouterLink to="/" class="btn-secondary">Back to Menu</RouterLink>
      </div>

      <!-- Already paid -->
      <div v-else-if="order && order.status !== 'AWAITING_PAYMENT'" class="card p-10 text-center">
        <PhCheckCircle class="h-12 w-12 text-green-500 mx-auto mb-3" />
        <h2 class="text-xl font-bold mb-2">Payment already confirmed!</h2>
        <p class="text-gray-500 dark:text-gray-400 mb-6">This order has already been processed.</p>
        <RouterLink :to="`/track/${order.orderNumber}`" class="btn-primary">Track Your Order</RouterLink>
      </div>

      <!-- Payment form -->
      <div v-else-if="order" class="space-y-6">
        <div class="text-center">
          <h1 class="text-2xl font-bold">Complete Your Payment</h1>
          <p class="text-gray-500 dark:text-gray-400 text-sm mt-1">Order <span class="font-mono font-semibold">{{ order.orderNumber }}</span></p>
        </div>

        <!-- Order summary -->
        <div class="card p-5">
          <h2 class="font-semibold mb-3">Order Summary</h2>
          <div class="space-y-2 mb-4">
            <div v-for="item in order.items" :key="item.id" class="flex items-center gap-3">
              <img
                :src="item.menuItemImage || 'https://via.placeholder.com/40'"
                :alt="item.menuItemName"
                class="w-10 h-10 object-cover rounded-lg flex-shrink-0"
                @error="e => e.target.src = 'https://via.placeholder.com/40'"
              />
              <div class="flex-1">
                <p class="text-sm font-medium">{{ item.menuItemName }}</p>
                <p class="text-xs text-gray-400">{{ item.quantity }} × {{ formatRupiah(item.unitPrice) }}</p>
              </div>
              <p class="text-sm font-semibold">{{ formatRupiah(item.subtotal) }}</p>
            </div>
          </div>
          <div class="border-t dark:border-gray-700 pt-3 flex justify-between font-bold text-lg">
            <span>Total</span>
            <span class="text-orange-600">{{ formatRupiah(order.totalAmount) }}</span>
          </div>
        </div>

        <!-- Payment method display -->
        <div class="card p-5">
          <h2 class="font-semibold mb-4">Payment Method</h2>

          <!-- CARD -->
          <div v-if="order.payment?.method === 'CARD'" class="space-y-4">
            <div class="flex items-center gap-3 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-700">
              <PhCreditCard class="h-7 w-7 text-blue-500 flex-shrink-0" />
              <div>
                <p class="font-medium text-blue-900 dark:text-blue-300">Card Payment</p>
                <p class="text-sm text-blue-600 dark:text-blue-400">Your card will be charged {{ formatRupiah(order.totalAmount) }}</p>
              </div>
            </div>

            <!-- Simulated card fields -->
            <div class="space-y-3">
              <div>
                <label class="label">Cardholder Name</label>
                <input
                  v-model="cardName"
                  class="input"
                  :class="{ 'border-red-400 focus:ring-red-400': errors.cardName }"
                  placeholder="JOHN DOE"
                  @blur="validateCardName"
                />
                <p v-if="errors.cardName" class="text-red-500 text-xs mt-1">{{ errors.cardName }}</p>
              </div>
              <div>
                <label class="label">Card Number</label>
                <input
                  v-model="cardNumber"
                  class="input font-mono"
                  :class="{ 'border-red-400 focus:ring-red-400': errors.cardNumber }"
                  placeholder="4111 1111 1111 1111"
                  maxlength="19"
                  @input="formatCardNumber"
                  @blur="validateCardNumber"
                />
                <p v-if="errors.cardNumber" class="text-red-500 text-xs mt-1">{{ errors.cardNumber }}</p>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="label">Expiry</label>
                  <input
                    v-model="expiry"
                    class="input"
                    :class="{ 'border-red-400 focus:ring-red-400': errors.expiry }"
                    placeholder="MM/YY"
                    maxlength="5"
                    @input="formatExpiry"
                    @blur="validateExpiry"
                  />
                  <p v-if="errors.expiry" class="text-red-500 text-xs mt-1">{{ errors.expiry }}</p>
                </div>
                <div>
                  <label class="label">CVV</label>
                  <input
                    v-model="cvv"
                    class="input"
                    :class="{ 'border-red-400 focus:ring-red-400': errors.cvv }"
                    placeholder="123"
                    maxlength="3"
                    type="password"
                    @input="cvv = cvv.replace(/\D/g, '')"
                    @blur="validateCvv"
                  />
                  <p v-if="errors.cvv" class="text-red-500 text-xs mt-1">{{ errors.cvv }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- CASH -->
          <div v-else class="flex items-center gap-3 p-4 bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-700">
            <PhMoney class="h-7 w-7 text-green-500 flex-shrink-0" />
            <div>
              <p class="font-medium text-green-900 dark:text-green-300">Cash Payment</p>
              <p class="text-sm text-green-600 dark:text-green-400">Our staff will collect {{ formatRupiah(order.totalAmount) }} at your table when the order is served.</p>
            </div>
          </div>
        </div>

        <!-- Error -->
        <div v-if="payError" class="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
          {{ payError }}
        </div>

        <!-- Confirm button -->
        <button
          @click="confirmPayment"
          :disabled="paying || !canPay"
          class="btn-primary w-full py-4 text-base font-semibold"
        >
          <span v-if="paying">Processing...</span>
          <span v-else-if="order.payment?.method === 'CARD'">
            Pay {{ formatRupiah(order.totalAmount) }}
          </span>
          <span v-else>
            Confirm Order — Pay at Table
          </span>
        </button>

        <p class="text-center text-xs text-gray-400">
          By confirming, your order will be sent to the kitchen and <strong>cannot be cancelled</strong>.
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import { PhWarningCircle, PhCheckCircle, PhCreditCard, PhMoney } from '@phosphor-icons/vue'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)
const loadError = ref('')
const paying = ref(false)
const payError = ref('')

// Card fields (cosmetic — no real processing)
const cardName = ref('')
const cardNumber = ref('')
const expiry = ref('')
const cvv = ref('')
const errors = ref({ cardName: '', cardNumber: '', expiry: '', cvv: '' })

function luhn(number) {
  const digits = number.replace(/\D/g, '')
  let sum = 0
  let shouldDouble = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = parseInt(digits[i])
    if (shouldDouble) { d *= 2; if (d > 9) d -= 9 }
    sum += d
    shouldDouble = !shouldDouble
  }
  return sum % 10 === 0
}

function validateCardName() {
  errors.value.cardName = cardName.value.trim().length < 2 ? 'Enter the cardholder name.' : ''
}

function validateCardNumber() {
  const digits = cardNumber.value.replace(/\s/g, '')
  if (digits.length !== 16) {
    errors.value.cardNumber = 'Card number must be 16 digits.'
  } else if (!luhn(digits)) {
    errors.value.cardNumber = 'Invalid card number.'
  } else {
    errors.value.cardNumber = ''
  }
}

function validateExpiry() {
  if (expiry.value.length !== 5) { errors.value.expiry = 'Enter expiry as MM/YY.'; return }
  const [mm, yy] = expiry.value.split('/')
  const month = parseInt(mm)
  const year = 2000 + parseInt(yy)
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth() + 1
  if (month < 1 || month > 12) {
    errors.value.expiry = 'Invalid month.'
  } else if (year < currentYear || (year === currentYear && month < currentMonth)) {
    errors.value.expiry = 'Card has expired.'
  } else {
    errors.value.expiry = ''
  }
}

function validateCvv() {
  errors.value.cvv = cvv.value.length !== 3 ? 'CVV must be 3 digits.' : ''
}

function validateAll() {
  validateCardName()
  validateCardNumber()
  validateExpiry()
  validateCvv()
  return !Object.values(errors.value).some(e => e)
}

const canPay = computed(() => {
  if (!order.value) return false
  if (order.value.payment?.method === 'CARD') {
    return cardName.value.trim().length >= 2
        && cardNumber.value.replace(/\s/g, '').length === 16
        && expiry.value.length === 5
        && cvv.value.length === 3
  }
  return true
})

function formatCardNumber(e) {
  let v = e.target.value.replace(/\D/g, '').substring(0, 16)
  cardNumber.value = v.replace(/(.{4})/g, '$1 ').trim()
  if (errors.value.cardNumber) validateCardNumber()
}

function formatExpiry(e) {
  let v = e.target.value.replace(/\D/g, '').substring(0, 4)
  if (v.length >= 2) v = v.slice(0, 2) + '/' + v.slice(2)
  expiry.value = v
  if (errors.value.expiry) validateExpiry()
}

async function loadOrder() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await orderApi.trackOrder(route.params.orderNumber)
    order.value = res.data
  } catch {
    loadError.value = 'Order not found.'
  } finally {
    loading.value = false
  }
}

async function confirmPayment() {
  if (order.value.payment?.method === 'CARD' && !validateAll()) return
  paying.value = true
  payError.value = ''
  try {
    await orderApi.confirmPayment(route.params.orderNumber)
    router.push(`/track/${route.params.orderNumber}`)
  } catch (e) {
    payError.value = e.response?.data?.message || 'Payment failed. Please try again.'
  } finally {
    paying.value = false
  }
}

onMounted(loadOrder)
</script>
