import client from './client'
import type {
  JobResponse,
  JobFeedResponse,
  JobFeedParams,
  CreateJobRequest,
  PageResponse,
} from '../types/api'

export const getJobFeed = (params: JobFeedParams) =>
  client.get<JobFeedResponse>('/jobs', { params })

export const getJob = (jobId: string) =>
  client.get<JobResponse>(`/jobs/${jobId}`)

export const getMyJobs = (page = 0, size = 20) =>
  client.get<PageResponse<JobResponse>>('/jobs/my', { params: { page, size } })

export const createJob = (data: CreateJobRequest) =>
  client.post<JobResponse>('/jobs', data)

export const updateJob = (jobId: string, data: Partial<CreateJobRequest>) =>
  client.put<JobResponse>(`/jobs/${jobId}`, data)

export const closeJob = (jobId: string) =>
  client.delete(`/jobs/${jobId}`)

// Re-export types for backward compat with existing pages
export type { JobResponse, JobFeedResponse, CreateJobRequest, JobFeedParams }
