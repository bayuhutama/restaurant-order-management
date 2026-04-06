<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-8">Checkout</h1>

    <div v-if="cart.items.length === 0" class="text-center py-20">
      <p class="text-gray-500 dark:text-gray-400 mb-4">Your cart is empty</p>
      <RouterLink to="/" class="btn-primary">Browse Menu</RouterLink>
    </div>

    <div v-else class="grid lg:grid-cols-3 gap-8">
      <!-- Form -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Contact Info -->
        <div class="card p-6">
          <h2 class="text-lg font-semibold mb-4">Contact Information <span class="text-sm font-normal text-gray-400">(optional)</span></h2>

          <div class="space-y-4">
            <div>
              <label class="label">Name</label>
              <input v-model="form.guestName" class="input" placeholder="Your name (optional)" />
            </div>
            <div>
              <label class="label">Phone</label>
              <input v-model="form.guestPhone" class="input" placeholder="Your phone number (optional)" />
            </div>
            <div>
              <label class="label">Email</label>
              <input v-model="form.guestEmail" class="input" type="email" placeholder="your@email.com (optional)" />
            </div>
          </div>
        </div>

        <!-- Table & Notes -->
        <div class="card p-6">
          <h2 class="text-lg font-semibold mb-4">Order Details</h2>
          <div class="space-y-4">
            <!-- Table number — locked if set via QR scan -->
            <div>
              <label class="label">Table Number <span class="text-red-500">*</span></label>
              <div v-if="tableStore.tableNumber" class="flex items-center gap-3 px-4 py-3 bg-green-50 dark:bg-green-900/20 border border-green-300 dark:border-green-700 rounded-lg">
                <div class="flex-1">
                  <p class="font-semibold text-green-900 dark:text-green-300">Table {{ tableStore.tableNumber }}</p>
                  <p class="text-xs text-green-600 dark:text-green-400">Set from QR code scan</p>
                </div>
                <PhCheck class="h-5 w-5 text-green-500" />
              </div>
              <div v-else class="space-y-2">
                <input
                  v-model="form.tableNumber"
                  class="input"
                  placeholder="Enter your table number"
                  :class="tableError ? 'border-red-400 focus:ring-red-400' : ''"
                />
                <p class="text-xs text-amber-600">
                  For best experience, scan the QR code at your table.
                </p>
              </div>
            </div>
            <div>
              <label class="label">Special Instructions</label>
              <textarea v-model="form.notes" class="input resize-none" rows="3" placeholder="Any allergies or special requests..."></textarea>
            </div>
          </div>
        </div>

      </div>

      <!-- Order Summary -->
      <div class="lg:col-span-1">
        <div class="card p-6 sticky top-24">
          <h2 class="text-lg font-semibold mb-4">Order Summary</h2>

          <div class="space-y-3 mb-4">
            <div v-for="item in cart.items" :key="item.id" class="flex justify-between text-sm">
              <span class="text-gray-700 dark:text-gray-300">{{ item.name }} × {{ item.quantity }}</span>
              <span class="font-medium">{{ formatRupiah(item.price * item.quantity) }}</span>
            </div>
          </div>

          <div class="border-t pt-4 mb-6">
            <div class="flex justify-between font-bold text-lg">
              <span>Total</span>
              <span class="text-orange-600">{{ formatRupiah(cart.total) }}</span>
            </div>
          </div>

          <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {{ error }}
          </div>

          <button
            @click="submitOrder"
            :disabled="submitting"
            class="btn-primary w-full py-3 text-base"
          >
            <span v-if="submitting">Placing Order...</span>
            <span v-else>Place Order</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { formatRupiah } from '@/utils/format'
import { useTableStore } from '@/stores/table'
import { useOrdersStore } from '@/stores/orders'
import { orderApi } from '@/api'
import { PhCheck } from '@phosphor-icons/vue'

const cart = useCartStore()
const tableStore = useTableStore()
const ordersStore = useOrdersStore()
const router = useRouter()

const form = ref({
  guestName: '',
  guestPhone: '',
  guestEmail: '',
  tableNumber: '',
  notes: ''
})

const submitting = ref(false)
const error = ref('')
const tableError = ref(false)

// The effective table number — from QR store if available, otherwise from manual input
const effectiveTable = computed(() =>
  tableStore.tableNumber || form.value.tableNumber.trim()
)

async function submitOrder() {
  error.value = ''
  tableError.value = false

  if (!effectiveTable.value) {
    tableError.value = true
    error.value = 'Table number is required. Please scan the QR code at your table.'
    return
  }

  const payload = {
    items: cart.items.map(i => ({ menuItemId: i.id, quantity: i.quantity, notes: '' })),
    notes: form.value.notes,
    tableNumber: effectiveTable.value,
    guestName: form.value.guestName || null,
    guestPhone: form.value.guestPhone || null,
    guestEmail: form.value.guestEmail || null,
    paymentMethod: 'CASH'
  }

  submitting.value = true
  try {
    const res = await orderApi.placeOrder(payload)
    cart.clearCart()
    // If table was entered manually (not from QR), persist it to the table store
    if (!tableStore.tableNumber) {
      tableStore.setTable(form.value.tableNumber.trim())
    }
    ordersStore.addOrder(res.data.orderNumber, effectiveTable.value)
    router.push(`/payment/${res.data.orderNumber}`)
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to place order. Please try again.'
  } finally {
    submitting.value = false
  }
}
</script>
