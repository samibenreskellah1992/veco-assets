import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './use-auth'
import { UNAUTHORIZED_EVENT } from '@/services/api-client'

// auth-service fait un vrai appel axios (/auth/login, /auth/me) - on le
// mocke entierement, comme token-storage (localStorage) pour ne pas
// dependre d'un backend ni de l'etat du jsdom entre tests.
vi.mock('@/services/auth-service')
vi.mock('@/lib/token-storage')

import { fetchCurrentUser, loginRequest } from '@/services/auth-service'
import { clearStoredToken, getStoredToken, setStoredToken } from '@/lib/token-storage'

const mockedFetchCurrentUser = vi.mocked(fetchCurrentUser)
const mockedLoginRequest = vi.mocked(loginRequest)
const mockedGetStoredToken = vi.mocked(getStoredToken)
const mockedClearStoredToken = vi.mocked(clearStoredToken)
const mockedSetStoredToken = vi.mocked(setStoredToken)

const USER_WITH_REPORT_VIEW = {
  id: 'u1',
  matricule: 'M-001',
  fullName: 'Amine Test',
  email: 'amine@vecopharm.dz',
  siteName: 'Siege',
  department: 'IT',
  service: 'Support',
  roles: ['GESTIONNAIRE_PATRIMOINE'],
  permissions: ['REPORT_VIEW', 'IMMOBILISATION_VIEW'],
}

/**
 * Composant sonde qui affiche l'etat du contexte pour que les tests
 * puissent l'inspecter via le DOM, plutot que d'appeler useAuth() hors
 * d'un composant (invalide - regle des Hooks React).
 */
function Probe() {
  const { user, isLoading, hasPermission, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="user">{user?.fullName ?? 'none'}</span>
      <span data-testid="can-view-report">{String(hasPermission('REPORT_VIEW'))}</span>
      <span data-testid="can-export-report">{String(hasPermission('REPORT_EXPORT'))}</span>
      <button onClick={() => login('amine@vecopharm.dz', 'secret')}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  )
}

describe('AuthProvider / useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('starts with no stored token: isLoading settles to false and there is no user', async () => {
    mockedGetStoredToken.mockReturnValue(null)

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(mockedFetchCurrentUser).not.toHaveBeenCalled()
  })

  it('restores the session from a stored token via /auth/me on first load', async () => {
    mockedGetStoredToken.mockReturnValue('stored-jwt')
    mockedFetchCurrentUser.mockResolvedValue(USER_WITH_REPORT_VIEW)

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('user')).toHaveTextContent('Amine Test')
    expect(mockedFetchCurrentUser).toHaveBeenCalledTimes(1)
  })

  it('clears a stored token that /auth/me rejects (expired/invalid) instead of leaving a phantom session', async () => {
    mockedGetStoredToken.mockReturnValue('stale-jwt')
    mockedFetchCurrentUser.mockRejectedValue(new Error('401'))

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(mockedClearStoredToken).toHaveBeenCalledTimes(1)
  })

  it('hasPermission reflects only the permissions on the current user (backend @PreAuthorize remains the real guard)', async () => {
    mockedGetStoredToken.mockReturnValue('stored-jwt')
    mockedFetchCurrentUser.mockResolvedValue(USER_WITH_REPORT_VIEW)

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('can-view-report')).toHaveTextContent('true')
    expect(screen.getByTestId('can-export-report')).toHaveTextContent('false')
  })

  it('login stores the returned token and sets the user', async () => {
    mockedGetStoredToken.mockReturnValue(null)
    mockedLoginRequest.mockResolvedValue({
      accessToken: 'fresh-jwt',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: USER_WITH_REPORT_VIEW,
    })

    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))

    await user.click(screen.getByText('login'))

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('Amine Test'))
    expect(mockedSetStoredToken).toHaveBeenCalledWith('fresh-jwt')
  })

  it('logout clears the token and the user', async () => {
    mockedGetStoredToken.mockReturnValue('stored-jwt')
    mockedFetchCurrentUser.mockResolvedValue(USER_WITH_REPORT_VIEW)

    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('Amine Test'))

    await user.click(screen.getByText('logout'))

    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(mockedClearStoredToken).toHaveBeenCalledTimes(1)
  })

  it('reacts to a global 401 (UNAUTHORIZED_EVENT dispatched by api-client) by clearing the user', async () => {
    mockedGetStoredToken.mockReturnValue('stored-jwt')
    mockedFetchCurrentUser.mockResolvedValue(USER_WITH_REPORT_VIEW)

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('Amine Test'))

    act(() => {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    })

    expect(screen.getByTestId('user')).toHaveTextContent('none')
  })
})
