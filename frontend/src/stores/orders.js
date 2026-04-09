import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Pinia store for the customer's order history.
 * Stores { orderNumber, tableNumber } pairs so orders are scoped to the
 * current table — a customer at table 5 only sees table 5's orders.
 *
 * Persisted to localStorage so order history survives page refreshes.
 * Orders for a table are cleared when the session ends (detected in MyOrdersView/Navbar).
 */
export const useOrdersStore = defineStore('myOrders', () => {
  // Each entry: { orderNumber, tableNumber }
  const orders = ref(JSON.parse(localStorage.getItem('myOrders') || '[]'))

  /** Adds an order to the front of the list (newest first); skips duplicates. */
  function addOrder(orderNumber, tableNumber) {
    if (!orders.value.find(o => o.orderNumber === orderNumber)) {
      orders.value.unshift({ orderNumber, tableNumber: String(tableNumber) })
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

  /** Writes the current orders list to localStorage. */
  function persist() {
    localStorage.setItem('myOrders', JSON.stringify(orders.value))
  }

  return { orders, addOrder, removeOrder, getNumbersForTable }
})
