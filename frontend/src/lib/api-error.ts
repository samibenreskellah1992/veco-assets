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

/**
 * Same as {@link extractApiErrorMessage}, for requests made with
 * `responseType: 'blob'` (e.g. `/api/labels/generate`, which normally
 * returns a PDF) - on an error response axios still hands back a Blob
 * rather than parsed JSON, so the ApiError.message has to be read out of
 * it asynchronously instead of off `error.response.data` directly.
 */
export async function extractBlobApiErrorMessage(error: unknown, fallback = 'Une erreur est survenue'): Promise<string> {
  if (isAxiosError(error) && error.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      const parsed = JSON.parse(text) as { message?: string }
      if (parsed.message) {
        return parsed.message
      }
    } catch {
      // Corps non-JSON (ex. erreur 500 generique) - on retombe sur le message par defaut ci-dessous.
    }
    return fallback
  }
  return extractApiErrorMessage(error, fallback)
}
