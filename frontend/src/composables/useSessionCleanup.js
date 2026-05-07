import { useTableStore } from '@/stores/table'
import { useOrdersStore } from '@/stores/orders'
import { tableSessionApi } from '@/api'

/**
 * Checks if the current table session is still active.
 * If it has been closed or expired (returns 404), clears local storage
 * to prevent the user from seeing stale orders or an outdated table number.
 */
export function useSessionCleanup() {
  const tableStore = useTableStore()
  const ordersStore = useOrdersStore()

  function clearSession(tableNumber) {
    if (tableNumber) {
      ordersStore.getNumbersForTable(tableNumber).forEach(n => ordersStore.removeOrder(n))
    }
    tableStore.clearTable()
  }

  async function checkAndClean(tableNumber) {
    if (!tableNumber) return

    try {
      await tableSessionApi.getSession(tableNumber)
    } catch (e) {
      // 404 means the session was paid or expired
      if (e.response?.status === 404) {
        clearSession(tableNumber)
      }
    }
  }

  return { clearSession, checkAndClean }
}
