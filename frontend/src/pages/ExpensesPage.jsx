import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { expenseAPI, groupAPI } from '../api'
import { useDropzone } from 'react-dropzone'
import toast from 'react-hot-toast'

const CATEGORIES = ['FOOD','TRANSPORT','ACCOMMODATION','ENTERTAINMENT','UTILITIES','SHOPPING','HEALTHCARE','EDUCATION','SPORTS','OTHER']
const CAT_EMOJI = { FOOD:'🍔', TRANSPORT:'🚗', ACCOMMODATION:'🏨', ENTERTAINMENT:'🎭', UTILITIES:'💡', SHOPPING:'🛍️', HEALTHCARE:'💊', EDUCATION:'📚', SPORTS:'⚽', OTHER:'📦' }

function OcrScanner({ onResult }) {
  const [scanning, setScanning] = useState(false)
  const onDrop = useCallback(async (files) => {
    if (!files[0]) return
    setScanning(true)
    try {
      const { data } = await expenseAPI.scanReceipt(files[0])
      onResult(data.data)
      toast.success('Receipt scanned!')
    } catch { toast.error('OCR scan failed') }
    finally { setScanning(false) }
  }, [onResult])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop, accept: { 'image/*': [] }, multiple: false })

  return (
    <div {...getRootProps()} className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all duration-200 ${
      isDragActive ? 'border-lime-400 bg-lime-400/5' : 'border-ink-600/50 hover:border-ink-500'
    }`}>
      <input {...getInputProps()} />
      {scanning ? (
        <div className="flex flex-col items-center gap-2">
          <div className="w-6 h-6 border-2 border-lime-400 border-t-transparent rounded-full animate-spin" />
          <span className="text-ink-400 text-sm">Scanning receipt…</span>
        </div>
      ) : (
        <>
          <div className="text-2xl mb-2">📸</div>
          <p className="text-ink-300 text-sm font-medium">Scan Receipt with OCR</p>
          <p className="text-ink-500 text-xs mt-1">Drop image or click to upload</p>
        </>
      )}
    </div>
  )
}

function AddExpenseModal({ groupId, members, onClose, onAdded }) {
  const [form, setForm] = useState({
    description: '', amount: '', currency: 'USD',
    splitType: 'EQUAL', category: 'OTHER',
    expenseDate: new Date().toISOString().split('T')[0],
    paidById: members[0]?.userId || '',
    participantIds: members.map(m => m.userId),
    splitData: {},
    merchantName: '',
  })
  const [loading, setLoading] = useState(false)
  const [suggestions, setSuggestions] = useState([])

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const handleOcrResult = (result) => {
    if (result.extractedAmount) set('amount', result.extractedAmount)
    if (result.merchantName) {
      set('description', result.merchantName)
      set('merchantName', result.merchantName)
    }
  }

  const fetchSuggestions = async () => {
    if (!form.amount || form.splitType !== 'EQUAL') return
    try {
      const { data } = await expenseAPI.getSplitSuggestions(groupId, form.amount)
      setSuggestions(data.data || [])
    } catch {}
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const payload = {
        ...form,
        amount: parseFloat(form.amount),
        groupId: parseInt(groupId),
        paidById: parseInt(form.paidById),
        participantIds: form.participantIds.map(Number),
      }
      const { data } = await expenseAPI.create(payload)
      toast.success('Expense added!')
      onAdded(data.data)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add expense')
    } finally {
      setLoading(false)
    }
  }

  const toggleParticipant = (id) => {
    const ids = form.participantIds
    set('participantIds', ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id])
  }

  return (
    <div className="fixed inset-0 bg-ink-950/80 backdrop-blur-sm flex items-end sm:items-center justify-center z-50 p-4 animate-fade-in">
      <div className="glass rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto animate-fade-up">
        <div className="sticky top-0 glass border-b border-ink-700/40 px-6 py-4 flex items-center justify-between">
          <h3 className="text-lg font-bold text-ink-100">Add Expense</h3>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-100 transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <OcrScanner onResult={handleOcrResult} />

          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="text-sm text-ink-300 mb-1.5 block">Description</label>
              <input required className="input-field" placeholder="Dinner at restaurant"
                value={form.description} onChange={e => set('description', e.target.value)} />
            </div>
            <div>
              <label className="text-sm text-ink-300 mb-1.5 block">Amount</label>
              <input required type="number" min="0.01" step="0.01" className="input-field font-num"
                placeholder="0.00" value={form.amount}
                onChange={e => set('amount', e.target.value)}
                onBlur={fetchSuggestions} />
            </div>
            <div>
              <label className="text-sm text-ink-300 mb-1.5 block">Currency</label>
              <select className="input-field" value={form.currency} onChange={e => set('currency', e.target.value)}>
                {['USD','EUR','GBP','INR','JPY','CAD'].map(c => (
                  <option key={c} value={c} className="bg-ink-800">{c}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm text-ink-300 mb-1.5 block">Paid by</label>
              <select className="input-field" value={form.paidById} onChange={e => set('paidById', e.target.value)}>
                {members.map(m => (
                  <option key={m.userId} value={m.userId} className="bg-ink-800">{m.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-sm text-ink-300 mb-1.5 block">Date</label>
              <input type="date" className="input-field" value={form.expenseDate}
                onChange={e => set('expenseDate', e.target.value)} />
            </div>
          </div>

          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Category</label>
            <div className="grid grid-cols-5 gap-2">
              {CATEGORIES.map(cat => (
                <button key={cat} type="button"
                  className={`p-2 rounded-xl text-center transition-all duration-150 ${
                    form.category === cat
                      ? 'bg-lime-400/20 border border-lime-400/40 text-lime-400'
                      : 'glass-light text-ink-400 hover:text-ink-200'
                  }`}
                  onClick={() => set('category', cat)}>
                  <div className="text-lg">{CAT_EMOJI[cat]}</div>
                  <div className="text-xs mt-0.5 truncate">{cat.charAt(0) + cat.slice(1).toLowerCase()}</div>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Split type</label>
            <div className="flex gap-2">
              {['EQUAL','UNEQUAL','PERCENTAGE'].map(type => (
                <button key={type} type="button"
                  className={`flex-1 py-2 rounded-xl text-sm font-medium transition-all ${
                    form.splitType === type
                      ? 'bg-lime-400/20 border border-lime-400/40 text-lime-400'
                      : 'glass-light text-ink-400 hover:text-ink-200'
                  }`}
                  onClick={() => set('splitType', type)}>
                  {type.charAt(0) + type.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>

          {/* Participants for EQUAL split */}
          {form.splitType === 'EQUAL' && (
            <div>
              <label className="text-sm text-ink-300 mb-2 block">Participants</label>
              <div className="flex flex-wrap gap-2">
                {members.map(m => (
                  <button key={m.userId} type="button"
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-sm transition-all ${
                      form.participantIds.includes(m.userId)
                        ? 'bg-lime-400/15 border border-lime-400/30 text-lime-400'
                        : 'glass-light text-ink-400'
                    }`}
                    onClick={() => toggleParticipant(m.userId)}>
                    <div className="w-5 h-5 rounded-full bg-gradient-to-br from-lime-400 to-lime-600 flex items-center justify-center text-ink-950 text-xs font-bold">
                      {m.name?.charAt(0)}
                    </div>
                    {m.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Smart suggestions */}
          {suggestions.length > 0 && form.splitType === 'EQUAL' && (
            <div className="glass-light rounded-xl p-4">
              <div className="flex items-center gap-2 mb-3">
                <span className="text-lime-400 text-sm">✨</span>
                <span className="text-ink-300 text-xs font-semibold uppercase tracking-wider">Smart Split Suggestion</span>
              </div>
              {suggestions.map(s => (
                <div key={s.userId} className="flex items-center justify-between py-1.5">
                  <span className="text-ink-300 text-sm">{s.userName}</span>
                  <div className="text-right">
                    <span className="font-num text-lime-400 text-sm font-semibold">${parseFloat(s.suggestedAmount).toFixed(2)}</span>
                    <span className="text-ink-500 text-xs ml-2">({parseFloat(s.percentage).toFixed(0)}%)</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Cancel</button>
            <button type="submit" disabled={loading} className="btn-primary flex-1">
              {loading ? 'Adding…' : 'Add Expense'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function ExpensesPage() {
  const { id: groupId } = useParams()
  const [expenses, setExpenses] = useState([])
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [showAdd, setShowAdd] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [search, setSearch] = useState('')

  const load = async (reset = false) => {
    try {
      const p = reset ? 0 : page
      const { data } = await expenseAPI.getForGroup(groupId, { page: p, size: 20, search: search || undefined })
      const items = data.data?.content || []
      setExpenses(reset ? items : prev => [...prev, ...items])
      setHasMore(!data.data?.last)
      if (!reset) setPage(p + 1)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    groupAPI.get(groupId).then(r => setMembers(r.data.data?.members || []))
    load(true)
  }, [groupId, search])

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6 animate-fade-up">
        <div>
          <h1 className="text-2xl font-bold text-ink-100">Expenses</h1>
          <p className="text-ink-400 text-sm mt-1">{expenses.length} expenses loaded</p>
        </div>
        <button onClick={() => setShowAdd(true)} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Expense
        </button>
      </div>

      {/* Search */}
      <div className="relative mb-6 animate-fade-up">
        <svg className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-ink-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input className="input-field pl-10" placeholder="Search expenses…"
          value={search} onChange={e => setSearch(e.target.value)} />
      </div>

      {/* Expense list */}
      {loading ? (
        <div className="space-y-3">
          {[1,2,3,4].map(i => <div key={i} className="glass rounded-2xl h-20 animate-pulse" />)}
        </div>
      ) : expenses.length === 0 ? (
        <div className="glass rounded-2xl p-12 text-center">
          <div className="text-4xl mb-4">💸</div>
          <h3 className="text-ink-200 font-semibold">No expenses yet</h3>
          <p className="text-ink-400 text-sm mt-2 mb-6">Add the first expense to get started</p>
          <button onClick={() => setShowAdd(true)} className="btn-primary">Add first expense</button>
        </div>
      ) : (
        <div className="space-y-3 stagger">
          {expenses.map(exp => (
            <div key={exp.id} className="glass rounded-2xl p-4 flex items-center gap-4 hover:border-ink-500/40 transition-all">
              <div className="w-10 h-10 rounded-xl bg-ink-700/60 flex items-center justify-center text-xl flex-shrink-0">
                {CAT_EMOJI[exp.category] || '📦'}
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-medium text-ink-100 truncate">{exp.description}</div>
                <div className="text-ink-400 text-xs mt-0.5">
                  Paid by <span className="text-ink-200">{exp.paidBy?.name}</span>
                  {' · '}{new Date(exp.expenseDate).toLocaleDateString()}
                  {exp.recurring && <span className="ml-2 text-lime-400">🔁</span>}
                </div>
              </div>
              <div className="text-right flex-shrink-0">
                <div className="font-num font-bold text-ink-100 text-lg">
                  {exp.currency} {parseFloat(exp.amount).toFixed(2)}
                </div>
                <div className="text-xs text-ink-500">{exp.splitType?.toLowerCase()}</div>
              </div>
              {exp.settled && (
                <span className="text-xs bg-lime-400/15 text-lime-400 border border-lime-400/25 px-2 py-0.5 rounded-full">Settled</span>
              )}
            </div>
          ))}
        </div>
      )}

      {hasMore && !loading && (
        <div className="text-center mt-6">
          <button onClick={() => load()} className="btn-ghost">Load more</button>
        </div>
      )}

      {showAdd && (
        <AddExpenseModal
          groupId={groupId}
          members={members}
          onClose={() => setShowAdd(false)}
          onAdded={(exp) => { setExpenses(prev => [exp, ...prev]); setShowAdd(false) }}
        />
      )}
    </div>
  )
}
