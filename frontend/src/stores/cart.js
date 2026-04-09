import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Pinia store for the shopping cart.
 * Cart contents are persisted to localStorage so they survive page refreshes.
 * Items are keyed by menu item ID; adding the same item increments quantity.
 */
export const useCartStore = defineStore('cart', () => {
  // Restore cart from localStorage on store initialisation
  const items = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

  /** Sum of (price × quantity) across all items. */
  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  /** Total number of individual units in the cart (not distinct items). */
  const itemCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  /** Adds a menu item to the cart, or increments its quantity if already present. */
  function addItem(menuItem) {
    const existing = items.value.find(i => i.id === menuItem.id)
    if (existing) {
      existing.quantity += 1
    } else {
      items.value.push({ ...menuItem, quantity: 1 })
    }
    persist()
  }

  /** Removes an item from the cart entirely. */
  function removeItem(menuItemId) {
    items.value = items.value.filter(i => i.id !== menuItemId)
    persist()
  }

  /** Sets the quantity for a specific item; removes it if qty drops to 0 or below. */
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

  /** Empties the cart — called after a successful order is placed. */
  function clearCart() {
    items.value = []
    persist()
  }

  /** Writes the current cart state to localStorage. */
  function persist() {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }

  return { items, total, itemCount, addItem, removeItem, updateQuantity, clearCart }
})
