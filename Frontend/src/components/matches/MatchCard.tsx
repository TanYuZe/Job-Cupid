import { Link } from 'react-router-dom'
import type { MatchResponse } from '../../types/api'
import { useAuth } from '../../contexts/AuthContext'

interface Props {
  match: MatchResponse
}

export default function MatchCard({ match }: Props) {
  const { user } = useAuth()
  const isCandidate = user?.role === 'ROLE_USER'

  const matchedDate = new Date(match.matchedAt).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  })

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:border-purple-300 dark:hover:border-purple-600 transition">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          {isCandidate ? (
            <>
              {match.job ? (
                <Link
                  to={`/jobs/${match.jobId}`}
                  className="font-semibold text-gray-900 dark:text-gray-100 hover:text-purple-600 transition block truncate"
                >
                  {match.job.title}
                </Link>
              ) : (
                <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">Job #{match.jobId.slice(0, 8)}</p>
              )}
              {match.job && <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{match.job.category}</p>}
            </>
          ) : (
            <>
              <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">
                {match.candidate?.name ?? `Candidate #${match.candidateId.slice(0, 8)}`}
              </p>
              {match.candidate?.headline && (
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{match.candidate.headline}</p>
              )}
            </>
          )}
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1.5">Matched on {matchedDate}</p>
        </div>
        <span className="shrink-0 text-xl">💘</span>
      </div>

      {/* Candidate skills preview (employer view) */}
      {!isCandidate && match.candidate?.skills?.length > 0 && (
        <div className="flex flex-wrap gap-1 mt-3">
          {match.candidate.skills.slice(0, 4).map((s) => (
            <span key={s} className="text-xs bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 border border-purple-200 dark:border-purple-700 px-2 py-0.5 rounded">
              {s}
            </span>
          ))}
          {match.candidate.skills.length > 4 && (
            <span className="text-xs text-gray-400 dark:text-gray-500">+{match.candidate.skills.length - 4} more</span>
          )}
        </div>
      )}
    </div>
  )
}
