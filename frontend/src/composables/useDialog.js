import { reactive } from 'vue'

/**
 * Module-level singleton state — one dialog at a time across the whole app.
 * The AppDialog component (mounted in App.vue) reads from this state to render the dialog.
 *
 * Using a module-level singleton (not per-component state) means any component
 * can call showAlert/showConfirm without prop-drilling or an event bus.
 */
const state = reactive({
  show: false,
  type: 'alert',       // 'alert' | 'confirm'
  title: '',
  message: '',
  variant: 'default',  // 'default' | 'danger'
  resolve: null,       // Promise resolver — called with true/false when dialog closes
})

/**
 * Composable that provides custom dialog methods to replace native alert/confirm.
 * Returns Promises so callers can await the user's response with async/await.
 *
 * @example
 *   const { showAlert, showConfirm } = useDialog()
 *   await showAlert('Item deleted')
 *   const confirmed = await showConfirm('Delete this item?', 'Confirm', 'danger')
 */
export function useDialog() {
  /** Shows an informational alert with an OK button. Resolves when dismissed. */
  function showAlert(message, title = 'Notice') {
    return new Promise(resolve => {
      Object.assign(state, { show: true, type: 'alert', title, message, variant: 'default', resolve })
    })
  }

  /**
   * Shows a confirmation dialog with Cancel and Confirm buttons.
   * Resolves with true if confirmed, false if cancelled.
   * @param variant - 'danger' uses red styling for destructive actions
   */
  function showConfirm(message, title = 'Are you sure?', variant = 'default') {
    return new Promise(resolve => {
      Object.assign(state, { show: true, type: 'confirm', title, message, variant, resolve })
    })
  }

  /** Closes the dialog and resolves its Promise with the given result. */
  function close(result) {
    state.resolve?.(result)
    state.show = false
  }

  return { state, showAlert, showConfirm, close }
}
