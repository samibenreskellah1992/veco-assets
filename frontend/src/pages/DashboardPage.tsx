import { useHealth } from '@/hooks/use-health'

/**
 * Phase 1 placeholder: proves the frontend is actually wired to the
 * backend REST layer (calls GET /api/health through TanStack Query).
 * The real dashboard (KPI tiles, charts, activité récente) is built in
 * Phase 9 - Reporting, against real data from the domain API.
 */
export function DashboardPage() {
  const { data, isLoading, isError } = useHealth()

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tableau de bord</h1>
        <p className="text-sm text-muted-foreground">Vue d'ensemble de votre patrimoine</p>
      </div>

      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="mb-2 text-sm font-medium text-muted-foreground">
          État de la connexion backend
        </h2>
        {isLoading && <p className="text-sm">Vérification...</p>}
        {isError && (
          <p className="text-sm text-destructive">
            Backend injoignable. Démarrez l'API (voir README) puis rechargez.
          </p>
        )}
        {data && (
          <p className="text-sm text-success">
            {data.application} — {data.status} ({new Date(data.timestamp).toLocaleString('fr-FR')})
          </p>
        )}
      </div>

      <div className="rounded-lg border border-dashed border-border p-6 text-sm text-muted-foreground">
        Les indicateurs (total immobilisations, étiquetées, anomalies, graphiques par site/catégorie/état,
        activité récente) seront branchés sur des données réelles en Phase 9 — Reporting.
      </div>
    </div>
  )
}
