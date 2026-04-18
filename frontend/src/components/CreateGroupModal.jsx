import { useState } from 'react'
import { groupAPI } from '../api'
import toast from 'react-hot-toast'

const GROUP_TYPES = ['TRIP','HOME','OFFICE','COUPLE','OTHER']
const TYPE_EMOJI  = { TRIP:'✈️', HOME:'🏠', OFFICE:'💼', COUPLE:'💑', OTHER:'👥' }

export default function CreateGroupModal({ onClose, onCreated }) {
  const [form, setForm] = useState({ name: '', description: '', type: 'OTHER', currency: 'USD' })
  const [loading, setLoading] = useState(false)

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await groupAPI.create(form)
      toast.success(`Group "${form.name}" created!`)
      onCreated(data.data)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create group')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-ink-950/80 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-fade-in">
      <div className="glass rounded-2xl w-full max-w-md animate-fade-up">
        <div className="border-b border-ink-700/40 px-6 py-4 flex items-center justify-between">
          <h3 className="text-lg font-bold text-ink-100">Create Group</h3>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-100 transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Group name</label>
            <input required className="input-field" placeholder="Summer trip, Apartment, etc."
              value={form.name} onChange={e => set('name', e.target.value)} />
          </div>

          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Description (optional)</label>
            <input className="input-field" placeholder="Brief description…"
              value={form.description} onChange={e => set('description', e.target.value)} />
          </div>

          <div>
            <label className="text-sm text-ink-300 mb-2 block">Group type</label>
            <div className="grid grid-cols-5 gap-2">
              {GROUP_TYPES.map(type => (
                <button key={type} type="button"
                  className={`p-3 rounded-xl text-center transition-all ${
                    form.type === type
                      ? 'bg-lime-400/20 border border-lime-400/40'
                      : 'glass-light hover:border-ink-500/50'
                  }`}
                  onClick={() => set('type', type)}>
                  <div className="text-xl">{TYPE_EMOJI[type]}</div>
                  <div className={`text-xs mt-1 ${form.type === type ? 'text-lime-400' : 'text-ink-400'}`}>
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-sm text-ink-300 mb-1.5 block">Default currency</label>
            <select className="input-field" value={form.currency} onChange={e => set('currency', e.target.value)}>
              {['USD','EUR','GBP','INR','JPY','CAD','AUD','SGD'].map(c => (
                <option key={c} value={c} className="bg-ink-800">{c}</option>
              ))}
            </select>
          </div>

          <div className="flex gap-3 pt-1">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Cancel</button>
            <button type="submit" disabled={loading} className="btn-primary flex-1">
              {loading ? 'Creating…' : 'Create Group'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
