import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTheme } from '../contexts/ThemeContext'
import { logout } from '../api/auth'

export default function Navbar() {
  const { user, signOut } = useAuth()
  const { theme, toggle } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()

  const isActive = (path: string) =>
    location.pathname === path || location.pathname.startsWith(path + '/')

  const navLink = (to: string, label: string) => (
    <Link
      to={to}
      className={`text-sm transition ${
        isActive(to)
          ? 'text-purple-600 font-semibold'
          : 'text-gray-600 dark:text-gray-300 hover:text-purple-600 dark:hover:text-purple-400'
      }`}
    >
      {label}
    </Link>
  )

  const handleSignOut = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) await logout(refreshToken)
    } catch {
      // ignore — still sign out locally
    }
    signOut()
    navigate('/login')
  }

  return (
    <nav className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 px-6 py-3 flex items-center justify-between sticky top-0 z-40">
      <Link to="/" className="text-xl font-bold text-purple-600 shrink-0">
        JobCupid 💘
      </Link>

      <div className="flex items-center gap-5 overflow-x-auto">
        {user && (
          <>
            {user.role === 'ROLE_USER' && (
              <>
                {navLink('/feed', 'Job Feed')}
                {navLink('/applications', 'Applications')}
                {navLink('/matches', 'Matches')}
              </>
            )}

            {user.role === 'ROLE_EMPLOYER' && (
              <>
                {navLink('/jobs/my', 'My Jobs')}
                {navLink('/matches', 'Matches')}
                <Link
                  to="/jobs/new"
                  className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700 shrink-0"
                >
                  Post a Job
                </Link>
              </>
            )}

            {user.role === 'ROLE_ADMIN' && (
              <>
                {navLink('/admin/users', 'Users')}
                {navLink('/admin/jobs', 'Jobs')}
              </>
            )}

            {navLink('/profile', 'Profile')}

            <button
              onClick={handleSignOut}
              className="text-sm text-gray-500 dark:text-gray-400 hover:text-red-500 shrink-0 transition"
            >
              Sign out
            </button>
          </>
        )}

        {!user && (
          <>
            <Link to="/login" className="text-sm text-gray-600 dark:text-gray-300 hover:text-purple-600">
              Login
            </Link>
            <Link
              to="/register"
              className="text-sm bg-purple-600 text-white px-3 py-1.5 rounded-lg hover:bg-purple-700"
            >
              Register
            </Link>
          </>
        )}

        {/* Dark mode toggle */}
        <button
          onClick={toggle}
          aria-label="Toggle dark mode"
          className="shrink-0 w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
        >
          {theme === 'dark' ? (
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clipRule="evenodd" />
            </svg>
          ) : (
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" />
            </svg>
          )}
        </button>
      </div>
    </nav>
  )
}
