import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useOrdersStore = defineStore('myOrders', () => {
  // Each entry: { orderNumber, tableNumber }
  const orders = ref(JSON.parse(localStorage.getItem('myOrders') || '[]'))

  function addOrder(orderNumber, tableNumber) {
    if (!orders.value.find(o => o.orderNumber === orderNumber)) {
      orders.value.unshift({ orderNumber, tableNumber: String(tableNumber) })
      persist()
    }
  }

  function removeOrder(orderNumber) {
    orders.value = orders.value.filter(o => o.orderNumber !== orderNumber)
    persist()
  }

  function getNumbersForTable(tableNumber) {
    return orders.value
      .filter(o => o.tableNumber === String(tableNumber))
      .map(o => o.orderNumber)
  }

  function persist() {
    localStorage.setItem('myOrders', JSON.stringify(orders.value))
  }

  return { orders, addOrder, removeOrder, getNumbersForTable }
})
