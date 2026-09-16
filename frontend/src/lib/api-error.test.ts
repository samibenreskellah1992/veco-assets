import { describe, expect, it } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { extractApiErrorMessage, extractBlobApiErrorMessage } from './api-error'

/**
 * Tests unitaires (Phase 10 - tache #88, premier signal de test frontend
 * reellement executable dans ce sandbox) de l'extraction du message
 * ApiError.message renvoye par GlobalExceptionHandler cote backend (voir
 * ApiExceptionHandler / docs/ARCHITECTURE.md). Les deux fonctions doivent
 * degrader proprement vers le message par defaut plutot que planter, quel
 * que soit ce que renvoie axios.
 */

function axiosErrorWithData(data: unknown, status = 400): AxiosError {
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    data,
    status,
    statusText: 'Bad Request',
    headers: {},
    config: { headers: new AxiosHeaders() },
  })
}

describe('extractApiErrorMessage', () => {
  it('returns the backend ApiError.message when present', () => {
    const error = axiosErrorWithData({ message: 'Code deja utilise', error: 'BUSINESS_RULE_VIOLATION' })
    expect(extractApiErrorMessage(error)).toBe('Code deja utilise')
  })

  it('falls back to the default message when the backend sent no message field', () => {
    const error = axiosErrorWithData({ error: 'INTERNAL_ERROR' })
    expect(extractApiErrorMessage(error)).toBe('Une erreur est survenue')
  })

  it('honors a custom fallback', () => {
    const error = axiosErrorWithData({})
    expect(extractApiErrorMessage(error, 'Impossible de charger ce rapport.')).toBe('Impossible de charger ce rapport.')
  })

  it('falls back for a non-axios error (e.g. a thrown string or network failure shape)', () => {
    expect(extractApiErrorMessage(new Error('network down'))).toBe('Une erreur est survenue')
    expect(extractApiErrorMessage('not even an Error object')).toBe('Une erreur est survenue')
  })
})

describe('extractBlobApiErrorMessage', () => {
  it('reads the message out of a Blob error body (responseType: blob exports)', async () => {
    const blob = new Blob([JSON.stringify({ message: 'Format non supporte' })], { type: 'application/json' })
    const error = axiosErrorWithData(blob)
    await expect(extractBlobApiErrorMessage(error)).resolves.toBe('Format non supporte')
  })

  it('falls back to default when the blob body is not JSON (e.g. a generic 500 HTML page)', async () => {
    const blob = new Blob(['<html>500</html>'], { type: 'text/html' })
    const error = axiosErrorWithData(blob)
    await expect(extractBlobApiErrorMessage(error)).resolves.toBe('Une erreur est survenue')
  })

  it('falls back to default when the blob body is JSON but has no message field', async () => {
    const blob = new Blob([JSON.stringify({ error: 'INTERNAL_ERROR' })], { type: 'application/json' })
    const error = axiosErrorWithData(blob)
    await expect(extractBlobApiErrorMessage(error)).resolves.toBe('Une erreur est survenue')
  })

  it('delegates to extractApiErrorMessage when the error data is not a Blob (JSON error response)', async () => {
    const error = axiosErrorWithData({ message: 'Acces refuse' })
    await expect(extractBlobApiErrorMessage(error)).resolves.toBe('Acces refuse')
  })
})
