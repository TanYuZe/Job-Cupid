import { createContext, useContext, useState, type ReactNode } from 'react'
import type { AuthResponse } from '../types/api'

export interface AuthUser {
  userId: string
  email: string
  role: string
  isPremium: boolean
  token: string
  refreshToken: string
}

interface AuthContextType {
  user: AuthUser | null
  signIn: (data: AuthResponse) => void
  signOut: () => void
  updatePremium: (isPremium: boolean) => void
}

const AuthContext = createContext<AuthContextType | null>(null)

function normalizeRole(role: string) {
  return role.startsWith('ROLE_') ? role : `ROLE_${role}`
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    try {
      const parsed: AuthUser = JSON.parse(raw)
      parsed.role = normalizeRole(parsed.role)
      return parsed
    } catch {
      return null
    }
  })

  const signIn = (data: AuthResponse) => {
    const u: AuthUser = {
      userId: data.userId,
      email: data.email,
      role: normalizeRole(data.role),
      isPremium: data.isPremium,
      token: data.accessToken,
      refreshToken: data.refreshToken,
    }
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(u))
    setUser(u)
  }

  const signOut = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
  }

  const updatePremium = (isPremium: boolean) => {
    setUser((prev) => {
      if (!prev) return prev
      const updated = { ...prev, isPremium }
      localStorage.setItem('user', JSON.stringify(updated))
      return updated
    })
  }

  return (
    <AuthContext.Provider value={{ user, signIn, signOut, updatePremium }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be inside AuthProvider')
  return ctx
}
