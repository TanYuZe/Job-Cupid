import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function NotFoundPage() {
  const { user } = useAuth()
  const home = !user ? '/' : user.role === 'ROLE_EMPLOYER' ? '/jobs/my' : '/feed'

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center px-4">
      <div className="text-center">
        <p className="text-6xl font-bold text-purple-600 mb-4">404</p>
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-gray-100 mb-2">Page not found</h1>
        <p className="text-gray-500 dark:text-gray-400 mb-8">The page you're looking for doesn't exist.</p>
        <Link
          to={home}
          className="inline-block bg-purple-600 text-white px-6 py-2.5 rounded-lg font-medium hover:bg-purple-700 transition"
        >
          Go home
        </Link>
      </div>
    </div>
  )
}
