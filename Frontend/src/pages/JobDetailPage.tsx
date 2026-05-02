import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getJob } from '../api/jobs'
import { apply } from '../api/applications'
import type { JobResponse } from '../types/api'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import { useAuth } from '../contexts/AuthContext'

const EMPLOYMENT_LABELS: Record<string, string> = {
  FULL_TIME: 'Full-time', PART_TIME: 'Part-time', CONTRACT: 'Contract',
  INTERNSHIP: 'Internship', FREELANCE: 'Freelance',
}
const LEVEL_COLOR: Record<string, 'green' | 'blue' | 'purple' | 'yellow' | 'red'> = {
  ENTRY: 'green', MID: 'blue', SENIOR: 'purple', LEAD: 'yellow', EXECUTIVE: 'red',
}

type ApplyState = 'idle' | 'applying' | 'applied' | 'error' | 'must-like' | 'duplicate'

export default function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [job, setJob] = useState<JobResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [applyState, setApplyState] = useState<ApplyState>('idle')
  const [coverLetter, setCoverLetter] = useState('')
  const [showApplyForm, setShowApplyForm] = useState(false)

  useEffect(() => {
    if (!jobId) return
    getJob(jobId)
      .then(({ data }) => setJob(data))
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true)
      })
      .finally(() => setLoading(false))
  }, [jobId])

  const handleApply = async () => {
    if (!jobId) return
    setApplyState('applying')
    try {
      await apply(jobId, { coverLetter: coverLetter || undefined })
      setApplyState('applied')
      setShowApplyForm(false)
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 422) setApplyState('must-like')
      else if (status === 409) setApplyState('duplicate')
      else setApplyState('error')
    }
  }

  if (loading) return <div className="py-20"><Spinner className="py-20" /></div>

  if (notFound) return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <p className="text-3xl mb-3">🔍</p>
        <p className="text-lg font-medium text-gray-700">Job not found</p>
        <p className="text-sm text-gray-400 mt-1 mb-6">This job may have been closed or removed.</p>
        <Button variant="secondary" onClick={() => navigate(-1)}>Go back</Button>
      </div>
    </div>
  )

  if (!job) return null

  const salary =
    job.salaryMin && job.salaryMax
      ? `${job.currency} ${job.salaryMin.toLocaleString()} – ${job.salaryMax.toLocaleString()}`
      : job.salaryMin ? `${job.currency} ${job.salaryMin.toLocaleString()}+` : null

  const isCandidate = user?.role === 'ROLE_USER'

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-purple-600 mb-6 transition"
        >
          ← Back
        </button>

        <div className="bg-white rounded-2xl shadow-sm p-6 flex flex-col gap-5">
          {/* Header */}
          <div className="flex justify-between items-start gap-2">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{job.title}</h1>
              <p className="text-gray-500 mt-0.5">{job.category}</p>
            </div>
            {job.boostScore > 0 && <Badge color="yellow">Featured</Badge>}
          </div>

          {/* Meta badges */}
          <div className="flex flex-wrap gap-2">
            {job.location && <span className="text-sm text-gray-600">📍 {job.location}</span>}
            {job.isRemote && <Badge color="teal">Remote</Badge>}
            <Badge color="gray">{EMPLOYMENT_LABELS[job.employmentType] ?? job.employmentType}</Badge>
            <Badge color={LEVEL_COLOR[job.experienceLevel] ?? 'gray'}>{job.experienceLevel}</Badge>
            {job.status !== 'ACTIVE' && <Badge color="red">{job.status}</Badge>}
          </div>

          {salary && <p className="text-lg font-semibold text-green-700">{salary} / yr</p>}

          {/* Description */}
          <div>
            <h2 className="text-sm font-semibold text-gray-700 mb-2">About this role</h2>
            <p className="text-sm text-gray-600 whitespace-pre-line">{job.description}</p>
          </div>

          {/* Skills */}
          {job.requiredSkills?.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-gray-700 mb-2">Required skills</h2>
              <div className="flex flex-wrap gap-1.5">
                {job.requiredSkills.map((skill) => (
                  <span key={skill} className="text-xs bg-purple-50 text-purple-700 border border-purple-200 px-2.5 py-1 rounded">
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Stats */}
          <p className="text-xs text-gray-400">
            {job.applicationCount} applicant{job.applicationCount !== 1 ? 's' : ''} ·{' '}
            Posted {new Date(job.createdAt).toLocaleDateString()}
            {job.expiresAt && ` · Expires ${new Date(job.expiresAt).toLocaleDateString()}`}
          </p>

          {/* Apply section — candidates only */}
          {isCandidate && job.status === 'ACTIVE' && (
            <div className="border-t pt-5">
              {applyState === 'applied' || applyState === 'duplicate' ? (
                <div className="flex items-center gap-2 text-green-700 bg-green-50 border border-green-200 rounded-lg p-3 text-sm">
                  ✓ You have applied to this job
                </div>
              ) : applyState === 'must-like' ? (
                <div className="text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm">
                  You need to swipe 💘 Like on this job from the feed before you can apply.
                </div>
              ) : applyState === 'error' ? (
                <div className="text-red-600 bg-red-50 border border-red-200 rounded-lg p-3 text-sm mb-3">
                  Something went wrong. Please try again.
                </div>
              ) : null}

              {!['applied', 'duplicate', 'must-like'].includes(applyState) && (
                <>
                  {showApplyForm && (
                    <div className="mb-4">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Cover letter <span className="text-gray-400 font-normal">(optional)</span>
                      </label>
                      <textarea
                        value={coverLetter}
                        onChange={(e) => setCoverLetter(e.target.value)}
                        rows={4}
                        placeholder="Tell them why you're a great fit…"
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-y"
                      />
                    </div>
                  )}
                  <div className="flex gap-3">
                    {!showApplyForm ? (
                      <Button className="flex-1" onClick={() => setShowApplyForm(true)}>
                        Apply now
                      </Button>
                    ) : (
                      <>
                        <Button variant="secondary" className="flex-1" onClick={() => setShowApplyForm(false)}>
                          Cancel
                        </Button>
                        <Button
                          className="flex-1"
                          loading={applyState === 'applying'}
                          onClick={handleApply}
                        >
                          Submit application
                        </Button>
                      </>
                    )}
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
