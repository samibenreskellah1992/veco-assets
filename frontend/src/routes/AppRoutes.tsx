import { Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { LoginPage } from '@/pages/LoginPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { ReferentielPage } from '@/pages/ReferentielPage'
import { UsersPage } from '@/pages/UsersPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route
            path="immobilisations"
            element={<PlaceholderPage title="Immobilisations" phase="Phase 5 — Immobilisations" />}
          />
          <Route
            path="etiquetage"
            element={<PlaceholderPage title="Étiquetage" phase="Phase 6 — Étiquetage" />}
          />
          <Route
            path="inventaires"
            element={<PlaceholderPage title="Inventaires" phase="Phase 7 — Inventaire" />}
          />
          <Route
            path="mouvements"
            element={<PlaceholderPage title="Mouvements" phase="Phase 8 — Mouvements" />}
          />
          <Route
            path="anomalies"
            element={<PlaceholderPage title="Anomalies" phase="Phase 7 — Inventaire" />}
          />
          <Route path="sites" element={<ReferentielPage />} />
          <Route path="utilisateurs" element={<UsersPage />} />
          <Route
            path="rapports"
            element={<PlaceholderPage title="Rapports" phase="Phase 9 — Reporting" />}
          />
          <Route
            path="administration"
            element={<PlaceholderPage title="Administration" phase="Phase 10 — Qualité (journal d'audit)" />}
          />
        </Route>
      </Route>
    </Routes>
  )
}
