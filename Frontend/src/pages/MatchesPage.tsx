import { useState, useEffect } from 'react'
import { getMatches } from '../api/matches'
import type { MatchResponse } from '../types/api'
import { useAuth } from '../contexts/AuthContext'
import MatchCard from '../components/matches/MatchCard'
import Spinner from '../components/ui/Spinner'
import EmptyState from '../components/ui/EmptyState'
import Button from '../components/ui/Button'
import ErrorBanner from '../components/ui/ErrorBanner'
import { Link } from 'react-router-dom'

export default function MatchesPage() {
  const { user } = useAuth()
  const isCandidate = user?.role === 'ROLE_USER'

  const [matches, setMatches] = useState<MatchResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getMatches(page)
      .then(({ data }) => {
        setMatches((prev) => (page === 0 ? data.content : [...prev, ...data.content]))
        setTotalPages(data.totalPages)
      })
      .catch(() => setError('Failed to load matches.'))
      .finally(() => setLoading(false))
  }, [page])

  const hasMore = page + 1 < totalPages

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">Matches 💘</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
          {isCandidate
            ? 'Jobs where you and the employer both expressed interest.'
            : 'Candidates you liked who also applied to your jobs.'}
        </p>

        {error && <ErrorBanner message={error} className="mb-6" />}

        {loading && page === 0 && <Spinner className="py-20" />}

        {!loading && matches.length === 0 && (
          <EmptyState
            icon="💔"
            heading="No matches yet"
            subtext={
              isCandidate
                ? 'Keep swiping and applying — your match is out there!'
                : 'Like some candidates to create your first match.'
            }
            action={
              isCandidate ? (
                <Link
                  to="/feed"
                  className="inline-block bg-purple-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-purple-700"
                >
                  Browse jobs
                </Link>
              ) : undefined
            }
          />
        )}

        {matches.length > 0 && (
          <div className="flex flex-col gap-3">
            {matches.map((match) => (
              <MatchCard key={match.id} match={match} />
            ))}
          </div>
        )}

        {hasMore && !loading && (
          <div className="flex justify-center mt-6">
            <Button variant="secondary" onClick={() => setPage((p) => p + 1)}>
              Load more
            </Button>
          </div>
        )}

        {loading && page > 0 && (
          <div className="flex justify-center mt-6"><Spinner size="sm" /></div>
        )}
      </div>
    </div>
  )
}
