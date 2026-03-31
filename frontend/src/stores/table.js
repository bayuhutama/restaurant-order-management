import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTableStore = defineStore('table', () => {
  // Use sessionStorage so it clears when the browser tab closes
  const tableNumber = ref(sessionStorage.getItem('tableNumber') || '')

  function setTable(number) {
    tableNumber.value = String(number).trim()
    sessionStorage.setItem('tableNumber', tableNumber.value)
  }

  function clearTable() {
    tableNumber.value = ''
    sessionStorage.removeItem('tableNumber')
  }

  return { tableNumber, setTable, clearTable }
})
