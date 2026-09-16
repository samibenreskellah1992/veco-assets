import { useQuery } from '@tanstack/react-query'

import { useAuth } from '@/hooks/use-auth'
import { dashboardApi } from '@/services/dashboard-service'
import type { CountByLabelDto } from '@/types/dashboard'
import { Card, CardContent } from '@/components/ui/card'

/**
 * Tableau de bord (prompt maitre Phase 9). Chaque chiffre vient de
 * GET /api/dashboard, recalculé à la demande côté backend depuis le parc
 * réel (voir DashboardService) - aucune valeur codée en dur ici, ce qui
 * satisfait le critère de vérification "chaque chiffre du dashboard est
 * vérifiable contre la base de données".
 */
export function DashboardPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('REPORT_VIEW')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La consultation du tableau de bord est réservée aux comptes disposant de la permission REPORT_VIEW.
        </CardContent>
      </Card>
    )
  }

  return <DashboardContent />
}

function DashboardContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.get,
  })

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tableau de bord</h1>
        <p className="text-sm text-muted-foreground">Vue d'ensemble du patrimoine VECOPHARM</p>
      </div>

      {isLoading && <p className="text-sm text-muted-foreground">Chargement...</p>}
      {isError && (
        <Card>
          <CardContent className="pt-6 text-sm text-destructive">
            Impossible de charger le tableau de bord. Vérifiez que le backend est démarré (voir README).
          </CardContent>
        </Card>
      )}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            <KpiTile label="Total immobilisations" value={data.totalAssets} />
            <KpiTile label="Étiquetées" value={data.labeledAssets} sub={`${data.unlabeledAssets} non étiquetées`} />
            <KpiTile label="Inventoriées" value={data.inventoriedAssets} sub={`${data.notInventoriedAssets} jamais inventoriées`} />
            <KpiTile label="Anomalies ouvertes" value={data.openAnomalies} tone={data.openAnomalies > 0 ? 'destructive' : undefined} />
            <KpiTile label="Valeur d'acquisition totale" value={formatAmount(data.totalAcquisitionValue)} isText />
            <KpiTile label="En stock" value={data.inStock} />
            <KpiTile label="En service" value={data.inService} />
            <KpiTile label="En maintenance" value={data.inMaintenance} tone={data.inMaintenance > 0 ? 'destructive' : undefined} />
            <KpiTile label="Réformées" value={data.reformed} />
            <KpiTile label="Sorties" value={data.exited} />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            <BreakdownCard title="Par site" data={data.bySite} />
            <BreakdownCard title="Par catégorie" data={data.byCategory} />
            <BreakdownCard title="Par état physique" data={data.byCondition} />
          </div>

          <Card>
            <CardContent className="pt-6">
              <h2 className="mb-3 text-sm font-medium">Activité récente</h2>
              {data.recentActivity.length === 0 ? (
                <p className="text-sm text-muted-foreground">Aucune activité enregistrée.</p>
              ) : (
                <ul className="divide-y divide-border">
                  {data.recentActivity.map((entry) => (
                    <li key={entry.id} className="flex items-center justify-between gap-4 py-2 text-sm">
                      <span>
                        <span className="font-medium">{entry.userFullName ?? 'Système'}</span>
                        {' — '}
                        {ACTION_LABEL[entry.action] ?? entry.action}
                        {entry.entityName ? ` (${entry.entityName})` : ''}
                      </span>
                      <span className="shrink-0 text-xs text-muted-foreground">{formatDateTime(entry.occurredAt)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}

function KpiTile({
  label,
  value,
  sub,
  tone,
  isText,
}: {
  label: string
  value: number | string
  sub?: string
  tone?: 'destructive'
  isText?: boolean
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className={`font-semibold ${isText ? 'text-lg' : 'text-2xl'} ${tone === 'destructive' ? 'text-destructive' : ''}`}>
          {value}
        </p>
        <p className="text-xs text-muted-foreground">{label}</p>
        {sub && <p className="mt-1 text-xs text-muted-foreground">{sub}</p>}
      </CardContent>
    </Card>
  )
}

/** Graphique en barres horizontales fait main (pas de dépendance graphique - voir docs/ARCHITECTURE.md). */
function BreakdownCard({ title, data }: { title: string; data: CountByLabelDto[] }) {
  const max = Math.max(1, ...data.map((d) => d.count))
  return (
    <Card>
      <CardContent className="pt-6">
        <h2 className="mb-3 text-sm font-medium">{title}</h2>
        {data.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucune donnée.</p>
        ) : (
          <ul className="space-y-2">
            {data.map((row) => (
              <li key={row.label}>
                <div className="mb-0.5 flex items-center justify-between text-xs">
                  <span className="truncate pr-2">{row.label}</span>
                  <span className="shrink-0 font-medium">{row.count}</span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${(row.count / max) * 100}%` }} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

const ACTION_LABEL: Record<string, string> = {
  CONNEXION: 'Connexion',
  CREATION: 'Création',
  MODIFICATION: 'Modification',
  SUPPRESSION_LOGIQUE: 'Suppression logique',
  SUPPRESSION: 'Suppression',
  AFFECTATION: 'Affectation',
  TRANSFERT: 'Transfert',
  INVENTAIRE: 'Inventaire',
  VALIDATION: 'Validation',
  CHANGEMENT_STATUT: 'Changement de statut',
  GENERATION_ETIQUETTE: "Génération d'étiquette",
}

function formatAmount(value: number) {
  return `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(value)} DZD`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('fr-FR')
}
