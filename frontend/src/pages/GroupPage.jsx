import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { groupAPI } from '../api'
import toast from 'react-hot-toast'

export default function GroupPage() {
  const { id } = useParams()
  const [group, setGroup] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    groupAPI.get(id).then(r => setGroup(r.data.data)).finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <div className="p-6 space-y-4">
      <div className="glass rounded-2xl h-32 animate-pulse" />
      <div className="glass rounded-2xl h-48 animate-pulse" />
    </div>
  )

  if (!group) return null

  const typeEmoji = { TRIP:'✈️', HOME:'🏠', OFFICE:'💼', COUPLE:'💑', OTHER:'👥' }

  const navLinks = [
    { to: 'expenses', label: 'Expenses', icon: '💸', desc: 'View and add expenses' },
    { to: 'balances', label: 'Balances', icon: '⚖️', desc: 'Who owes what' },
    { to: 'analytics', label: 'Analytics', icon: '📊', desc: 'Spending insights' },
  ]

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Group header */}
      <div className="glass rounded-2xl p-6 mb-6 animate-fade-up">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-ink-700 flex items-center justify-center text-3xl">
              {typeEmoji[group.type] || '👥'}
            </div>
            <div>
              <h1 className="text-2xl font-bold text-ink-100">{group.name}</h1>
              {group.description && (
                <p className="text-ink-400 text-sm mt-1">{group.description}</p>
              )}
              <div className="flex items-center gap-3 mt-2">
                <span className="text-xs bg-ink-700 text-ink-300 px-2 py-0.5 rounded-full">{group.type}</span>
                <span className="text-xs bg-ink-700 text-ink-300 px-2 py-0.5 rounded-full">{group.currency}</span>
                <span className="text-xs text-ink-500">{group.memberCount} members</span>
              </div>
            </div>
          </div>

          {/* Invite code */}
          <div className="text-right">
            <div className="text-xs text-ink-500 mb-1">Invite code</div>
            <button
              className="font-mono text-sm font-bold text-lime-400 bg-lime-400/10 border border-lime-400/20 px-3 py-1.5 rounded-lg hover:bg-lime-400/15 transition-all"
              onClick={() => { navigator.clipboard.writeText(group.inviteCode); toast.success('Code copied!') }}
            >
              {group.inviteCode}
            </button>
          </div>
        </div>
      </div>

      {/* Quick navigation */}
      <div className="grid grid-cols-3 gap-4 mb-6 stagger">
        {navLinks.map(link => (
          <Link key={link.to} to={`/groups/${id}/${link.to}`}
            className="glass rounded-2xl p-5 hover:border-lime-400/20 hover:bg-ink-700/20 transition-all group">
            <div className="text-2xl mb-3">{link.icon}</div>
            <div className="font-semibold text-ink-100 group-hover:text-lime-400 transition-colors">
              {link.label}
            </div>
            <div className="text-ink-400 text-xs mt-1">{link.desc}</div>
          </Link>
        ))}
      </div>

      {/* Members */}
      <div className="glass rounded-2xl p-6 animate-fade-up">
        <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest mb-4">Members</h2>
        <div className="space-y-3">
          {(group.members || []).map(m => (
            <div key={m.userId} className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full bg-gradient-to-br from-lime-400 to-lime-600 flex items-center justify-center text-ink-950 font-bold text-sm flex-shrink-0">
                {m.name?.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1">
                <div className="text-ink-100 text-sm font-medium">{m.name}</div>
                <div className="text-ink-500 text-xs">{m.email}</div>
              </div>
              {m.role === 'ADMIN' && (
                <span className="text-xs bg-lime-400/15 text-lime-400 border border-lime-400/20 px-2 py-0.5 rounded-full">Admin</span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
