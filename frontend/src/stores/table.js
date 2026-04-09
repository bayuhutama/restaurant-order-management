import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Pinia store for the current table number.
 *
 * Uses sessionStorage (not localStorage) so the table association is
 * automatically cleared when the browser tab is closed — preventing a
 * future customer at the same device from inheriting the previous session.
 *
 * The table number is set either by scanning a QR code (/?table=N) or
 * by the customer entering it manually at checkout.
 */
export const useTableStore = defineStore('table', () => {
  // Restore from sessionStorage so the table persists across page navigations
  // within the same tab, but clears when the tab is closed.
  const tableNumber = ref(sessionStorage.getItem('tableNumber') || '')

  /** Sets and persists the table number. */
  function setTable(number) {
    tableNumber.value = String(number).trim()
    sessionStorage.setItem('tableNumber', tableNumber.value)
  }

  /** Clears the table association (e.g. when a session ends or the customer leaves). */
  function clearTable() {
    tableNumber.value = ''
    sessionStorage.removeItem('tableNumber')
  }

  return { tableNumber, setTable, clearTable }
})
