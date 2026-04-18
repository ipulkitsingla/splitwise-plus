import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { analyticsAPI } from '../api'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts'

const COLORS = ['#c8f547', '#7ff3b8', '#ffd166', '#ff6b6b', '#a78bfa', '#38bdf8', '#fb923c', '#34d399']

const MONTH_NAMES = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="glass rounded-xl p-3 text-xs border border-ink-600/40">
      <div className="text-ink-300 mb-1">{label}</div>
      {payload.map(p => (
        <div key={p.name} style={{ color: p.color }} className="font-num font-semibold">
          {p.name}: ${parseFloat(p.value).toFixed(2)}
        </div>
      ))}
    </div>
  )
}

export default function AnalyticsPage() {
  const { id: groupId } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    analyticsAPI.getGroupAnalytics(groupId)
      .then(r => setData(r.data.data))
      .finally(() => setLoading(false))
  }, [groupId])

  if (loading) return (
    <div className="p-6 space-y-4">
      {[1,2,3].map(i => <div key={i} className="glass rounded-2xl h-48 animate-pulse" />)}
    </div>
  )

  if (!data) return null

  const monthlyData = (data.monthlyTrends || []).reverse().map(t => ({
    month: MONTH_NAMES[t.month - 1],
    total: parseFloat(t.total),
  }))

  const categoryData = Object.entries(data.categoryBreakdown || {}).map(([name, value]) => ({
    name: name.charAt(0) + name.slice(1).toLowerCase(),
    value: parseFloat(value),
  }))

  const spenderData = (data.topSpenders || []).map(s => ({
    name: s.userName,
    paid: parseFloat(s.totalPaid),
    pct: parseFloat(s.percentage),
  }))

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="animate-fade-up">
        <h1 className="text-2xl font-bold text-ink-100">Analytics</h1>
        <p className="text-ink-400 text-sm mt-1">Spending insights for this group</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 stagger">
        {[
          { label: 'Total Spent', value: `$${parseFloat(data.totalExpenses || 0).toFixed(2)}`, accent: '#c8f547' },
          { label: 'You Owe',    value: `$${parseFloat(data.totalOwed  || 0).toFixed(2)}`, accent: '#ff6b6b' },
          { label: 'Owed to You', value: `$${parseFloat(data.totalOwe  || 0).toFixed(2)}`, accent: '#7ff3b8' },
          { label: 'Expenses',   value: data.expenseCount || 0, accent: '#ffd166' },
        ].map(card => (
          <div key={card.label} className="glass rounded-2xl p-5">
            <div className="text-ink-400 text-xs uppercase tracking-widest mb-3">{card.label}</div>
            <div className="font-num text-3xl font-bold" style={{ color: card.accent }}>{card.value}</div>
          </div>
        ))}
      </div>

      {/* Monthly trend */}
      {monthlyData.length > 0 && (
        <div className="glass rounded-2xl p-6 animate-fade-up">
          <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest mb-6">Monthly Spending Trend</h2>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={monthlyData}>
              <defs>
                <linearGradient id="totalGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#c8f547" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#c8f547" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(144,144,184,0.08)" />
              <XAxis dataKey="month" tick={{ fill: '#9090b8', fontSize: 12 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#9090b8', fontSize: 12 }} axisLine={false} tickLine={false}
                tickFormatter={v => `$${v}`} />
              <Tooltip content={<CustomTooltip />} />
              <Area type="monotone" dataKey="total" name="Total" stroke="#c8f547" strokeWidth={2}
                fill="url(#totalGrad)" dot={{ fill: '#c8f547', r: 3 }} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category breakdown */}
        {categoryData.length > 0 && (
          <div className="glass rounded-2xl p-6 animate-fade-up">
            <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest mb-6">By Category</h2>
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={categoryData} cx="50%" cy="50%" innerRadius={55} outerRadius={90}
                  dataKey="value" nameKey="name" paddingAngle={3}>
                  {categoryData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} stroke="transparent" />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend
                  formatter={(v) => <span className="text-xs text-ink-300">{v}</span>}
                  iconSize={8} iconType="circle"
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Top spenders */}
        {spenderData.length > 0 && (
          <div className="glass rounded-2xl p-6 animate-fade-up">
            <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest mb-6">Top Spenders</h2>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={spenderData} layout="vertical" barCategoryGap={8}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(144,144,184,0.08)" horizontal={false} />
                <XAxis type="number" tick={{ fill: '#9090b8', fontSize: 11 }} axisLine={false} tickLine={false}
                  tickFormatter={v => `$${v}`} />
                <YAxis type="category" dataKey="name" tick={{ fill: '#9090b8', fontSize: 12 }} axisLine={false} tickLine={false} width={70} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="paid" name="Paid" radius={[0, 6, 6, 0]}>
                  {spenderData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  )
}
