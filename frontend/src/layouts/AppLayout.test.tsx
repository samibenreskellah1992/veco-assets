import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppLayout } from './AppLayout'

// Regression Phase 10 (tache #89, revue responsive) : la barre laterale
// est devenue un tiroir hors-champ en dessous de `lg` - ce test verifie
// que le bouton hamburger l'ouvre/la ferme (classe -translate-x-full vs
// translate-x-0), pas le rendu visuel a une largeur d'ecran donnee (jsdom
// n'a pas de vrai viewport CSS).
vi.mock('@/hooks/use-auth')

import { useAuth } from '@/hooks/use-auth'

const mockedUseAuth = vi.mocked(useAuth)

describe('AppLayout mobile navigation', () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({
      user: {
        id: 'u1',
        matricule: null,
        fullName: 'Amine Test',
        email: 'amine@vecopharm.dz',
        siteName: 'Siege',
        department: null,
        service: null,
        roles: ['GESTIONNAIRE_PATRIMOINE'],
        permissions: [],
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      hasPermission: () => true,
    })
  })

  function renderLayout() {
    return render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<div>Contenu de la page</div>} />
            <Route path="*" element={<div>Autre page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )
  }

  function asideElement() {
    // La navigation liste "Dashboard" en premier - on remonte a l'<aside> depuis ce lien.
    return screen.getByRole('link', { name: /dashboard/i }).closest('aside')
  }

  it('starts with the drawer off-screen (closed) and opens it via the hamburger button', async () => {
    const user = userEvent.setup()
    renderLayout()

    expect(asideElement()).toHaveClass('-translate-x-full')

    await user.click(screen.getByTitle('Ouvrir le menu'))

    expect(asideElement()).toHaveClass('translate-x-0')
    expect(asideElement()).not.toHaveClass('-translate-x-full')
  })

  it('closes the drawer when a nav link is clicked (so navigating on mobile does not leave it open)', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByTitle('Ouvrir le menu'))
    expect(asideElement()).toHaveClass('translate-x-0')

    await user.click(screen.getByRole('link', { name: /immobilisations/i }))

    expect(asideElement()).toHaveClass('-translate-x-full')
  })

  it('closes the drawer when the overlay is clicked', async () => {
    const user = userEvent.setup()
    const { container } = renderLayout()

    await user.click(screen.getByTitle('Ouvrir le menu'))
    expect(asideElement()).toHaveClass('translate-x-0')

    const overlay = container.querySelector('.bg-black\\/50')
    expect(overlay).not.toBeNull()
    await user.click(overlay as Element)

    expect(asideElement()).toHaveClass('-translate-x-full')
  })
})
