import axios from 'axios'
import { useAuthStore } from './store'

const API_URL = import.meta.env.VITE_API_URL || '/api/v1'

export const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Handle 401 — attempt refresh
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      const refreshToken = useAuthStore.getState().refreshToken
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            `${API_URL}/auth/refresh?refreshToken=${refreshToken}`
          )
          useAuthStore.getState().setAuth(data.data)
          original.headers.Authorization = `Bearer ${data.data.accessToken}`
          return api(original)
        } catch {
          useAuthStore.getState().logout()
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

// ── API Methods ───────────────────────────────────────────────

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
}

export const groupAPI = {
  create: (data) => api.post('/groups', data),
  get: (id) => api.get(`/groups/${id}`),
  myGroups: () => api.get('/groups/my'),
  addMember: (id, userId) => api.post(`/groups/${id}/members?userId=${userId}`),
  removeMember: (id, memberId) => api.delete(`/groups/${id}/members/${memberId}`),
  join: (code) => api.post(`/groups/join?inviteCode=${code}`),
  invite: (id, email) => api.post(`/groups/${id}/invite?email=${email}`),
}

export const expenseAPI = {
  create: (data) => api.post('/expenses', data),
  getForGroup: (groupId, params) => api.get(`/expenses/group/${groupId}`, { params }),
  getById: (id) => api.get(`/expenses/${id}`),
  update: (id, data) => api.put(`/expenses/${id}`, data),
  delete: (id) => api.delete(`/expenses/${id}`),
  scanReceipt: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/expenses/scan-receipt', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getSplitSuggestions: (groupId, amount) =>
    api.get(`/expenses/group/${groupId}/suggestions?amount=${amount}`),
}

export const settlementAPI = {
  settle: (data) => api.post('/settlements', data),
  getBalances: (groupId) => api.get(`/settlements/group/${groupId}/balances`),
  getHistory: (groupId) => api.get(`/settlements/group/${groupId}`),
}

export const notificationAPI = {
  getAll: (page = 0, size = 20) => api.get(`/notifications?page=${page}&size=${size}`),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put('/notifications/read-all'),
}

export const analyticsAPI = {
  getGroupAnalytics: (groupId, params) => api.get(`/analytics/group/${groupId}`, { params }),
}
