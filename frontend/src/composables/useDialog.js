import { reactive } from 'vue'

// Module-level singleton state — one dialog at a time across the whole app
const state = reactive({
  show: false,
  type: 'alert',   // 'alert' | 'confirm'
  title: '',
  message: '',
  variant: 'default', // 'default' | 'danger'
  resolve: null,
})

export function useDialog() {
  function showAlert(message, title = 'Notice') {
    return new Promise(resolve => {
      Object.assign(state, { show: true, type: 'alert', title, message, variant: 'default', resolve })
    })
  }

  function showConfirm(message, title = 'Are you sure?', variant = 'default') {
    return new Promise(resolve => {
      Object.assign(state, { show: true, type: 'confirm', title, message, variant, resolve })
    })
  }

  function close(result) {
    state.resolve?.(result)
    state.show = false
  }

  return { state, showAlert, showConfirm, close }
}
