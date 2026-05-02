import { useState } from 'react'
import type { JobFeedParams } from '../../types/api'
import Input from '../ui/Input'
import Select from '../ui/Select'
import Button from '../ui/Button'

interface Props {
  onApply: (filters: Omit<JobFeedParams, 'cursor' | 'size'>) => void
}

const CATEGORIES = [
  { value: '', label: 'All categories' },
  ...['Engineering', 'Design', 'Product', 'Marketing', 'Sales', 'Finance',
      'Operations', 'HR', 'Legal', 'Customer Support', 'Data Science', 'Other']
    .map((c) => ({ value: c, label: c })),
]

export default function FeedFilters({ onApply }: Props) {
  const [open, setOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [location, setLocation] = useState('')
  const [remote, setRemote] = useState(false)
  const [salaryMin, setSalaryMin] = useState('')
  const [salaryMax, setSalaryMax] = useState('')

  const apply = () => {
    onApply({
      keyword: keyword || undefined,
      category: category || undefined,
      location: location || undefined,
      remote: remote || undefined,
      salaryMin: salaryMin ? Number(salaryMin) : undefined,
      salaryMax: salaryMax ? Number(salaryMax) : undefined,
    })
    setOpen(false)
  }

  const reset = () => {
    setKeyword('')
    setCategory('')
    setLocation('')
    setRemote(false)
    setSalaryMin('')
    setSalaryMax('')
    onApply({})
    setOpen(false)
  }

  const activeCount = [keyword, category, location, remote, salaryMin, salaryMax].filter(Boolean).length

  return (
    <div className="mb-5">
      {/* Search bar + filter toggle */}
      <div className="flex gap-2">
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && apply()}
          placeholder="Search jobs… (press Enter)"
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
        />
        <button
          type="button"
          onClick={() => setOpen((o) => !o)}
          className={`flex items-center gap-1.5 px-4 py-2 border rounded-lg text-sm font-medium transition ${
            open || activeCount > 0
              ? 'border-purple-500 text-purple-600 bg-purple-50'
              : 'border-gray-300 text-gray-600 hover:border-purple-400'
          }`}
        >
          Filters{activeCount > 0 && (
            <span className="ml-1 w-5 h-5 rounded-full bg-purple-600 text-white text-xs flex items-center justify-center">
              {activeCount}
            </span>
          )}
        </button>
        <Button onClick={apply} size="sm">Search</Button>
      </div>

      {/* Collapsible filter panel */}
      {open && (
        <div className="mt-3 bg-white border border-gray-200 rounded-xl p-4 flex flex-col gap-4 shadow-sm">
          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Category"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              options={CATEGORIES}
            />
            <Input
              label="Location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="e.g. Singapore"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Min salary"
              type="number"
              value={salaryMin}
              onChange={(e) => setSalaryMin(e.target.value)}
              placeholder="60000"
            />
            <Input
              label="Max salary"
              type="number"
              value={salaryMax}
              onChange={(e) => setSalaryMax(e.target.value)}
              placeholder="120000"
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={remote}
              onChange={(e) => setRemote(e.target.checked)}
              className="w-4 h-4 accent-purple-600"
            />
            <span className="text-sm text-gray-700">Remote only</span>
          </label>

          <div className="flex gap-2 pt-1">
            <Button variant="secondary" size="sm" onClick={reset} className="flex-1">
              Reset filters
            </Button>
            <Button size="sm" onClick={apply} className="flex-1">
              Apply filters
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
