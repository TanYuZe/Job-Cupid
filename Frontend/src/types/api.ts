// ─── Auth ────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  userId: string
  email: string
  role: string
  isPremium: boolean
}

// ─── User / Profile ──────────────────────────────────────────────────────────

export interface CandidateProfileResponse {
  userId: string
  email: string
  role: string
  isPremium: boolean
  photoUrl: string | null
  headline: string | null
  resumeUrl: string | null
  skills: string[]
  yearsOfExperience: number | null
  desiredSalaryMin: number | null
  desiredSalaryMax: number | null
  preferredRemote: boolean
  preferredLocation: string | null
  isOpenToWork: boolean
}

export interface EmployerProfileResponse {
  userId: string
  email: string
  role: string
  isPremium: boolean
  photoUrl: string | null
  companyName: string | null
  companyDescription: string | null
  companyWebsite: string | null
  companyLogoUrl: string | null
  companySize: string | null
  industry: string | null
  foundedYear: number | null
}

export type UserProfileResponse = CandidateProfileResponse | EmployerProfileResponse

export interface UpdateCandidateProfileRequest {
  headline?: string
  skills?: string[]
  yearsOfExperience?: number
  desiredSalaryMin?: number
  desiredSalaryMax?: number
  preferredRemote?: boolean
  preferredLocation?: string
  isOpenToWork?: boolean
}

export interface UpdateEmployerProfileRequest {
  companyName?: string
  companyDescription?: string
  companyWebsite?: string
  companySize?: string
  industry?: string
  foundedYear?: number
}

export interface PhotoUploadResponse {
  presignedUrl: string
  publicUrl: string
  expiresIn: number
}

// ─── Jobs ────────────────────────────────────────────────────────────────────

export type JobStatus = 'ACTIVE' | 'PAUSED' | 'CLOSED' | 'ARCHIVED'
export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'FREELANCE'
export type ExperienceLevel = 'ENTRY' | 'MID' | 'SENIOR' | 'LEAD' | 'EXECUTIVE'

export interface JobResponse {
  id: string
  employerId: string
  title: string
  description: string
  category: string
  location: string | null
  isRemote: boolean
  salaryMin: number | null
  salaryMax: number | null
  currency: string
  employmentType: EmploymentType
  experienceLevel: ExperienceLevel
  requiredSkills: string[]
  status: JobStatus
  boostScore: number
  applicationCount: number
  expiresAt: string | null
  createdAt: string
}

export interface JobFeedResponse {
  items: JobResponse[]
  nextCursor: string | null
  hasMore: boolean
}

export interface JobFeedParams {
  category?: string
  location?: string
  remote?: boolean
  salaryMin?: number
  salaryMax?: number
  keyword?: string
  cursor?: string
  size?: number
}

export interface CreateJobRequest {
  title: string
  description: string
  category: string
  location?: string
  isRemote?: boolean
  salaryMin?: number
  salaryMax?: number
  currency?: string
  employmentType: string
  experienceLevel: string
  requiredSkills?: string[]
  expiresAt?: string
}

// ─── Swipes ──────────────────────────────────────────────────────────────────

export type SwipeAction = 'LIKE' | 'PASS'
export type EmployerSwipeAction = 'LIKE' | 'REJECT'

export interface CandidateSwipeRequest {
  action: SwipeAction
}

export interface EmployerSwipeRequest {
  action: EmployerSwipeAction
}

export interface CandidateSummary {
  userId: string
  name: string
  headline: string | null
  photoUrl: string | null
  skills: string[]
}

// ─── Applications ────────────────────────────────────────────────────────────

export type ApplicationStatus =
  | 'PENDING'
  | 'REVIEWED'
  | 'SHORTLISTED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN'

export interface ApplicationResponse {
  id: string
  candidateId: string
  jobId: string
  job?: JobResponse
  coverLetter: string | null
  resumeUrl: string | null
  status: ApplicationStatus
  appliedAt: string
  reviewedAt: string | null
}

export interface ApplyRequest {
  coverLetter?: string
  resumeUrl?: string
}

export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}

// ─── Matches ─────────────────────────────────────────────────────────────────

export type MatchStatus = 'ACTIVE' | 'ARCHIVED' | 'EXPIRED'

export interface MatchResponse {
  id: string
  candidateId: string
  employerId: string
  jobId: string
  applicationId: string
  status: MatchStatus
  matchedAt: string
  job?: JobResponse
  candidate?: CandidateSummary
}

// ─── Notifications ───────────────────────────────────────────────────────────

export type NotificationType =
  | 'MATCH_CREATED'
  | 'APPLICATION_RECEIVED'
  | 'APPLICATION_STATUS_CHANGED'
  | 'SWIPE_RECEIVED'
  | 'SUBSCRIPTION_RENEWED'
  | 'SUBSCRIPTION_EXPIRING'

export interface NotificationResponse {
  id: string
  userId: string
  type: NotificationType
  title: string
  body: string
  referenceId: string | null
  referenceType: string | null
  isRead: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationPageResponse {
  content: NotificationResponse[]
  totalPages: number
  totalElements: number
  number: number
  unreadCount: number
}

// ─── Subscriptions ───────────────────────────────────────────────────────────

export type SubscriptionPlan = 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'
export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PAST_DUE'

export interface SubscriptionResponse {
  id: string
  userId: string
  plan: SubscriptionPlan
  status: SubscriptionStatus
  currentPeriodStart: string
  currentPeriodEnd: string
  cancelledAt: string | null
}

export interface PlanInfo {
  plan: SubscriptionPlan
  name: string
  price: number
  currency: string
  interval: string
  features: string[]
}

// ─── Admin ───────────────────────────────────────────────────────────────────

export interface AdminUserResponse {
  userId: string
  email: string
  role: string
  isPremium: boolean
  isBanned: boolean
  isActive: boolean
  createdAt: string
  lastLoginAt: string | null
}

// ─── Error ───────────────────────────────────────────────────────────────────

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  traceId: string
}
