import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { settlementAPI } from '../api'
import { useAuthStore } from '../store'
import toast from 'react-hot-toast'

function SettleModal({ balance, onClose, onSettled }) {
  const [amount, setAmount] = useState(balance.amount)
  const [method, setMethod] = useState('CASH')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(false)
  const { id: groupId } = useParams()

  const handleSettle = async () => {
    setLoading(true)
    try {
      await settlementAPI.settle({
        groupId: parseInt(groupId),
        receiverId: balance.toUserId,
        amount: parseFloat(amount),
        currency: balance.currency || 'USD',
        paymentMethod: method,
        note,
      })
      toast.success(`Payment of $${amount} recorded!`)
      onSettled()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to record payment')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-ink-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-fade-in">
      <div className="glass rounded-2xl p-6 w-full max-w-md animate-fade-up">
        <h3 className="text-lg font-bold text-ink-100 mb-1">Record Payment</h3>
        <p className="text-ink-400 text-sm mb-6">
          Pay <span className="text-lime-400 font-medium">{balance.toUserName}</span>
        </p>

        <div className="space-y-4">
          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Amount ({balance.currency})</label>
            <input type="number" min="0.01" step="0.01" className="input-field font-num"
              value={amount} onChange={e => setAmount(e.target.value)} />
          </div>
          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Payment method</label>
            <select className="input-field" value={method} onChange={e => setMethod(e.target.value)}>
              {['CASH','UPI','BANK_TRANSFER','PAYPAL','VENMO','OTHER'].map(m => (
                <option key={m} value={m} className="bg-ink-800">{m.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Note (optional)</label>
            <input type="text" className="input-field" placeholder="e.g. Paid via GPay"
              value={note} onChange={e => setNote(e.target.value)} />
          </div>
        </div>

        <div className="flex gap-3 mt-6">
          <button onClick={onClose} className="btn-ghost flex-1">Cancel</button>
          <button onClick={handleSettle} disabled={loading} className="btn-primary flex-1">
            {loading ? 'Recording…' : 'Record Payment'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function BalancesPage() {
  const { id: groupId } = useParams()
  const { user } = useAuthStore()
  const [summary, setSummary] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [settleTarget, setSettleTarget] = useState(null)

  const load = async () => {
    try {
      const [bRes, hRes] = await Promise.all([
        settlementAPI.getBalances(groupId),
        settlementAPI.getHistory(groupId),
      ])
      setSummary(bRes.data.data)
      setHistory(hRes.data.data || [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [groupId])

  if (loading) return (
    <div className="p-6 space-y-4">
      {[1,2,3].map(i => <div key={i} className="glass rounded-2xl h-20 animate-pulse" />)}
    </div>
  )

  const txns = summary?.simplifiedTransactions || []
  const myTxns = txns.filter(t => t.fromUserId === user?.id || t.toUserId === user?.id)
  const otherTxns = txns.filter(t => t.fromUserId !== user?.id && t.toUserId !== user?.id)

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <div className="animate-fade-up">
        <h1 className="text-2xl font-bold text-ink-100">Balances</h1>
        <p className="text-ink-400 text-sm mt-1">
          {txns.length} optimized transaction{txns.length !== 1 ? 's' : ''} needed to settle all debts
        </p>
      </div>

      {/* Algorithm badge */}
      <div className="flex items-center gap-2 glass rounded-xl px-4 py-2 w-fit animate-fade-up">
        <span className="text-lime-400 text-sm">⚡</span>
        <span className="text-ink-300 text-xs">Debt minimization algorithm applied — reduced to fewest possible transactions</span>
      </div>

      {/* My transactions */}
      {myTxns.length > 0 && (
        <div className="space-y-3 animate-fade-up">
          <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest">Your Balances</h2>
          {myTxns.map((t, i) => {
            const isDebtor = t.fromUserId === user?.id
            return (
              <div key={i} className="glass rounded-2xl p-4 flex items-center gap-4">
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg ${
                  isDebtor ? 'bg-coral-400/15' : 'bg-lime-400/15'
                }`}>
                  {isDebtor ? '↑' : '↓'}
                </div>
                <div className="flex-1">
                  <div className="text-ink-100 font-medium text-sm">
                    {isDebtor
                      ? <>You owe <span className="text-lime-400">{t.toUserName}</span></>
                      : <><span className="text-lime-400">{t.fromUserName}</span> owes you</>
                    }
                  </div>
                  <div className="text-ink-400 text-xs mt-0.5">{t.currency}</div>
                </div>
                <div className={`font-num font-bold text-xl ${isDebtor ? 'text-coral-400' : 'text-lime-400'}`}>
                  ${parseFloat(t.amount).toFixed(2)}
                </div>
                {isDebtor && (
                  <button onClick={() => setSettleTarget(t)} className="btn-primary text-sm px-4 py-2">
                    Settle
                  </button>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* Other transactions */}
      {otherTxns.length > 0 && (
        <div className="space-y-3 animate-fade-up">
          <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest">Other Debts</h2>
          {otherTxns.map((t, i) => (
            <div key={i} className="glass rounded-2xl p-4 flex items-center gap-4 opacity-70">
              <div className="flex-1 text-ink-300 text-sm">
                <span className="text-ink-100 font-medium">{t.fromUserName}</span>
                {' → '}
                <span className="text-ink-100 font-medium">{t.toUserName}</span>
              </div>
              <div className="font-num text-ink-200 font-semibold">
                ${parseFloat(t.amount).toFixed(2)}
              </div>
            </div>
          ))}
        </div>
      )}

      {txns.length === 0 && (
        <div className="glass rounded-2xl p-12 text-center">
          <div className="text-4xl mb-4">✅</div>
          <h3 className="text-ink-200 font-semibold">All settled up!</h3>
          <p className="text-ink-400 text-sm mt-2">No outstanding debts in this group</p>
        </div>
      )}

      {/* Settlement history */}
      {history.length > 0 && (
        <div className="space-y-3 animate-fade-up">
          <h2 className="text-sm font-semibold text-ink-300 uppercase tracking-widest">Payment History</h2>
          {history.slice(0, 10).map(s => (
            <div key={s.id} className="glass-light rounded-xl p-4 flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-ink-600/50 flex items-center justify-center text-sm">💸</div>
              <div className="flex-1">
                <div className="text-ink-200 text-sm">
                  <span className="font-medium">{s.payer?.name}</span>
                  {' paid '}
                  <span className="font-medium">{s.receiver?.name}</span>
                </div>
                <div className="text-ink-500 text-xs mt-0.5">
                  {s.paymentMethod?.replace('_', ' ')} · {new Date(s.settledAt).toLocaleDateString()}
                </div>
              </div>
              <div className="font-num text-lime-400 font-semibold">
                ${parseFloat(s.amount).toFixed(2)}
              </div>
            </div>
          ))}
        </div>
      )}

      {settleTarget && (
        <SettleModal
          balance={settleTarget}
          onClose={() => setSettleTarget(null)}
          onSettled={() => { setSettleTarget(null); load() }}
        />
      )}
    </div>
  )
}
