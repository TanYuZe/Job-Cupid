import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getMyJobs, type JobResponse } from '../api/jobs'
import JobCard from '../components/JobCard'

interface PageData {
  content: JobResponse[]
  totalPages: number
  number: number
}

export default function MyJobsPage() {
  const [data, setData] = useState<PageData | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getMyJobs(page)
      .then(({ data }) => setData(data))
      .catch(() => setError('Failed to load jobs. Is the backend running?'))
      .finally(() => setLoading(false))
  }, [page])

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">My Job Postings</h1>
          <Link
            to="/jobs/new"
            className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700"
          >
            + Post a Job
          </Link>
        </div>

        {loading && <p className="text-center text-gray-400 dark:text-gray-500 py-20">Loading…</p>}

        {error && (
          <div className="text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 text-sm">
            {error}
          </div>
        )}

        {!loading && data?.content.length === 0 && (
          <div className="text-center py-20">
            <p className="text-3xl mb-2">📋</p>
            <p className="text-gray-500">No job postings yet.</p>
            <Link
              to="/jobs/new"
              className="mt-4 inline-block bg-purple-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-purple-700"
            >
              Post your first job
            </Link>
          </div>
        )}

        <div className="flex flex-col gap-4">
          {data?.content.map((job) => (
            <div key={job.id} className="relative">
              <JobCard job={job} />
              <div className="mt-2 flex gap-2">
                <Link
                  to={`/jobs/${job.id}/edit`}
                  className="text-xs px-3 py-1.5 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:border-purple-400"
                >
                  Edit
                </Link>
                <span className={`text-xs px-3 py-1.5 rounded-lg font-medium ${
                  job.status === 'ACTIVE'
                    ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
                }`}>
                  {job.status}
                </span>
                <span className="text-xs px-3 py-1.5 text-gray-400 dark:text-gray-500">
                  {job.applicationCount} applicants
                </span>
              </div>
            </div>
          ))}
        </div>

        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-3 mt-8">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:border-purple-400"
            >
              Prev
            </button>
            <span className="text-sm text-gray-500 dark:text-gray-400 self-center">
              Page {data.number + 1} of {data.totalPages}
            </span>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:border-purple-400"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
