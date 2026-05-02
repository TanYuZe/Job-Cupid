import type { JobResponse } from '../api/jobs'

interface Props {
  job: JobResponse
  onLike?: () => void
  onPass?: () => void
  showActions?: boolean
}

const EMPLOYMENT_LABELS: Record<string, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  CONTRACT: 'Contract',
  INTERNSHIP: 'Internship',
  FREELANCE: 'Freelance',
}

const LEVEL_COLORS: Record<string, string> = {
  ENTRY: 'bg-green-100 text-green-700',
  MID: 'bg-blue-100 text-blue-700',
  SENIOR: 'bg-purple-100 text-purple-700',
  LEAD: 'bg-orange-100 text-orange-700',
  EXECUTIVE: 'bg-red-100 text-red-700',
}

export default function JobCard({ job, onLike, onPass, showActions = false }: Props) {
  const salary =
    job.salaryMin && job.salaryMax
      ? `${job.currency} ${job.salaryMin.toLocaleString()} – ${job.salaryMax.toLocaleString()}`
      : job.salaryMin
      ? `${job.currency} ${job.salaryMin.toLocaleString()}+`
      : null

  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6 flex flex-col gap-4">
      <div className="flex justify-between items-start">
        <div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{job.title}</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{job.category}</p>
        </div>
        {job.boostScore > 0 && (
          <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full font-medium">
            Featured
          </span>
        )}
      </div>

      <div className="flex flex-wrap gap-2 text-sm">
        {job.location && (
          <span className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
            📍 {job.location}
          </span>
        )}
        {job.isRemote && (
          <span className="bg-teal-100 dark:bg-teal-900/30 text-teal-700 dark:text-teal-400 px-2 py-0.5 rounded-full">
            Remote
          </span>
        )}
        <span className="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-0.5 rounded-full">
          {EMPLOYMENT_LABELS[job.employmentType] ?? job.employmentType}
        </span>
        <span className={`px-2 py-0.5 rounded-full ${LEVEL_COLORS[job.experienceLevel] ?? 'bg-gray-100 text-gray-700'}`}>
          {job.experienceLevel}
        </span>
      </div>

      {salary && (
        <p className="text-sm font-medium text-green-700">{salary} / yr</p>
      )}

      <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-3">{job.description}</p>

      {job.requiredSkills?.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {job.requiredSkills.map((skill) => (
            <span key={skill} className="text-xs bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 border border-purple-200 dark:border-purple-700 px-2 py-0.5 rounded">
              {skill}
            </span>
          ))}
        </div>
      )}

      {showActions && onLike && onPass && (
        <div className="flex gap-3 mt-2">
          <button
            onClick={onPass}
            className="flex-1 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition font-medium"
          >
            Pass
          </button>
          <button
            onClick={onLike}
            className="flex-1 py-2.5 bg-purple-600 rounded-xl text-white hover:bg-purple-700 transition font-medium"
          >
            Apply 💘
          </button>
        </div>
      )}
    </div>
  )
}
