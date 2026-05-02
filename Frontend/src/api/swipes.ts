import client from './client'
import type { SwipeAction, EmployerSwipeAction, CandidateSummary } from '../types/api'

export const candidateSwipe = (jobId: string, action: SwipeAction) =>
  client.post(`/swipes/jobs/${jobId}`, { action })

export const employerSwipe = (applicationId: string, action: EmployerSwipeAction) =>
  client.post(`/swipes/applicants/${applicationId}`, { action })

export const getJobLikers = (jobId: string) =>
  client.get<CandidateSummary[]>(`/swipes/jobs/${jobId}/likes`)
