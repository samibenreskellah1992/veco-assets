export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

/** Mirrors backend UserDto (Administration > Utilisateurs, Phase 4) - distinct from types/auth.ts UserSummary (profil "qui suis-je"). */
export interface UserDto {
  id: string
  matricule: string | null
  firstName: string
  lastName: string
  email: string
  phone: string | null
  siteId: string | null
  siteName: string | null
  department: string | null
  service: string | null
  status: UserStatus
  lastLoginAt: string | null
  roleCodes: string[]
}

export interface UserCreateRequest {
  matricule?: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  siteId?: string
  department?: string
  service?: string
  password: string
  roleCodes: string[]
}

export interface UserUpdateRequest {
  matricule?: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  siteId?: string
  department?: string
  service?: string
  status: UserStatus
  roleCodes: string[]
}

export interface RoleDto {
  id: string
  code: string
  label: string
  description: string | null
}
