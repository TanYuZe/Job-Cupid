import { Link } from 'react-router-dom'

export default function SwipeLimitBanner() {
  return (
    <div className="bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200 rounded-2xl p-8 text-center">
      <p className="text-4xl mb-3">💎</p>
      <h2 className="text-lg font-semibold text-gray-900 mb-2">Daily swipe limit reached</h2>
      <p className="text-sm text-gray-600 mb-6">
        Free accounts get 20 swipes per day. Upgrade to Premium for unlimited swipes and more.
      </p>
      <Link
        to="/upgrade"
        className="inline-block bg-purple-600 text-white px-6 py-2.5 rounded-lg font-medium hover:bg-purple-700 transition"
      >
        Upgrade to Premium
      </Link>
      <p className="mt-4 text-xs text-gray-400">Your limit resets at midnight</p>
    </div>
  )
}
