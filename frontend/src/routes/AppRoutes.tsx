import { Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { LoginPage } from '@/pages/LoginPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { ReferentielPage } from '@/pages/ReferentielPage'
import { UsersPage } from '@/pages/UsersPage'
import { AssetsPage } from '@/pages/AssetsPage'
import { AssetFormPage } from '@/pages/AssetFormPage'
import { AssetDetailPage } from '@/pages/AssetDetailPage'
import { LocationsPage } from '@/pages/LocationsPage'
import { LocationFormPage } from '@/pages/LocationFormPage'
import { LocationDetailPage } from '@/pages/LocationDetailPage'
import { LocationInventorySessionDetailPage } from '@/pages/LocationInventorySessionDetailPage'
import { EtiquetagePage } from '@/pages/EtiquetagePage'
import { InventoryCampaignsPage } from '@/pages/InventoryCampaignsPage'
import { InventoryCampaignDetailPage } from '@/pages/InventoryCampaignDetailPage'
import { AnomaliesPage } from '@/pages/AnomaliesPage'
import { MouvementsPage } from '@/pages/MouvementsPage'
import { ReportsPage } from '@/pages/ReportsPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="immobilisations" element={<AssetsPage />} />
          <Route path="immobilisations/nouveau" element={<AssetFormPage />} />
          <Route path="immobilisations/:id" element={<AssetDetailPage />} />
          <Route path="immobilisations/:id/modifier" element={<AssetFormPage />} />
          <Route path="locaux" element={<LocationsPage />} />
          <Route path="locaux/nouveau" element={<LocationFormPage />} />
          <Route path="locaux/:id" element={<LocationDetailPage />} />
          <Route path="locaux/:id/modifier" element={<LocationFormPage />} />
          <Route path="locaux/:id/scan/:sessionId" element={<LocationInventorySessionDetailPage />} />
          <Route path="etiquetage" element={<EtiquetagePage />} />
          <Route path="inventaires" element={<InventoryCampaignsPage />} />
          <Route path="inventaires/:id" element={<InventoryCampaignDetailPage />} />
          <Route path="mouvements" element={<MouvementsPage />} />
          <Route path="anomalies" element={<AnomaliesPage />} />
          <Route path="sites" element={<ReferentielPage />} />
          <Route path="utilisateurs" element={<UsersPage />} />
          <Route path="rapports" element={<ReportsPage />} />
          <Route
            path="administration"
            element={<PlaceholderPage title="Administration" phase="Phase 10 — Qualité (journal d'audit)" />}
          />
        </Route>
      </Route>
    </Routes>
  )
}
