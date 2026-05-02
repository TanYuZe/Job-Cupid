import { useState, useEffect, useCallback } from 'react'
import { getJobFeed } from '../api/jobs'
import { candidateSwipe } from '../api/swipes'
import type { JobResponse, JobFeedParams } from '../types/api'
import SwipeCard from '../components/jobs/SwipeCard'
import FeedFilters from '../components/jobs/FeedFilters'
import SwipeLimitBanner from '../components/jobs/SwipeLimitBanner'
import Spinner from '../components/ui/Spinner'
import EmptyState from '../components/ui/EmptyState'
import Button from '../components/ui/Button'

export default function FeedPage() {
  const [jobs, setJobs] = useState<JobResponse[]>([])
  const [cursor, setCursor] = useState<string | undefined>()
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [swiping, setSwiping] = useState(false)
  const [swipeLimitReached, setSwipeLimitReached] = useState(false)
  const [filters, setFilters] = useState<Omit<JobFeedParams, 'cursor' | 'size'>>({})
  const [currentIndex, setCurrentIndex] = useState(0)

  const loadFeed = useCallback(
    async (reset = false, activeFilters = filters) => {
      if (loading) return
      setLoading(true)
      try {
        const { data } = await getJobFeed({
          ...activeFilters,
          cursor: reset ? undefined : cursor,
          size: 10,
        })
        setJobs((prev) => (reset ? data.items : [...prev, ...data.items]))
        setCursor(data.nextCursor ?? undefined)
        setHasMore(data.hasMore)
        if (reset) setCurrentIndex(0)
      } catch {
        // backend may not be running; keep existing state
      } finally {
        setLoading(false)
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [filters, cursor]
  )

  useEffect(() => {
    loadFeed(true, filters)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters])

  const handleApplyFilters = (newFilters: Omit<JobFeedParams, 'cursor' | 'size'>) => {
    setFilters(newFilters)
    setSwipeLimitReached(false)
  }

  const advance = () => {
    const next = currentIndex + 1
    if (next >= jobs.length && hasMore) {
      loadFeed(false)
    }
    setCurrentIndex(next)
  }

  const handleSwipe = async (action: 'LIKE' | 'PASS') => {
    const job = jobs[currentIndex]
    if (!job || swiping) return
    setSwiping(true)
    try {
      await candidateSwipe(job.id, action)
      advance()
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 429) {
        setSwipeLimitReached(true)
      } else {
        // For non-limit errors, still advance so the user isn't stuck
        advance()
      }
    } finally {
      setSwiping(false)
    }
  }

  const currentJob = jobs[currentIndex]

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-lg mx-auto px-4 py-8">
        <FeedFilters onApply={handleApplyFilters} />

        {swipeLimitReached && <SwipeLimitBanner />}

        {!swipeLimitReached && loading && !currentJob && (
          <Spinner className="py-20" />
        )}

        {!swipeLimitReached && !loading && !currentJob && (
          <EmptyState
            icon="🎉"
            heading="You've seen all jobs!"
            subtext="Check back later or adjust your filters."
            action={
              <Button onClick={() => loadFeed(true)} variant="secondary">
                Reload feed
              </Button>
            }
          />
        )}

        {!swipeLimitReached && currentJob && (
          <>
            <p className="text-xs text-center text-gray-400 dark:text-gray-500 mb-3">
              {currentIndex + 1} of {jobs.length}{hasMore ? '+' : ''}
            </p>
            <SwipeCard
              key={currentJob.id}
              job={currentJob}
              onAction={handleSwipe}
              disabled={swiping}
            />
          </>
        )}
      </div>
    </div>
  )
}
