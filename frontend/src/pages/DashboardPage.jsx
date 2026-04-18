import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../store'
import { groupAPI, settlementAPI } from '../api'
import CreateGroupModal from '../components/CreateGroupModal'

function StatCard({ label, value, sub, accent = 'lime' }) {
  const colors = {
    lime: 'text-lime-400',
    coral: 'text-coral-400',
    amber: 'text-amber-400',
    neutral: 'text-ink-200',
  }
  return (
    <div className="glass rounded-2xl p-5 hover:border-ink-500/40 transition-all duration-200">
      <div className="text-ink-400 text-xs font-semibold uppercase tracking-widest mb-3">{label}</div>
      <div className={`font-num text-3xl font-bold ${colors[accent]} mb-1`}>{value}</div>
      {sub && <div className="text-ink-400 text-xs">{sub}</div>}
    </div>
  )
}

function GroupCard({ group }) {
  const typeEmoji = {
    TRIP: '✈️', HOME: '🏠', OFFICE: '💼', COUPLE: '💑', OTHER: '👥'
  }
  return (
    <Link
      to={`/groups/${group.id}`}
      className="glass rounded-2xl p-5 hover:border-lime-400/20 hover:bg-ink-700/30 transition-all duration-200 group block"
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-ink-700 flex items-center justify-center text-xl">
            {typeEmoji[group.type] || '👥'}
          </div>
          <div>
            <div className="font-semibold text-ink-100 group-hover:text-lime-400 transition-colors">
              {group.name}
            </div>
            <div className="text-ink-400 text-xs mt-0.5">{group.memberCount} members · {group.currency}</div>
          </div>
        </div>
        <svg className="w-4 h-4 text-ink-500 group-hover:text-lime-400 group-hover:translate-x-0.5 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
      </div>

      {/* Member avatars */}
      <div className="flex items-center gap-1">
        {(group.members || []).slice(0, 5).map((m, i) => (
          <div
            key={m.userId}
            className="w-6 h-6 rounded-full bg-gradient-to-br from-lime-400 to-lime-600 flex items-center justify-center text-ink-950 text-xs font-bold border-2 border-ink-800"
            style={{ zIndex: 5 - i, marginLeft: i > 0 ? '-6px' : 0 }}
            title={m.name}
          >
            {m.name?.charAt(0).toUpperCase()}
          </div>
        ))}
        {(group.memberCount || 0) > 5 && (
          <span className="text-ink-400 text-xs ml-2">+{group.memberCount - 5}</span>
        )}
      </div>

      {/* Quick links */}
      <div className="flex gap-2 mt-4 pt-4 border-t border-ink-700/40">
        {[
          { to: `expenses`, label: 'Expenses' },
          { to: `balances`, label: 'Balances' },
          { to: `analytics`, label: 'Analytics' },
        ].map(link => (
          <Link
            key={link.to}
            to={`/groups/${group.id}/${link.to}`}
            onClick={e => e.stopPropagation()}
            className="text-xs text-ink-400 hover:text-lime-400 px-2 py-1 rounded-md hover:bg-lime-400/10 transition-all"
          >
            {link.label}
          </Link>
        ))}
      </div>
    </Link>
  )
}

export default function DashboardPage() {
  const { user } = useAuthStore()
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreateGroup, setShowCreateGroup] = useState(false)
  const [totalOwed, setTotalOwed] = useState(0)
  const [totalOwe, setTotalOwe] = useState(0)

  useEffect(() => {
    async function load() {
      try {
        const { data } = await groupAPI.myGroups()
        const gs = data.data || []
        setGroups(gs)

        // Compute aggregate balances
        let owed = 0, owe = 0
        for (const g of gs) {
          try {
            const { data: bd } = await settlementAPI.getBalances(g.id)
            const txns = bd.data?.simplifiedTransactions || []
            txns.forEach(t => {
              if (t.toUserId === user?.id) owed += parseFloat(t.amount)
              if (t.fromUserId === user?.id) owe += parseFloat(t.amount)
            })
          } catch {}
        }
        setTotalOwed(owed)
        setTotalOwe(owe)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [user])

  const net = totalOwed - totalOwe

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-8 animate-fade-up">
        <div>
          <h1 className="text-2xl font-bold text-ink-100">
            Good {getGreeting()},{' '}
            <span className="gradient-text">{user?.name?.split(' ')[0]}</span>
          </h1>
          <p className="text-ink-400 text-sm mt-1">Here's your expense overview</p>
        </div>
        <button onClick={() => setShowCreateGroup(true)} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Group
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8 stagger">
        <StatCard
          label="You're owed"
          value={`$${totalOwed.toFixed(2)}`}
          sub="Across all groups"
          accent="lime"
        />
        <StatCard
          label="You owe"
          value={`$${totalOwe.toFixed(2)}`}
          sub="Outstanding debt"
          accent="coral"
        />
        <StatCard
          label="Net balance"
          value={`${net >= 0 ? '+' : ''}$${net.toFixed(2)}`}
          sub={net >= 0 ? 'You\'re ahead' : 'You\'re behind'}
          accent={net >= 0 ? 'lime' : 'coral'}
        />
        <StatCard
          label="Groups"
          value={groups.length}
          sub={`${groups.reduce((s, g) => s + (g.memberCount || 0), 0)} total members`}
          accent="neutral"
        />
      </div>

      {/* Groups grid */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest">Your Groups</h2>
        <span className="text-xs text-ink-500">{groups.length} groups</span>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="glass rounded-2xl h-44 animate-pulse" />
          ))}
        </div>
      ) : groups.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <div className="text-4xl mb-4">🏗️</div>
          <h3 className="text-ink-200 font-semibold mb-2">No groups yet</h3>
          <p className="text-ink-400 text-sm mb-6">Create a group to start tracking shared expenses</p>
          <button onClick={() => setShowCreateGroup(true)} className="btn-primary">
            Create your first group
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 stagger">
          {groups.map(g => <GroupCard key={g.id} group={g} />)}
        </div>
      )}

      {showCreateGroup && (
        <CreateGroupModal
          onClose={() => setShowCreateGroup(false)}
          onCreated={(g) => { setGroups(prev => [g, ...prev]); setShowCreateGroup(false) }}
        />
      )}
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
