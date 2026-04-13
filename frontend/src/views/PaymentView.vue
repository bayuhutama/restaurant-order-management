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
      <div v-else-if="order && order.payment?.status === 'PAID'" class="card p-10 text-center">
        <PhCheckCircle class="h-12 w-12 text-green-500 mx-auto mb-3" />
        <h2 class="text-xl font-bold mb-2">Payment already confirmed!</h2>
        <p class="text-gray-500 dark:text-gray-400 mb-6">This order has already been processed.</p>
        <RouterLink to="/my-orders" class="btn-primary">My Orders</RouterLink>
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

        <!-- Payment method tabs -->
        <div class="card p-5">
          <h2 class="font-semibold mb-4">Payment Method</h2>

          <!-- Tab selector -->
          <div class="grid grid-cols-3 gap-2 mb-5">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              @click="paymentMethod = tab.value"
              :class="[
                'flex flex-col items-center gap-1.5 py-3 px-2 rounded-xl border-2 text-sm font-medium transition-colors',
                paymentMethod === tab.value
                  ? 'border-orange-500 bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-400'
                  : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:border-gray-300 dark:hover:border-gray-500'
              ]"
            >
              <component :is="tab.icon" class="h-6 w-6" />
              {{ tab.label }}
            </button>
          </div>

          <!-- QR Code tab -->
          <div v-if="paymentMethod === 'QR'" class="text-center space-y-4">
            <div class="bg-gray-50 dark:bg-gray-700 rounded-xl p-5 inline-block">
              <img v-if="qrDataUrl" :src="qrDataUrl" alt="Payment QR Code" class="w-48 h-48 mx-auto" />
              <div v-else class="w-48 h-48 mx-auto flex items-center justify-center">
                <div class="w-6 h-6 border-4 border-gray-200 border-t-orange-500 rounded-full animate-spin"></div>
              </div>
            </div>
            <p class="text-sm text-gray-600 dark:text-gray-400">Scan this QR code with your banking app or e-wallet to pay <strong class="text-orange-600">{{ formatRupiah(order.totalAmount) }}</strong>.</p>
            <p class="text-xs text-gray-400 font-mono">Ref: {{ order.orderNumber }}</p>
          </div>

          <!-- Card tab -->
          <div v-else-if="paymentMethod === 'CARD'" class="space-y-3">
            <div class="flex items-center gap-3 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-700 mb-4">
              <PhCreditCard class="h-6 w-6 text-blue-500 flex-shrink-0" />
              <p class="text-sm text-blue-700 dark:text-blue-300">Your card will be charged <strong>{{ formatRupiah(order.totalAmount) }}</strong></p>
            </div>
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

          <!-- Cash at Cashier tab -->
          <div v-else class="space-y-4">
            <div class="flex items-start gap-3 p-4 bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-700">
              <PhMoney class="h-6 w-6 text-green-500 flex-shrink-0 mt-0.5" />
              <div>
                <p class="font-medium text-green-900 dark:text-green-300 mb-1">Pay at the Cashier</p>
                <p class="text-sm text-green-700 dark:text-green-400">Please go to the cashier and quote your order number:</p>
              </div>
            </div>
            <div class="text-center py-4 bg-gray-50 dark:bg-gray-700 rounded-xl">
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Your order number</p>
              <p class="text-2xl font-mono font-bold tracking-wider text-gray-900 dark:text-gray-100">{{ order.orderNumber }}</p>
              <p class="text-lg font-semibold text-orange-600 mt-2">{{ formatRupiah(order.totalAmount) }}</p>
            </div>
            <p class="text-xs text-center text-gray-400">The cashier will process your payment and confirm the order.</p>
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
          <span v-else-if="paymentMethod === 'QR'">I've Paid via QR</span>
          <span v-else-if="paymentMethod === 'CARD'">Pay {{ formatRupiah(order.totalAmount) }}</span>
          <span v-else>Confirm — Pay at Cashier</span>
        </button>

        <p class="text-center text-xs text-gray-400">
          By confirming, your order will be sent to the kitchen and <strong>cannot be cancelled</strong>.
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
/**
 * Payment page for a single order.
 * Three payment method tabs:
 * - QR Code: generates a QR code from the order reference using the 'qrcode' library
 * - Card:    collects card details with Luhn validation (cosmetic — not connected to a payment gateway)
 * - Cash:    shows the order number for the customer to quote at the cashier
 *
 * Confirming any method calls POST /api/orders/{orderNumber}/pay, which marks
 * the payment as PAID and allows staff to advance the order to CONFIRMED.
 * After confirmation, redirects to /my-orders.
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { orderApi } from '@/api'
import { useOrdersStore } from '@/stores/orders'
import { formatRupiah } from '@/utils/format'
import { PhWarningCircle, PhCheckCircle, PhCreditCard, PhMoney, PhQrCode } from '@phosphor-icons/vue'

const route = useRoute()
const router = useRouter()
const ordersStore = useOrdersStore()

const order = ref(null)
const loading = ref(true)
const loadError = ref('')
const paying = ref(false)
const payError = ref('')
const paymentMethod = ref('QR')
const qrDataUrl = ref('')

const tabs = [
  { value: 'QR',   label: 'QR Code',   icon: PhQrCode },
  { value: 'CARD', label: 'Card',       icon: PhCreditCard },
  { value: 'CASH', label: 'Cash',       icon: PhMoney },
]

// Card form fields — validation is UI-only (Luhn check + expiry check), no real gateway
const cardName = ref('')
const cardNumber = ref('')
const expiry = ref('')
const cvv = ref('')
const errors = ref({ cardName: '', cardNumber: '', expiry: '', cvv: '' })

/** Validates a card number using the Luhn algorithm. */
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
  if (paymentMethod.value === 'CARD') {
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

async function generateQr() {
  if (!order.value) return
  const text = `SAVORIA:${order.value.orderNumber}:${order.value.totalAmount}`
  qrDataUrl.value = await QRCode.toDataURL(text, { width: 256, margin: 2 })
}

async function loadOrder() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await orderApi.trackOrder(route.params.orderNumber)
    order.value = res.data
    await generateQr()
  } catch {
    loadError.value = 'Order not found.'
  } finally {
    loading.value = false
  }
}

async function confirmPayment() {
  if (paymentMethod.value === 'CARD' && !validateAll()) return
  paying.value = true
  payError.value = ''
  try {
    // Retrieve the one-time payment token stored by CheckoutView when the order was placed.
    // The token is required by the server to prove this caller is the same client that
    // placed the order — preventing token harvest via the public WebSocket.
    const paymentToken = ordersStore.getTokenForOrder(route.params.orderNumber)
    await orderApi.confirmPayment(route.params.orderNumber, paymentToken)
    router.push('/my-orders')
  } catch (e) {
    payError.value = e.response?.data?.message || 'Payment failed. Please try again.'
  } finally {
    paying.value = false
  }
}

onMounted(loadOrder)
</script>
