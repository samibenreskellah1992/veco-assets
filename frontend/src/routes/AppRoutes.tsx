import { Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { DashboardPage } from '@/pages/DashboardPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function AppRoutes() {
  return (
    <Routes>
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
        <Route
          path="sites"
          element={<PlaceholderPage title="Sites & Localisations" phase="Phase 4 — Référentiel" />}
        />
        <Route
          path="utilisateurs"
          element={<PlaceholderPage title="Utilisateurs" phase="Phase 4 — Référentiel" />}
        />
        <Route
          path="rapports"
          element={<PlaceholderPage title="Rapports" phase="Phase 9 — Reporting" />}
        />
        <Route
          path="administration"
          element={<PlaceholderPage title="Administration" phase="Phase 4 / Phase 10" />}
        />
      </Route>
    </Routes>
  )
}
