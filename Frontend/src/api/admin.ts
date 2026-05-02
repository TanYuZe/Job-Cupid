import client from './client'
import type { AdminUserResponse, PageResponse } from '../types/api'

export const listUsers = (page = 0, size = 20, search?: string) =>
  client.get<PageResponse<AdminUserResponse>>('/admin/users', {
    params: { page, size, search },
  })

export const banUser = (userId: string, reason: string) =>
  client.put(`/admin/users/${userId}/ban`, { reason })

export const changeRole = (userId: string, role: string) =>
  client.put(`/admin/users/${userId}/role`, { role })

export const forceCloseJob = (jobId: string) =>
  client.delete(`/admin/jobs/${jobId}`)
