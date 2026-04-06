import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' }
})

// Attach JWT token to every request if present
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Auth
export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data)
}

// Menu (public)
export const menuApi = {
  getCategories: () => api.get('/categories'),
  getCategoryById: (id) => api.get(`/categories/${id}`),
  getMenuItems: (params) => api.get('/menu', { params }),
  getMenuItemById: (id) => api.get(`/menu/${id}`)
}

// Orders
export const orderApi = {
  placeOrder: (data) => api.post('/orders', data),
  confirmPayment: (orderNumber) => api.post(`/orders/${orderNumber}/pay`),
  trackOrder: (orderNumber) => api.get(`/orders/track/${orderNumber}`),
  myOrders: () => api.get('/orders/my')
}

// Table Sessions
export const tableSessionApi = {
  getSession: (tableNumber) => api.get(`/table-sessions/${tableNumber}`),
  paySession: (tableNumber, data) => api.post(`/table-sessions/${tableNumber}/pay`, data)
}

// Staff
export const staffApi = {
  getOrders: (activeOnly = false) => api.get('/staff/orders', { params: { activeOnly } }),
  getOrder: (id) => api.get(`/staff/orders/${id}`),
  updateStatus: (id, status) => api.patch(`/staff/orders/${id}/status`, { status }),
  getOpenSessions: () => api.get('/staff/tables'),
  closeSession: (tableNumber) => api.post(`/staff/tables/${tableNumber}/close`)
}

// Admin - File Upload
export const uploadApi = {
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// Admin - Categories
export const adminCategoryApi = {
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id) => api.delete(`/admin/categories/${id}`)
}

// Admin - Menu Items
export const adminMenuApi = {
  create: (data) => api.post('/admin/menu', data),
  update: (id, data) => api.put(`/admin/menu/${id}`, data),
  delete: (id) => api.delete(`/admin/menu/${id}`),
  toggleAvailability: (id) => api.patch(`/admin/menu/${id}/availability`)
}

// Admin - Orders
export const adminOrderApi = {
  getAll: () => api.get('/admin/orders'),
  updateStatus: (id, status) => api.patch(`/admin/orders/${id}/status`, { status })
}

export default api
