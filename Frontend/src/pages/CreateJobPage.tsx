import React, { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createJob } from '../api/jobs'

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE']
const EXPERIENCE_LEVELS = ['ENTRY', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE']
const CURRENCIES = ['USD', 'SGD', 'EUR', 'GBP', 'AUD']

export default function CreateJobPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [form, setForm] = useState({
    title: '',
    description: '',
    category: '',
    location: '',
    isRemote: false,
    salaryMin: '',
    salaryMax: '',
    currency: 'USD',
    employmentType: 'FULL_TIME',
    experienceLevel: 'MID',
    requiredSkills: '',
    expiresAt: '',
  })

  const set = (field: string, value: string | boolean) =>
    setForm((f) => ({ ...f, [field]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await createJob({
        title: form.title,
        description: form.description,
        category: form.category,
        location: form.location || undefined,
        isRemote: form.isRemote,
        salaryMin: form.salaryMin ? Number(form.salaryMin) : undefined,
        salaryMax: form.salaryMax ? Number(form.salaryMax) : undefined,
        currency: form.currency,
        employmentType: form.employmentType,
        experienceLevel: form.experienceLevel,
        requiredSkills: form.requiredSkills
          ? form.requiredSkills.split(',').map((s) => s.trim()).filter(Boolean)
          : [],
        expiresAt: form.expiresAt ? `${form.expiresAt}T00:00:00Z` : undefined,
      })
      navigate('/jobs/my')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Failed to create job posting.')
    } finally {
      setLoading(false)
    }
  }

  const field = (label: string, el: React.ReactNode) => (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
      {el}
    </div>
  )

  const input = (name: string, type = 'text', placeholder = '') => (
    <input
      type={type}
      value={form[name as keyof typeof form] as string}
      onChange={(e) => set(name, e.target.value)}
      placeholder={placeholder}
      className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500"
    />
  )

  const select = (name: string, options: string[]) => (
    <select
      value={form[name as keyof typeof form] as string}
      onChange={(e) => set(name, e.target.value)}
      className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
    >
      {options.map((o) => (
        <option key={o} value={o}>{o.replace(/_/g, ' ')}</option>
      ))}
    </select>
  )

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">Post a Job</h1>

        {error && (
          <div className="mb-4 text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-2xl shadow-sm p-6 flex flex-col gap-5">
          {field('Job title *', input('title', 'text', 'e.g. Senior Backend Engineer'))}
          {field('Category *', input('category', 'text', 'e.g. Engineering'))}

          {field('Description *',
            <textarea
              value={form.description}
              onChange={(e) => set('description', e.target.value)}
              required
              rows={5}
              placeholder="Describe the role, responsibilities, requirements…"
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-y"
            />
          )}

          <div className="grid grid-cols-2 gap-4">
            {field('Employment type *', select('employmentType', EMPLOYMENT_TYPES))}
            {field('Experience level *', select('experienceLevel', EXPERIENCE_LEVELS))}
          </div>

          <div className="grid grid-cols-2 gap-4">
            {field('Location', input('location', 'text', 'e.g. Singapore'))}
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Remote</label>
              <label className="flex items-center gap-2 mt-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.isRemote}
                  onChange={(e) => set('isRemote', e.target.checked)}
                  className="w-4 h-4 accent-purple-600"
                />
                <span className="text-sm text-gray-600 dark:text-gray-300">Remote-friendly</span>
              </label>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            {field('Currency', select('currency', CURRENCIES))}
            {field('Salary min', input('salaryMin', 'number', '60000'))}
            {field('Salary max', input('salaryMax', 'number', '100000'))}
          </div>

          {field('Required skills (comma-separated)',
            input('requiredSkills', 'text', 'Java, Spring Boot, PostgreSQL')
          )}

          {field('Expires at', input('expiresAt', 'date'))}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate('/jobs/my')}
              className="flex-1 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 disabled:opacity-60 text-sm"
            >
              {loading ? 'Posting…' : 'Post job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
