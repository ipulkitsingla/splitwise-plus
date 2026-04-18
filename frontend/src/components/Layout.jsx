import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAuthStore, useNotificationStore } from '../store'
import { groupAPI, notificationAPI } from '../api'
import toast from 'react-hot-toast'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = import.meta.env.VITE_WS_URL || '/api/v1/ws'

export default function Layout() {
  const { user, logout } = useAuthStore()
  const { unreadCount, setUnreadCount, addNotification } = useNotificationStore()
  const [groups, setGroups] = useState([])
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    groupAPI.myGroups().then(r => setGroups(r.data.data || []))
    notificationAPI.getUnreadCount().then(r => setUnreadCount(r.data.data || 0))
  }, [])

  // WebSocket for real-time notifications
  useEffect(() => {
    if (!user) return
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      onConnect: () => {
        client.subscribe(`/user/${user.id}/queue/notifications`, (msg) => {
          const notification = JSON.parse(msg.body)
          addNotification(notification)
          toast(notification.title, { icon: '🔔' })
        })
      },
      reconnectDelay: 5000,
    })
    client.activate()
    return () => client.deactivate()
  }, [user])

  const handleLogout = () => {
    logout()
    navigate('/login')
    toast.success('Logged out')
  }

  const navItem = 'flex items-center gap-3 px-3 py-2.5 rounded-xl text-ink-300 hover:text-ink-100 hover:bg-ink-700/50 transition-all duration-150 text-sm font-medium'
  const activeNav = 'bg-lime-400/10 text-lime-400 border border-lime-400/20'

  return (
    <div className="flex h-screen bg-ink-950 overflow-hidden">
      {/* Sidebar */}
      <aside
        className={`${sidebarOpen ? 'w-64' : 'w-16'} flex-shrink-0 flex flex-col glass border-r border-ink-700/40 transition-all duration-300 z-20`}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-4 py-5 border-b border-ink-700/40">
          <div className="w-8 h-8 bg-lime-400 rounded-lg flex items-center justify-center flex-shrink-0">
            <span className="text-ink-950 font-bold text-sm">S+</span>
          </div>
          {sidebarOpen && (
            <div className="animate-fade-in">
              <div className="font-bold text-ink-100 text-sm leading-tight">Splitwise++</div>
              <div className="text-ink-400 text-xs">Smart Expense Splitter</div>
            </div>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="ml-auto text-ink-400 hover:text-ink-100 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {sidebarOpen
                ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
                : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 5l7 7-7 7M5 5l7 7-7 7" />
              }
            </svg>
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto py-4 px-2 space-y-1">
          {/* Main nav */}
          <NavLink to="/" end className={({ isActive }) => `${navItem} ${isActive ? activeNav : ''}`}>
            <svg className="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            {sidebarOpen && <span>Dashboard</span>}
          </NavLink>

          <NavLink to="/notifications" className={({ isActive }) => `${navItem} ${isActive ? activeNav : ''}`}>
            <div className="relative flex-shrink-0">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 bg-coral-400 text-ink-950 text-xs rounded-full flex items-center justify-center font-bold">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </div>
            {sidebarOpen && <span>Notifications</span>}
          </NavLink>

          {/* Groups section */}
          {sidebarOpen && groups.length > 0 && (
            <div className="pt-4 pb-1 px-3">
              <span className="text-xs text-ink-500 font-semibold uppercase tracking-widest">Groups</span>
            </div>
          )}

          {groups.map(g => (
            <NavLink
              key={g.id}
              to={`/groups/${g.id}`}
              className={({ isActive }) => `${navItem} ${isActive ? activeNav : ''}`}
            >
              <div className="w-5 h-5 rounded-md bg-ink-600 flex items-center justify-center flex-shrink-0 text-xs font-bold text-ink-200">
                {g.name.charAt(0).toUpperCase()}
              </div>
              {sidebarOpen && (
                <span className="truncate">{g.name}</span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* User profile */}
        <div className="p-3 border-t border-ink-700/40">
          <div className={`flex items-center gap-3 ${!sidebarOpen && 'justify-center'}`}>
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-lime-400 to-lime-600 flex items-center justify-center text-ink-950 font-bold text-sm flex-shrink-0">
              {user?.name?.charAt(0).toUpperCase()}
            </div>
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-ink-100 truncate">{user?.name}</div>
                <div className="text-xs text-ink-400 truncate">{user?.email}</div>
              </div>
            )}
            {sidebarOpen && (
              <button onClick={handleLogout} className="text-ink-400 hover:text-coral-400 transition-colors" title="Logout">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
              </button>
            )}
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
