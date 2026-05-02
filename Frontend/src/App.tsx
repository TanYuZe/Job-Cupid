import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { ThemeProvider } from './contexts/ThemeContext'
import Navbar from './components/Navbar'

// Eagerly loaded (always needed fast)
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import NotFoundPage from './pages/NotFoundPage'

// Lazy loaded pages
const FeedPage = lazy(() => import('./pages/FeedPage'))
const JobDetailPage = lazy(() => import('./pages/JobDetailPage'))
const MyApplicationsPage = lazy(() => import('./pages/MyApplicationsPage'))
const MatchesPage = lazy(() => import('./pages/MatchesPage'))
const ProfilePage = lazy(() => import('./pages/ProfilePage'))
const MyJobsPage = lazy(() => import('./pages/MyJobsPage'))
const CreateJobPage = lazy(() => import('./pages/CreateJobPage'))
const EditJobPage = lazy(() => import('./pages/EditJobPage'))

function RequireAuth({ children }: { children: React.ReactElement }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return children
}

function RequireRole({ children, role }: { children: React.ReactElement; role: string }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <Navigate to="/" replace />
  return children
}

function GuestOnly({ children }: { children: React.ReactElement }) {
  const { user } = useAuth()
  if (user) return <Navigate to="/" replace />
  return children
}

function HomeRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role === 'ROLE_EMPLOYER') return <Navigate to="/jobs/my" replace />
  if (user.role === 'ROLE_ADMIN') return <Navigate to="/admin/users" replace />
  return <Navigate to="/feed" replace />
}

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
      <Navbar />
      <main className="flex-1">{children}</main>
    </div>
  )
}

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-[60vh]">
    <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
  </div>
)

function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Public */}
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
        <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />

        {/* Candidate */}
        <Route path="/feed" element={
          <RequireRole role="ROLE_USER">
            <Layout><FeedPage /></Layout>
          </RequireRole>
        } />
        <Route path="/jobs/:jobId" element={
          <RequireAuth>
            <Layout><JobDetailPage /></Layout>
          </RequireAuth>
        } />
        <Route path="/applications" element={
          <RequireRole role="ROLE_USER">
            <Layout><MyApplicationsPage /></Layout>
          </RequireRole>
        } />
        <Route path="/matches" element={
          <RequireAuth>
            <Layout><MatchesPage /></Layout>
          </RequireAuth>
        } />
        <Route path="/profile" element={
          <RequireAuth>
            <Layout><ProfilePage /></Layout>
          </RequireAuth>
        } />

        {/* Employer */}
        <Route path="/jobs/my" element={
          <RequireRole role="ROLE_EMPLOYER">
            <Layout><MyJobsPage /></Layout>
          </RequireRole>
        } />
        <Route path="/jobs/new" element={
          <RequireRole role="ROLE_EMPLOYER">
            <Layout><CreateJobPage /></Layout>
          </RequireRole>
        } />
        <Route path="/jobs/:jobId/edit" element={
          <RequireRole role="ROLE_EMPLOYER">
            <Layout><EditJobPage /></Layout>
          </RequireRole>
        } />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
