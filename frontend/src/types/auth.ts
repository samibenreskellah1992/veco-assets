/** Mirrors backend UserSummaryDto (dz.vecopharm.vecoassets.dto) - never a password field. */
export interface UserSummary {
  id: string
  matricule: string | null
  fullName: string
  email: string
  siteName: string | null
  department: string | null
  service: string | null
  roles: string[]
  permissions: string[]
}

/** Mirrors backend LoginResponse. */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserSummary
}
