import client from './client'
import type { AuthResponse } from '../types/api'

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  role: 'USER' | 'EMPLOYER'
}

export interface LoginRequest {
  email: string
  password: string
}

export const register = (data: RegisterRequest) =>
  client.post<AuthResponse>('/auth/register', data)

export const login = (data: LoginRequest) =>
  client.post<AuthResponse>('/auth/login', data)

export const refresh = (refreshToken: string) =>
  client.post<AuthResponse>('/auth/refresh', { refreshToken })

export const logout = (refreshToken: string) =>
  client.post('/auth/logout', { refreshToken })
