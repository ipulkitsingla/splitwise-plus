import { useState, useEffect } from 'react'
import { notificationAPI } from '../api'
import { useNotificationStore } from '../store'
import toast from 'react-hot-toast'

const TYPE_ICON = {
  EXPENSE_ADDED: '💸', PAYMENT_MADE: '✅', PAYMENT_REMINDER: '⏰',
  GROUP_INVITE: '👥', GROUP_UPDATE: '📝', MONTHLY_REPORT: '📊', INFO: 'ℹ️'
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const { setUnreadCount, markAllRead: markAllReadStore } = useNotificationStore()

  useEffect(() => {
    notificationAPI.getAll().then(r => {
      setNotifications(r.data.data?.content || [])
    }).finally(() => setLoading(false))
  }, [])

  const handleMarkAllRead = async () => {
    await notificationAPI.markAllRead()
    setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    markAllReadStore()
    setUnreadCount(0)
    toast.success('All marked as read')
  }

  const handleMarkRead = async (id) => {
    await notificationAPI.markRead(id)
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n))
    setUnreadCount(c => Math.max(0, c - 1))
  }

  const unread = notifications.filter(n => !n.read).length

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-6 animate-fade-up">
        <div>
          <h1 className="text-2xl font-bold text-ink-100">Notifications</h1>
          <p className="text-ink-400 text-sm mt-1">
            {unread > 0 ? `${unread} unread` : 'All caught up'}
          </p>
        </div>
        {unread > 0 && (
          <button onClick={handleMarkAllRead} className="btn-ghost text-sm py-2">
            Mark all read
          </button>
        )}
      </div>

      {loading ? (
        <div className="space-y-3">
          {[1,2,3,4].map(i => <div key={i} className="glass rounded-2xl h-16 animate-pulse" />)}
        </div>
      ) : notifications.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <div className="text-4xl mb-4">🔔</div>
          <h3 className="text-ink-200 font-semibold">No notifications</h3>
          <p className="text-ink-400 text-sm mt-2">You're all caught up!</p>
        </div>
      ) : (
        <div className="space-y-2 stagger">
          {notifications.map(n => (
            <div
              key={n.id}
              className={`glass rounded-2xl p-4 flex items-start gap-4 cursor-pointer hover:border-ink-500/40 transition-all ${
                !n.read ? 'border-lime-400/15 bg-lime-400/3' : ''
              }`}
              onClick={() => !n.read && handleMarkRead(n.id)}
            >
              <div className="w-9 h-9 rounded-xl bg-ink-700/60 flex items-center justify-center text-lg flex-shrink-0">
                {TYPE_ICON[n.type] || 'ℹ️'}
              </div>
              <div className="flex-1 min-w-0">
                <div className={`text-sm font-medium ${!n.read ? 'text-ink-100' : 'text-ink-300'}`}>
                  {n.title}
                </div>
                <div className="text-ink-400 text-xs mt-0.5">{n.message}</div>
                <div className="text-ink-600 text-xs mt-1">
                  {new Date(n.createdAt).toLocaleString()}
                </div>
              </div>
              {!n.read && (
                <div className="w-2 h-2 rounded-full bg-lime-400 mt-1.5 flex-shrink-0" />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
