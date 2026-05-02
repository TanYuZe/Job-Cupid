import { useState, useEffect } from 'react'
import { getMyApplications } from '../api/applications'
import type { ApplicationResponse } from '../types/api'
import ApplicationCard from '../components/applications/ApplicationCard'
import Spinner from '../components/ui/Spinner'
import EmptyState from '../components/ui/EmptyState'
import Button from '../components/ui/Button'
import ErrorBanner from '../components/ui/ErrorBanner'
import { Link } from 'react-router-dom'

export default function MyApplicationsPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getMyApplications(page)
      .then(({ data }) => {
        setApplications((prev) => (page === 0 ? data.content : [...prev, ...data.content]))
        setTotalPages(data.totalPages)
      })
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false))
  }, [page])

  const hasMore = page + 1 < totalPages

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">My Applications</h1>

        {error && <ErrorBanner message={error} className="mb-6" />}

        {loading && page === 0 && <Spinner className="py-20" />}

        {!loading && applications.length === 0 && (
          <EmptyState
            icon="📝"
            heading="No applications yet"
            subtext="Like a job from the feed and apply to get started."
            action={
              <Link
                to="/feed"
                className="inline-block bg-purple-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-purple-700"
              >
                Browse jobs
              </Link>
            }
          />
        )}

        {applications.length > 0 && (
          <div className="flex flex-col gap-3">
            {applications.map((app) => (
              <ApplicationCard key={app.id} application={app} />
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
