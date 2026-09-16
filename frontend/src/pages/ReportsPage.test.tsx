import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ReportsPage } from './ReportsPage'

// La page /rapports est entierement gatee par la permission REPORT_VIEW
// (voir ReportsPage.tsx) - c'est le parcours critique "permissions" cite
// par le prompt maitre Phase 10 (tache #88) pour le frontend. On mocke
// useAuth pour piloter les deux etats (refuse / autorise) sans dependre
// d'un vrai login, et les services reseau pour ne pas dependre du backend.
vi.mock('@/hooks/use-auth')
vi.mock('@/services/report-service')
vi.mock('@/services/referentiel-service')

import { useAuth } from '@/hooks/use-auth'
import { reportsApi } from '@/services/report-service'
import { assetCategoriesApi, sitesApi } from '@/services/referentiel-service'

const mockedUseAuth = vi.mocked(useAuth)
const mockedReportsApi = vi.mocked(reportsApi, true)
const mockedSitesApi = vi.mocked(sitesApi, true)
const mockedAssetCategoriesApi = vi.mocked(assetCategoriesApi, true)

function renderWithQueryClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ReportsPage />
    </QueryClientProvider>,
  )
}

function authWith(permissions: string[]) {
  mockedUseAuth.mockReturnValue({
    user: {
      id: 'u1',
      matricule: null,
      fullName: 'Test User',
      email: 'test@vecopharm.dz',
      siteName: null,
      department: null,
      service: null,
      roles: [],
      permissions,
    },
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
    hasPermission: (code: string) => permissions.includes(code),
  })
}

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedSitesApi.list.mockResolvedValue([])
    mockedAssetCategoriesApi.list.mockResolvedValue([])
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows an access-denied message and never calls the reports API when the user lacks REPORT_VIEW', async () => {
    authWith(['IMMOBILISATION_VIEW'])

    renderWithQueryClient()

    expect(
      screen.getByText(/r.serv.e aux comptes disposant de la permission REPORT_VIEW/i),
    ).toBeInTheDocument()
    expect(screen.queryByText('Rapports')).not.toBeInTheDocument()
    expect(mockedReportsApi.get).not.toHaveBeenCalled()
  })

  it('renders the report table for a user with REPORT_VIEW, but hides export buttons without REPORT_EXPORT', async () => {
    authWith(['REPORT_VIEW'])
    mockedReportsApi.get.mockResolvedValue({
      type: 'PAR_SITE',
      title: 'Répartition par site',
      generatedAt: '2026-06-01T10:00:00Z',
      columns: ['Site', 'Nombre', 'Valeur'],
      rows: [['Siege', '3', '15000.00']],
    })

    renderWithQueryClient()

    expect(await screen.findByText('Rapports')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Répartition par site')).toBeInTheDocument())
    expect(screen.getByText('Siege')).toBeInTheDocument()
    expect(mockedReportsApi.get).toHaveBeenCalledWith('PAR_SITE', expect.any(Object))

    // REPORT_EXPORT non accorde : les boutons CSV/Excel/PDF ne doivent pas apparaitre.
    expect(screen.queryByText('CSV')).not.toBeInTheDocument()
    expect(screen.queryByText('Excel')).not.toBeInTheDocument()
    expect(screen.queryByText('PDF')).not.toBeInTheDocument()
  })

  it('shows the export buttons when the user also has REPORT_EXPORT', async () => {
    authWith(['REPORT_VIEW', 'REPORT_EXPORT'])
    mockedReportsApi.get.mockResolvedValue({
      type: 'PAR_SITE',
      title: 'Répartition par site',
      generatedAt: '2026-06-01T10:00:00Z',
      columns: ['Site', 'Nombre', 'Valeur'],
      rows: [],
    })

    renderWithQueryClient()

    expect(await screen.findByText('CSV')).toBeInTheDocument()
    expect(screen.getByText('Excel')).toBeInTheDocument()
    expect(screen.getByText('PDF')).toBeInTheDocument()
  })

  it('shows the empty-state message when the report has no rows', async () => {
    authWith(['REPORT_VIEW'])
    mockedReportsApi.get.mockResolvedValue({
      type: 'REFORMES',
      title: 'Immobilisations réformées',
      generatedAt: '2026-06-01T10:00:00Z',
      columns: ['Code'],
      rows: [],
    })

    renderWithQueryClient()

    expect(await screen.findByText('Aucune donnée pour les filtres sélectionnés.')).toBeInTheDocument()
  })

  it('surfaces the backend error message when the report query fails', async () => {
    mockedUseAuth.mockReturnValue({
      user: { id: 'u1', matricule: null, fullName: 'Test', email: 't@t.dz', siteName: null, department: null, service: null, roles: [], permissions: ['REPORT_VIEW'] },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      hasPermission: (code: string) => code === 'REPORT_VIEW',
    })
    mockedReportsApi.get.mockRejectedValue(new Error('boom'))

    renderWithQueryClient()

    expect(await screen.findByText('Impossible de charger ce rapport.')).toBeInTheDocument()
  })
})
