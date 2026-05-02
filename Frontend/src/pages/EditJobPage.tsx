import React, { useState, useEffect, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getMyJobs, updateJob, type JobResponse } from '../api/jobs'

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE']
const EXPERIENCE_LEVELS = ['ENTRY', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE']
const JOB_STATUSES = ['ACTIVE', 'PAUSED', 'CLOSED']

export default function EditJobPage() {
  const { jobId } = useParams<{ jobId: string }>()
  const navigate = useNavigate()
  const [job, setJob] = useState<JobResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
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
    status: 'ACTIVE',
  })

  useEffect(() => {
    setLoading(true)
    getMyJobs(0, 100)
      .then(({ data }) => {
        const found = data.content.find((j: JobResponse) => j.id === jobId)
        if (found) {
          setJob(found)
          setForm({
            title: found.title,
            description: found.description,
            category: found.category,
            location: found.location ?? '',
            isRemote: found.isRemote,
            salaryMin: found.salaryMin?.toString() ?? '',
            salaryMax: found.salaryMax?.toString() ?? '',
            currency: found.currency,
            employmentType: found.employmentType,
            experienceLevel: found.experienceLevel,
            requiredSkills: found.requiredSkills?.join(', ') ?? '',
            status: found.status,
          })
        }
      })
      .catch(() => setError('Failed to load job.'))
      .finally(() => setLoading(false))
  }, [jobId])

  const set = (field: string, value: string | boolean) =>
    setForm((f) => ({ ...f, [field]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!jobId) return
    setError('')
    setSaving(true)
    try {
      await updateJob(jobId, {
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
      })
      navigate('/jobs/my')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Failed to update job.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="text-center py-20 text-gray-400">Loading…</div>
  if (!job) return <div className="text-center py-20 text-gray-400">Job not found.</div>

  const input = (name: string, type = 'text', placeholder = '') => (
    <input
      type={type}
      value={form[name as keyof typeof form] as string}
      onChange={(e) => set(name, e.target.value)}
      placeholder={placeholder}
      className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
    />
  )

  const sel = (name: string, options: string[]) => (
    <select
      value={form[name as keyof typeof form] as string}
      onChange={(e) => set(name, e.target.value)}
      className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 bg-white"
    >
      {options.map((o) => <option key={o} value={o}>{o.replace(/_/g, ' ')}</option>)}
    </select>
  )

  const field = (label: string, el: React.ReactNode) => (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-gray-700">{label}</label>
      {el}
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Edit Job</h1>

        {error && (
          <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm p-6 flex flex-col gap-5">
          {field('Job title', input('title'))}
          {field('Category', input('category'))}
          {field('Description',
            <textarea
              value={form.description}
              onChange={(e) => set('description', e.target.value)}
              rows={5}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-y"
            />
          )}
          <div className="grid grid-cols-2 gap-4">
            {field('Employment type', sel('employmentType', EMPLOYMENT_TYPES))}
            {field('Experience level', sel('experienceLevel', EXPERIENCE_LEVELS))}
          </div>
          <div className="grid grid-cols-2 gap-4">
            {field('Location', input('location', 'text', 'e.g. Singapore'))}
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">Remote</label>
              <label className="flex items-center gap-2 mt-2 cursor-pointer">
                <input type="checkbox" checked={form.isRemote}
                  onChange={(e) => set('isRemote', e.target.checked)}
                  className="w-4 h-4 accent-purple-600" />
                <span className="text-sm text-gray-600">Remote-friendly</span>
              </label>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            {field('Currency', sel('currency', ['USD', 'SGD', 'EUR', 'GBP', 'AUD']))}
            {field('Salary min', input('salaryMin', 'number'))}
            {field('Salary max', input('salaryMax', 'number'))}
          </div>
          {field('Required skills (comma-separated)', input('requiredSkills', 'text', 'Java, Spring Boot'))}
          {field('Status', sel('status', JOB_STATUSES))}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => navigate('/jobs/my')}
              className="flex-1 py-2.5 border border-gray-300 rounded-xl text-gray-600 hover:bg-gray-50 text-sm font-medium">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="flex-1 py-2.5 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 disabled:opacity-60 text-sm">
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
