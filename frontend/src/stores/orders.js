import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Pinia store for the customer's order history.
 * Stores { orderNumber, tableNumber, paymentToken } tuples so orders are scoped
 * to the current table — a customer at table 5 only sees table 5's orders.
 *
 * paymentToken is the one-time token issued by the server on order placement.
 * It is required when calling POST /api/orders/{orderNumber}/pay so that only
 * the customer who placed the order can confirm payment.
 *
 * Persisted to localStorage so order history (and the token) survive page refreshes.
 * Orders for a table are cleared when the session ends (detected in MyOrdersView/Navbar).
 */
export const useOrdersStore = defineStore('myOrders', () => {
  // Each entry: { orderNumber, tableNumber, paymentToken }
  const orders = ref(JSON.parse(localStorage.getItem('myOrders') || '[]'))

  /**
   * Adds an order to the front of the list (newest first); skips duplicates.
   * @param paymentToken the one-time token from the placeOrder response — must be
   *                     persisted here so PaymentView can retrieve it without a
   *                     separate server round-trip (and without exposing it in URLs).
   */
  function addOrder(orderNumber, tableNumber, paymentToken = null) {
    if (!orders.value.find(o => o.orderNumber === orderNumber)) {
      orders.value.unshift({ orderNumber, tableNumber: String(tableNumber), paymentToken })
      persist()
    }
  }

  /** Removes a single order (e.g. when a session ends and its orders are cleared). */
  function removeOrder(orderNumber) {
    orders.value = orders.value.filter(o => o.orderNumber !== orderNumber)
    persist()
  }

  /** Returns all order numbers associated with a specific table. */
  function getNumbersForTable(tableNumber) {
    return orders.value
      .filter(o => o.tableNumber === String(tableNumber))
      .map(o => o.orderNumber)
  }

  /**
   * Returns the payment token for a given order, or null if not found.
   * Used by PaymentView to supply the token when calling confirmPayment.
   */
  function getTokenForOrder(orderNumber) {
    return orders.value.find(o => o.orderNumber === orderNumber)?.paymentToken ?? null
  }

  /**
   * Debounced write to localStorage.
   * Multiple rapid removes (e.g. session clear) are coalesced into one write
   * instead of one synchronous serialisation per call.
   */
  let _persistTimer = null
  function persist() {
    clearTimeout(_persistTimer)
    _persistTimer = setTimeout(() => {
      localStorage.setItem('myOrders', JSON.stringify(orders.value))
    }, 300)
  }

  return { orders, addOrder, removeOrder, getNumbersForTable, getTokenForOrder }
})
