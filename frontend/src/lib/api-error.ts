import { isAxiosError } from 'axios'

/** Extracts the ApiError.message the backend GlobalExceptionHandler always returns, with a sane fallback. */
export function extractApiErrorMessage(error: unknown, fallback = 'Une erreur est survenue'): string {
  if (isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message
    if (message) {
      return message
    }
  }
  return fallback
}
