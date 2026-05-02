import client from './client'
import type {
  UserProfileResponse,
  UpdateCandidateProfileRequest,
  UpdateEmployerProfileRequest,
  PhotoUploadResponse,
} from '../types/api'

export const getMe = () => client.get<UserProfileResponse>('/users/me')

export const updateProfile = (
  data: UpdateCandidateProfileRequest | UpdateEmployerProfileRequest
) => client.put<UserProfileResponse>('/users/me', data)

export const requestPhotoUpload = (contentType: 'image/jpeg' | 'image/png') =>
  client.post<PhotoUploadResponse>('/users/me/photo', { contentType })

export const confirmPhotoUpload = (publicUrl: string) =>
  client.put<UserProfileResponse>('/users/me/photo/confirm', { publicUrl })

export const deleteAccount = () => client.delete('/users/me')
