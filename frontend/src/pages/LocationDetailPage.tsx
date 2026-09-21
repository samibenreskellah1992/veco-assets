import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Pencil } from 'lucide-react'

import { useAuth } from '@/hooks/use-auth'
import { locationsApi } from '@/services/referentiel-service'
import { assetsApi } from '@/services/asset-service'
import { LOCATION_STATUS_LABEL, type LocationStatus } from '@/types/referentiel'
import { ASSET_CONDITION_LABEL, ASSET_STATUS_LABEL, type AssetCondition, type AssetStatus } from '@/types/asset'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

function statusBadgeVariant(status: LocationStatus) {
  if (status === 'ACTIF') return 'success' as const
  if (status === 'FERME' || status === 'INACTIF') return 'destructive' as const
  return 'secondary' as const
}

function conditionBadgeVariant(condition: AssetCondition) {
  if (condition === 'NEUF' || condition === 'BON') return 'success' as const
  if (condition === 'MOYEN') return 'secondary' as const
  return 'destructive' as const
}

function statusAssetBadgeVariant(status: AssetStatus) {
  if (status === 'EN_SERVICE') return 'success' as const
  if (status === 'REFORME' || status === 'SORTI') return 'destructive' as const
  return 'secondary' as const
}

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('fr-FR')
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="text-sm">{value ?? '—'}</p>
    </div>
  )
}

/**
 * Checkpoint 1 de l'evolution "locaux scannables" (2026-09) : fiche de
 * consultation d'un local. N'affiche que le nombre d'immobilisations
 * rattachées et le dernier inventaire — la comparaison attendu/scanné, les
 * anomalies et le scan QR lui-même sont des phases suivantes, hors
 * périmètre ici (voir le prompt détaillé de Sami, sections 6+).
 */
export function LocationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasPermission } = useAuth()

  const { data: location, isLoading } = useQuery({ queryKey: ['locations', id], queryFn: () => locationsApi.get(id!) })
  const { data: assets } = useQuery({
    queryKey: ['assets', { locationId: id }],
    queryFn: () => assetsApi.list({ locationId: id!, size: 100 }),
    enabled: Boolean(id),
  })

  if (isLoading || !location) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/locaux" className="mb-1 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> Retour à la liste
          </Link>
          <h1 className="text-2xl font-semibold tracking-tight">
            {location.name}
            <Badge variant={statusBadgeVariant(location.status)} className="ml-2 align-middle">
              {LOCATION_STATUS_LABEL[location.status]}
            </Badge>
          </h1>
          <p className="font-mono text-sm text-muted-foreground">{location.qrCode}</p>
        </div>
        <div className="flex gap-2">
          {hasPermission('REFERENTIEL_MANAGE') && (
            <Button variant="outline" onClick={() => navigate(`/locaux/${location.id}/modifier`)}>
              <Pencil className="h-4 w-4" /> Modifier
            </Button>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Localisation</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <Field label="Site" value={location.siteName} />
          <Field label="Bâtiment" value={location.buildingName} />
          <Field label="Étage" value={location.floorName} />
          <Field label="Zone" value={location.zoneName} />
        </CardContent>
        {location.description && (
          <CardContent className="pt-0 text-sm whitespace-pre-wrap text-muted-foreground">{location.description}</CardContent>
        )}
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Responsable</CardTitle>
        </CardHeader>
        <CardContent>
          <Field label="Responsable du local" value={location.responsibleUserName} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Inventaire</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
          <Field label="Immobilisations affectées" value={location.assetCount} />
          <Field
            label="Dernier inventaire"
            value={location.lastInventoryAt ? formatDateTime(location.lastInventoryAt) : 'Jamais inventorié'}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Immobilisations affectées à ce local</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Code</TableHead>
                <TableHead>Désignation</TableHead>
                <TableHead>État</TableHead>
                <TableHead>Statut</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(!assets || assets.content.length === 0) && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    Aucune immobilisation rattachée à ce local.
                  </TableCell>
                </TableRow>
              )}
              {assets?.content.map((asset) => (
                <TableRow key={asset.id}>
                  <TableCell className="font-mono text-xs">
                    <Link to={`/immobilisations/${asset.id}`} className="text-primary hover:underline">
                      {asset.assetCode}
                    </Link>
                  </TableCell>
                  <TableCell className="font-medium">{asset.designation}</TableCell>
                  <TableCell>
                    <Badge variant={conditionBadgeVariant(asset.condition)}>{ASSET_CONDITION_LABEL[asset.condition]}</Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusAssetBadgeVariant(asset.status)}>{ASSET_STATUS_LABEL[asset.status]}</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
