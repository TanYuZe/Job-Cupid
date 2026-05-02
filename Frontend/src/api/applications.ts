import client from './client'
import type { ApplicationResponse, ApplyRequest, ApplicationStatus, PageResponse } from '../types/api'

export const apply = (jobId: string, data: ApplyRequest = {}) =>
  client.post<ApplicationResponse>(`/applications/jobs/${jobId}`, data)

export const getMyApplications = (page = 0, size = 20) =>
  client.get<PageResponse<ApplicationResponse>>('/applications/my', { params: { page, size } })

export const getJobApplications = (jobId: string, page = 0, size = 20) =>
  client.get<PageResponse<ApplicationResponse>>(`/applications/jobs/${jobId}`, {
    params: { page, size },
  })

export const updateApplicationStatus = (applicationId: string, status: ApplicationStatus) =>
  client.put<ApplicationResponse>(`/applications/${applicationId}/status`, { status })
