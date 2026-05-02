import { Link } from 'react-router-dom'
import type { ApplicationResponse, ApplicationStatus } from '../../types/api'
import Badge from '../ui/Badge'

interface Props {
  application: ApplicationResponse
}

const STATUS_COLOR: Record<ApplicationStatus, 'gray' | 'blue' | 'purple' | 'green' | 'red'> = {
  PENDING: 'gray',
  REVIEWED: 'blue',
  SHORTLISTED: 'purple',
  ACCEPTED: 'green',
  REJECTED: 'red',
  WITHDRAWN: 'gray',
}

const STATUS_LABEL: Record<ApplicationStatus, string> = {
  PENDING: 'Pending review',
  REVIEWED: 'Reviewed',
  SHORTLISTED: 'Shortlisted',
  ACCEPTED: 'Accepted',
  REJECTED: 'Not selected',
  WITHDRAWN: 'Withdrawn',
}

export default function ApplicationCard({ application }: Props) {
  const job = application.job
  const appliedDate = new Date(application.appliedAt).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  })

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 flex items-start justify-between gap-4 hover:border-purple-300 dark:hover:border-purple-600 transition">
      <div className="flex-1 min-w-0">
        {job ? (
          <Link
            to={`/jobs/${application.jobId}`}
            className="font-semibold text-gray-900 dark:text-gray-100 hover:text-purple-600 transition truncate block"
          >
            {job.title}
          </Link>
        ) : (
          <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">Job #{application.jobId.slice(0, 8)}</p>
        )}
        {job && <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{job.category}</p>}
        <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">Applied {appliedDate}</p>
      </div>
      <Badge color={STATUS_COLOR[application.status]}>
        {STATUS_LABEL[application.status]}
      </Badge>
    </div>
  )
}
