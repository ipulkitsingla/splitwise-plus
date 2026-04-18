import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store'
import { authAPI } from '../api'
import toast from 'react-hot-toast'

function AuthLayout({ children, title, subtitle }) {
  return (
    <div className="min-h-screen bg-ink-950 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background glows */}
      <div className="absolute top-[-20%] left-[-10%] w-[500px] h-[500px] bg-lime-400/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[400px] h-[400px] bg-ink-600/20 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10 animate-fade-up">
        {/* Logo */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 bg-lime-400 rounded-xl flex items-center justify-center">
            <span className="text-ink-950 font-bold text-base">S+</span>
          </div>
          <div>
            <div className="font-bold text-ink-100 leading-tight">Splitwise++</div>
            <div className="text-ink-400 text-xs">Smart Expense Splitter</div>
          </div>
        </div>

        <div className="glass rounded-2xl p-8">
          <h1 className="text-2xl font-bold text-ink-100 mb-1">{title}</h1>
          <p className="text-ink-400 text-sm mb-8">{subtitle}</p>
          {children}
        </div>
      </div>
    </div>
  )
}

export function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const setAuth = useAuthStore(s => s.setAuth)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await authAPI.login(form)
      setAuth(data.data)
      toast.success(`Welcome back, ${data.data.user.name}!`)
      navigate('/')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your account to continue">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Email</label>
          <input
            type="email" required
            className="input-field"
            placeholder="you@example.com"
            value={form.email}
            onChange={e => setForm({ ...form, email: e.target.value })}
          />
        </div>
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Password</label>
          <input
            type="password" required
            className="input-field"
            placeholder="••••••••"
            value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })}
          />
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="text-center text-ink-400 text-sm mt-6">
        Don't have an account?{' '}
        <Link to="/register" className="text-lime-400 hover:text-lime-300 font-medium">
          Create one
        </Link>
      </p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '', preferredCurrency: 'USD' })
  const [loading, setLoading] = useState(false)
  const setAuth = useAuthStore(s => s.setAuth)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await authAPI.register(form)
      setAuth(data.data)
      toast.success('Account created!')
      navigate('/')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Create account" subtitle="Join Splitwise++ and start splitting smarter">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Full name</label>
          <input type="text" required className="input-field" placeholder="Alex Johnson"
            value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        </div>
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Email</label>
          <input type="email" required className="input-field" placeholder="you@example.com"
            value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
        </div>
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Password</label>
          <input type="password" required minLength={8} className="input-field" placeholder="Min. 8 characters"
            value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} />
        </div>
        <div>
          <label className="text-sm text-ink-300 mb-1.5 block">Currency</label>
          <select className="input-field" value={form.preferredCurrency}
            onChange={e => setForm({ ...form, preferredCurrency: e.target.value })}>
            {['USD','EUR','GBP','INR','JPY','CAD','AUD','SGD'].map(c => (
              <option key={c} value={c} className="bg-ink-800">{c}</option>
            ))}
          </select>
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="text-center text-ink-400 text-sm mt-6">
        Already have an account?{' '}
        <Link to="/login" className="text-lime-400 hover:text-lime-300 font-medium">Sign in</Link>
      </p>
    </AuthLayout>
  )
}

export default LoginPage
