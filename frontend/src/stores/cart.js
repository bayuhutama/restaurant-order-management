import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const items = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  const itemCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  function addItem(menuItem) {
    const existing = items.value.find(i => i.id === menuItem.id)
    if (existing) {
      existing.quantity += 1
    } else {
      items.value.push({ ...menuItem, quantity: 1 })
    }
    persist()
  }

  function removeItem(menuItemId) {
    items.value = items.value.filter(i => i.id !== menuItemId)
    persist()
  }

  function updateQuantity(menuItemId, qty) {
    const item = items.value.find(i => i.id === menuItemId)
    if (item) {
      if (qty <= 0) {
        removeItem(menuItemId)
      } else {
        item.quantity = qty
        persist()
      }
    }
  }

  function clearCart() {
    items.value = []
    persist()
  }

  function persist() {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }

  return { items, total, itemCount, addItem, removeItem, updateQuantity, clearCart }
})
