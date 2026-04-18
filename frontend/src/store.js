import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,

      setAuth: (data) => set({
        user: data.user,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      }),

      logout: () => set({ user: null, accessToken: null, refreshToken: null }),

      isAuthenticated: () => !!get().accessToken,
    }),
    { name: 'auth-storage' }
  )
)

export const useNotificationStore = create((set) => ({
  notifications: [],
  unreadCount: 0,
  addNotification: (n) => set((s) => ({
    notifications: [n, ...s.notifications].slice(0, 50),
    unreadCount: s.unreadCount + 1,
  })),
  setUnreadCount: (count) => set({ unreadCount: count }),
  markAllRead: () => set((s) => ({
    notifications: s.notifications.map(n => ({ ...n, read: true })),
    unreadCount: 0,
  })),
}))
