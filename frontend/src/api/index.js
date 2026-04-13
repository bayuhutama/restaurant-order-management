import axios from 'axios'

/**
 * Shared Axios instance for all API calls.
 * Base URL points to the Spring Boot backend.
 */
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' }
})

// Attach JWT token to every request if present in localStorage
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data)
}

// ── Menu (public) ─────────────────────────────────────────────────────────────
export const menuApi = {
  getCategories: () => api.get('/categories'),
  getCategoryById: (id) => api.get(`/categories/${id}`),
  /** params: { categoryId?, available? } */
  getMenuItems: (params) => api.get('/menu', { params }),
  getMenuItemById: (id) => api.get(`/menu/${id}`)
}

// ── Orders (public) ───────────────────────────────────────────────────────────
export const orderApi = {
  placeOrder: (data) => api.post('/orders', data),
  /**
   * Customer confirms payment on the payment page.
   * @param paymentToken the one-time token issued in the placeOrder response;
   *                     required by the server to prevent unauthorised confirmation.
   */
  confirmPayment: (orderNumber, paymentToken) =>
    api.post(`/orders/${orderNumber}/pay`, { paymentToken }),
  trackOrder: (orderNumber) => api.get(`/orders/track/${orderNumber}`),
  myOrders: () => api.get('/orders/my')
}

// ── Table Sessions (public) ───────────────────────────────────────────────────
export const tableSessionApi = {
  /** Returns the active session for a table; 404 means session ended. */
  getSession: (tableNumber) => api.get(`/table-sessions/${tableNumber}`),
  /** Settles the whole table bill with the chosen payment method. */
  paySession: (tableNumber, data) => api.post(`/table-sessions/${tableNumber}/pay`, data)
}

// ── Staff (STAFF or ADMIN role required) ─────────────────────────────────────
export const staffApi = {
  /** @param activeOnly - when true, returns only in-progress orders */
  getOrders: (activeOnly = false) => api.get('/staff/orders', { params: { activeOnly } }),
  getOrder: (id) => api.get(`/staff/orders/${id}`),
  updateStatus: (id, status) => api.patch(`/staff/orders/${id}/status`, { status }),
  getOpenSessions: () => api.get('/staff/tables'),
  closeSession: (tableNumber) => api.post(`/staff/tables/${tableNumber}/close`)
}

// ── Admin - File Upload (ADMIN role required) ─────────────────────────────────
export const uploadApi = {
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ── Admin - Categories (ADMIN role required) ──────────────────────────────────
export const adminCategoryApi = {
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id) => api.delete(`/admin/categories/${id}`)
}

// ── Admin - Menu Items (ADMIN role required) ──────────────────────────────────
export const adminMenuApi = {
  create: (data) => api.post('/admin/menu', data),
  update: (id, data) => api.put(`/admin/menu/${id}`, data),
  delete: (id) => api.delete(`/admin/menu/${id}`),
  toggleAvailability: (id) => api.patch(`/admin/menu/${id}/availability`)
}

// ── Admin - Orders (ADMIN role required) ──────────────────────────────────────
export const adminOrderApi = {
  /** Returns all orders including AWAITING_PAYMENT. */
  getAll: () => api.get('/admin/orders'),
  updateStatus: (id, status) => api.patch(`/admin/orders/${id}/status`, { status })
}

export default api
