import { useState } from 'react'
import type { JobResponse } from '../../types/api'
import Badge from '../ui/Badge'
import { Link } from 'react-router-dom'

interface Props {
  job: JobResponse
  onAction: (action: 'LIKE' | 'PASS') => void
  disabled?: boolean
}

const EMPLOYMENT_LABELS: Record<string, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  CONTRACT: 'Contract',
  INTERNSHIP: 'Internship',
  FREELANCE: 'Freelance',
}

const LEVEL_COLOR: Record<string, 'green' | 'blue' | 'purple' | 'yellow' | 'red'> = {
  ENTRY: 'green',
  MID: 'blue',
  SENIOR: 'purple',
  LEAD: 'yellow',
  EXECUTIVE: 'red',
}

export default function SwipeCard({ job, onAction, disabled = false }: Props) {
  const [swipeDir, setSwipeDir] = useState<'left' | 'right' | null>(null)

  const trigger = (dir: 'left' | 'right', action: 'LIKE' | 'PASS') => {
    if (disabled || swipeDir) return
    setSwipeDir(dir)
    setTimeout(() => {
      setSwipeDir(null)
      onAction(action)
    }, 280)
  }

  const salary =
    job.salaryMin && job.salaryMax
      ? `${job.currency} ${job.salaryMin.toLocaleString()} – ${job.salaryMax.toLocaleString()}`
      : job.salaryMin
      ? `${job.currency} ${job.salaryMin.toLocaleString()}+`
      : null

  const cardStyle: React.CSSProperties =
    swipeDir === 'right'
      ? { transform: 'translateX(120%) rotate(15deg)', opacity: 0, transition: 'all 0.28s ease-out' }
      : swipeDir === 'left'
      ? { transform: 'translateX(-120%) rotate(-15deg)', opacity: 0, transition: 'all 0.28s ease-out' }
      : { transition: 'all 0.28s ease-out' }

  return (
    <div style={cardStyle} className="bg-white dark:bg-gray-800 rounded-2xl shadow-md">
      {/* Card header */}
      <div className="p-6 pb-4">
        <div className="flex justify-between items-start gap-2 mb-3">
          <div>
            <Link
              to={`/jobs/${job.id}`}
              className="text-xl font-semibold text-gray-900 dark:text-gray-100 hover:text-purple-600 transition"
            >
              {job.title}
            </Link>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{job.category}</p>
          </div>
          {job.boostScore > 0 && <Badge color="yellow">Featured</Badge>}
        </div>

        <div className="flex flex-wrap gap-2 mb-3">
          {job.location && (
            <span className="text-sm text-gray-600 dark:text-gray-400">📍 {job.location}</span>
          )}
          {job.isRemote && <Badge color="teal">Remote</Badge>}
          <Badge color="gray">{EMPLOYMENT_LABELS[job.employmentType] ?? job.employmentType}</Badge>
          <Badge color={LEVEL_COLOR[job.experienceLevel] ?? 'gray'}>
            {job.experienceLevel}
          </Badge>
        </div>

        {salary && (
          <p className="text-sm font-semibold text-green-700 mb-3">{salary} / yr</p>
        )}

        <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-4 mb-3">{job.description}</p>

        {job.requiredSkills?.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {job.requiredSkills.map((skill) => (
              <span
                key={skill}
                className="text-xs bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 border border-purple-200 dark:border-purple-700 px-2 py-0.5 rounded"
              >
                {skill}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="px-6 pb-6 flex gap-3">
        <button
          onClick={() => trigger('left', 'PASS')}
          disabled={disabled || !!swipeDir}
          className="flex-1 py-3 border-2 border-gray-200 dark:border-gray-600 rounded-xl text-gray-500 dark:text-gray-400 font-semibold hover:border-gray-400 hover:text-gray-700 dark:hover:border-gray-400 dark:hover:text-gray-200 disabled:opacity-40 transition text-lg"
          aria-label="Pass"
        >
          ✗ Pass
        </button>
        <button
          onClick={() => trigger('right', 'LIKE')}
          disabled={disabled || !!swipeDir}
          className="flex-1 py-3 bg-purple-600 rounded-xl text-white font-semibold hover:bg-purple-700 disabled:opacity-40 transition text-lg"
          aria-label="Like"
        >
          💘 Like
        </button>
      </div>
    </div>
  )
}
