import { useState, useEffect } from 'react'
import { getMe } from '../api/users'
import { useAuth } from '../contexts/AuthContext'
import type { CandidateProfileResponse, EmployerProfileResponse } from '../types/api'
import CandidateProfileForm from '../components/profile/CandidateProfileForm'
import EmployerProfileForm from '../components/profile/EmployerProfileForm'
import Spinner from '../components/ui/Spinner'
import ErrorBanner from '../components/ui/ErrorBanner'

export default function ProfilePage() {
  const { user } = useAuth()
  const [profile, setProfile] = useState<CandidateProfileResponse | EmployerProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getMe()
      .then(({ data }) => setProfile(data))
      .catch(() => setError('Failed to load profile.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="py-20"><Spinner className="py-20" /></div>

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">My Profile</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{user?.email}</p>
        </div>

        {error && <ErrorBanner message={error} className="mb-6" />}

        {profile && (
          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-sm p-6">
            {user?.role === 'ROLE_USER' ? (
              <CandidateProfileForm
                profile={profile as CandidateProfileResponse}
                onSaved={(updated) => setProfile(updated)}
              />
            ) : (
              <EmployerProfileForm
                profile={profile as EmployerProfileResponse}
                onSaved={(updated) => setProfile(updated)}
              />
            )}
          </div>
        )}
      </div>
    </div>
  )
}
