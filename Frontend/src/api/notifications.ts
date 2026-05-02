import client from './client'
import type { NotificationPageResponse } from '../types/api'

export const getNotifications = (page = 0, size = 20) =>
  client.get<NotificationPageResponse>('/notifications', { params: { page, size } })

export const markRead = (notificationId: string) =>
  client.put(`/notifications/${notificationId}/read`)

export const markAllRead = () =>
  client.put('/notifications/read-all')
