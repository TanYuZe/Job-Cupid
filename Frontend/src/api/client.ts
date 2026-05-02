import axios from 'axios'

const client = axios.create({ baseURL: '/api/v1' })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Silent token refresh on 401
let isRefreshing = false
let queue: Array<{ resolve: (t: string) => void; reject: (e: unknown) => void }> = []

function flushQueue(token: string) {
  queue.forEach(({ resolve }) => resolve(token))
  queue = []
}

function rejectQueue(err: unknown) {
  queue.forEach(({ reject }) => reject(err))
  queue = []
}

function redirectToLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

client.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config as typeof err.config & { _retry?: boolean }
    if (err.response?.status !== 401 || original._retry) {
      return Promise.reject(err)
    }

    // Never intercept 401s from auth endpoints — let them surface as normal errors
    if (original.url?.includes('/auth/')) {
      return Promise.reject(err)
    }

    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      redirectToLogin()
      return Promise.reject(err)
    }

    original._retry = true

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        queue.push({ resolve, reject })
      }).then((newToken) => {
        original.headers.Authorization = `Bearer ${newToken}`
        return client(original)
      })
    }

    isRefreshing = true
    try {
      // Use raw axios so this call doesn't go through the interceptor again
      const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken })
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      // Patch the stored user record with the new token
      const raw = localStorage.getItem('user')
      if (raw) {
        const u = JSON.parse(raw)
        u.token = data.accessToken
        localStorage.setItem('user', JSON.stringify(u))
      }
      flushQueue(data.accessToken)
      original.headers.Authorization = `Bearer ${data.accessToken}`
      return client(original)
    } catch (refreshErr) {
      rejectQueue(refreshErr)
      redirectToLogin()
      return Promise.reject(refreshErr)
    } finally {
      isRefreshing = false
    }
  }
)

export default client
