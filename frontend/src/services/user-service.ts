import { apiClient } from '@/services/api-client'
import type { RoleDto, UserCreateRequest, UserDto, UserUpdateRequest } from '@/types/user'

export const usersApi = {
  list: async () => (await apiClient.get<UserDto[]>('/users')).data,
  create: async (request: UserCreateRequest) => (await apiClient.post<UserDto>('/users', request)).data,
  update: async (id: string, request: UserUpdateRequest) => (await apiClient.put<UserDto>(`/users/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<UserDto>(`/users/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<UserDto>(`/users/${id}/deactivate`)).data,
  resetPassword: async (id: string, newPassword: string) =>
    apiClient.post(`/users/${id}/reset-password`, { newPassword }),
}

export const rolesApi = {
  list: async () => (await apiClient.get<RoleDto[]>('/roles')).data,
}
