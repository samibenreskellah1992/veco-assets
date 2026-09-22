import { useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ScanLine } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { locationInventorySessionsApi, inventoryAnomaliesApi } from '@/services/inventory-service'
import { locationsApi } from '@/services/referentiel-service'
import { assetsApi } from '@/services/asset-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import {
  ANOMALY_STATUS_LABEL,
  ANOMALY_TYPE_LABEL,
  LOCATION_SESSION_STATUS_LABEL,
  type AnomalyType,
  type InventoryAnomalyDto,
  type LocationSessionStatus,
  type ScanResult,
} from '@/types/inventory'
import type { MovementPrefill } from '@/types/movement'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const STATUS_BADGE_VARIANT: Record<LocationSessionStatus, 'default' | 'success'> = {
  EN_COURS: 'default',
  VALIDEE: 'success',
}

const ANOMALY_STATUS_BADGE_VARIANT: Record<string, 'default' | 'secondary' | 'success' | 'destructive'> = {
  NOUVELLE: 'destructive',
  EN_COURS: 'default',
  RESOLUE: 'success',
  REJETEE: 'secondary',
}

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('fr-FR')
}

/**
 * Checkpoint 3 de l'evolution "locaux scannables" (2026-09) : session de
 * scan d'inventaire pour UN local - meme structure d'ecran que
 * InventoryCampaignDetailPage (Phase 7), volontairement allegee (pas de
 * workflow multi-etapes, juste EN_COURS -> Validée).
 */
export function LocationInventorySessionDetailPage() {
  const { sessionId } = useParams<{ id: string; sessionId: string }>()
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()

  const [validateOpen, setValidateOpen] = useState(false)

  const { data: session, isLoading } = useQuery({
    queryKey: ['location-inventory-sessions', sessionId],
    queryFn: () => locationInventorySessionsApi.get(sessionId!),
  })
  const { data: location } = useQuery({
    queryKey: ['locations', session?.locationId],
    queryFn: () => locationsApi.get(session!.locationId),
    enabled: Boolean(session),
  })
  const { data: progress } = useQuery({
    queryKey: ['location-inventory-sessions', sessionId, 'progress'],
    queryFn: () => locationInventorySessionsApi.progress(sessionId!),
    enabled: Boolean(session),
  })

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['location-inventory-sessions', sessionId] })
    queryClient.invalidateQueries({ queryKey: ['location-inventory-sessions'] })
    queryClient.invalidateQueries({ queryKey: ['locations', session?.locationId] })
  }

  const validateMutation = useMutation({
    mutationFn: () => locationInventorySessionsApi.validate(sessionId!),
    onSuccess: () => {
      invalidateAll()
      setValidateOpen(false)
      toast.success('Session validée — dernier inventaire du local mis à jour')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  if (isLoading || !session) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  return (
    <div className="space-y-4">
      <div>
        <Link
          to={`/locaux/${session.locationId}`}
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" /> Retour au local
        </Link>
      </div>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">Session de scan — {session.locationName}</h1>
            <Badge variant={STATUS_BADGE_VARIANT[session.status]}>{LOCATION_SESSION_STATUS_LABEL[session.status]}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {location && [location.siteName, location.buildingName, location.floorName, location.zoneName].filter(Boolean).join(' / ')}
            {' · '}Ouverte le {formatDateTime(session.openedAt)}
            {session.openedByName && <> par {session.openedByName}</>}
            {session.status === 'VALIDEE' && (
              <>
                {' · '}Validée le {formatDateTime(session.validatedAt)}
                {session.validatedByName && <> par {session.validatedByName}</>}
              </>
            )}
          </p>
        </div>
        <div className="flex gap-2">
          {session.status === 'EN_COURS' && hasPermission('INVENTAIRE_VALIDATE') && (
            <Button onClick={() => setValidateOpen(true)}>Valider la session</Button>
          )}
        </div>
      </div>

      {progress && (
        <Card>
          <CardContent className="grid grid-cols-2 gap-4 pt-6 sm:grid-cols-5">
            <Stat label="Attendues" value={progress.totalAssetsExpected} />
            <Stat label="Scannées" value={progress.scannedAssetsCount} />
            <Stat label="Présentes" value={progress.presentCount} />
            <Stat label="Anomalies" value={progress.anomaliesCount} tone="destructive" />
            <Stat label="Restantes" value={progress.remainingCount} />
            <div className="col-span-2 sm:col-span-5">
              <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${Math.min(100, progress.progressPercent)}%` }}
                />
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{progress.progressPercent}% des immobilisations attendues scannées</p>
            </div>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue={session.status === 'EN_COURS' ? 'scanner' : 'anomalies'}>
        <TabsList>
          <TabsTrigger value="scanner">Scanner</TabsTrigger>
          <TabsTrigger value="anomalies">Anomalies</TabsTrigger>
          <TabsTrigger value="historique">Historique des scans</TabsTrigger>
          <TabsTrigger value="restantes">Restantes</TabsTrigger>
        </TabsList>

        <TabsContent value="scanner">
          <ScanPanel sessionId={session.id} sessionStatus={session.status} />
        </TabsContent>

        <TabsContent value="anomalies">
          <AnomaliesPanel sessionId={session.id} location={location} />
        </TabsContent>

        <TabsContent value="historique">
          <ScanHistoryPanel sessionId={session.id} />
        </TabsContent>

        <TabsContent value="restantes">
          <PendingAssetsPanel sessionId={session.id} />
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={validateOpen}
        onOpenChange={setValidateOpen}
        title="Valider la session"
        description="Une fois validée, la session n'accepte plus de scans et le local est marqué comme inventorié à l'instant. Confirmer la validation ?"
        confirmLabel="Valider"
        isConfirming={validateMutation.isPending}
        onConfirm={() => validateMutation.mutate()}
      />
    </div>
  )
}

function Stat({ label, value, tone }: { label: string; value: number; tone?: 'destructive' }) {
  return (
    <div>
      <p className={`text-2xl font-semibold ${tone === 'destructive' && value > 0 ? 'text-destructive' : ''}`}>{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  )
}

function ScanPanel({ sessionId, sessionStatus }: { sessionId: string; sessionStatus: LocationSessionStatus }) {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)

  const [assetCode, setAssetCode] = useState('')
  const [result, setResult] = useState<ScanResult>('PRESENT')
  const [anomalyType, setAnomalyType] = useState<AnomalyType>('AUTRE')
  const [comment, setComment] = useState('')

  const scanMutation = useMutation({
    mutationFn: () =>
      locationInventorySessionsApi.scan(sessionId, {
        assetCode: assetCode.trim(),
        result,
        anomalyType: result === 'ANOMALIE' ? anomalyType : undefined,
        comment: comment.trim() || undefined,
      }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['location-inventory-sessions', sessionId] })
      if (!response.assetRecognized) {
        toast.error(`Code non reconnu : ${assetCode.trim()} — anomalie enregistrée`)
      } else if (response.anomaly) {
        toast.warning(`${response.scan?.assetCode} — anomalie enregistrée (${ANOMALY_TYPE_LABEL[response.anomaly.anomalyType]})`)
      } else {
        toast.success(`${response.scan?.assetCode} — présente`)
      }
      setAssetCode('')
      setComment('')
      setResult('PRESENT')
      inputRef.current?.focus()
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  if (!hasPermission('INVENTAIRE_EXECUTE')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          Le scan est réservé aux comptes disposant de la permission INVENTAIRE_EXECUTE.
        </CardContent>
      </Card>
    )
  }
  if (sessionStatus !== 'EN_COURS') {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          Cette session n'accepte plus de scan (statut : {LOCATION_SESSION_STATUS_LABEL[sessionStatus]}).
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <ScanLine className="h-4 w-4" /> Scanner une immobilisation
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            if (assetCode.trim()) scanMutation.mutate()
          }}
          className="space-y-4"
        >
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">
              Code immobilisation (scan QR ou saisie manuelle)
            </label>
            <Input
              ref={inputRef}
              autoFocus
              className="text-lg"
              placeholder="VECO-IMM-000001"
              value={assetCode}
              onChange={(event) => setAssetCode(event.target.value)}
            />
          </div>

          <div className="flex gap-2">
            <Button
              type="button"
              variant={result === 'PRESENT' ? 'default' : 'outline'}
              className="flex-1"
              onClick={() => setResult('PRESENT')}
            >
              Présente
            </Button>
            <Button
              type="button"
              variant={result === 'ANOMALIE' ? 'destructive' : 'outline'}
              className="flex-1"
              onClick={() => setResult('ANOMALIE')}
            >
              Anomalie
            </Button>
          </div>

          {result === 'ANOMALIE' && (
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">Type d'anomalie</label>
              <Select value={anomalyType} onValueChange={(value) => setAnomalyType(value as AnomalyType)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(ANOMALY_TYPE_LABEL).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Commentaire (facultatif)</label>
            <Textarea value={comment} onChange={(event) => setComment(event.target.value)} rows={2} />
          </div>

          <Button type="submit" size="lg" className="w-full" disabled={!assetCode.trim() || scanMutation.isPending}>
            {scanMutation.isPending ? 'Enregistrement...' : 'Enregistrer le scan'}
          </Button>
        </form>
        <p className="text-xs text-muted-foreground">
          Un code qui ne correspond à aucune immobilisation connue, ou à une immobilisation rattachée à un AUTRE local, est
          automatiquement enregistré comme anomalie de localisation — jamais de déplacement automatique de l'immobilisation.
        </p>
      </CardContent>
    </Card>
  )
}

function AnomaliesPanel({
  sessionId,
  location,
}: {
  sessionId: string
  location: { id: string; siteId: string | null; buildingId: string | null; floorId: string | null; zoneId: string; name: string } | undefined
}) {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const canValidate = hasPermission('INVENTAIRE_VALIDATE')
  const canProposeMovement = hasPermission('MOUVEMENT_CREATE')

  const { data: anomalies, isLoading } = useQuery({
    queryKey: ['location-inventory-sessions', sessionId, 'anomalies'],
    queryFn: () => locationInventorySessionsApi.anomalies(sessionId),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: 'EN_COURS' | 'RESOLUE' | 'REJETEE' }) =>
      inventoryAnomaliesApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['location-inventory-sessions', sessionId, 'anomalies'] })
      toast.success('Statut mis à jour')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  /**
   * BR-LOC-006 : une immobilisation trouvée dans le mauvais local n'est
   * JAMAIS déplacée automatiquement - ce bouton ouvre uniquement le
   * formulaire "Nouveau mouvement" pré-rempli (page Mouvements),
   * l'utilisateur garde la main pour vérifier et confirmer la demande.
   * Type choisi selon que le site actuel de l'immobilisation correspond
   * ou non au site de ce local (CHANGEMENT_LOCALISATION vs
   * TRANSFERT_INTER_SITE - MovementService#applyTargetFields côté backend).
   */
  async function proposeRelocation(anomaly: InventoryAnomalyDto) {
    if (!anomaly.assetId || !location) return
    try {
      const asset = await queryClient.fetchQuery({ queryKey: ['assets', anomaly.assetId], queryFn: () => assetsApi.get(anomaly.assetId!) })
      const sameSite = Boolean(location.siteId) && asset.siteId === location.siteId
      const prefill: MovementPrefill = sameSite
        ? {
            assetId: asset.id,
            movementType: 'CHANGEMENT_LOCALISATION',
            toBuildingId: location.buildingId ?? undefined,
            toFloorId: location.floorId ?? undefined,
            toZoneId: location.zoneId,
            toLocationId: location.id,
            reason: `Anomalie de localisation détectée lors du scan du local ${location.name}`,
          }
        : {
            assetId: asset.id,
            movementType: 'TRANSFERT_INTER_SITE',
            toSiteId: location.siteId ?? undefined,
            toBuildingId: location.buildingId ?? undefined,
            toFloorId: location.floorId ?? undefined,
            toZoneId: location.zoneId,
            toLocationId: location.id,
            reason: `Anomalie de localisation détectée lors du scan du local ${location.name} (autre site)`,
          }
      navigate('/mouvements', { state: { prefill } })
    } catch (error) {
      toast.error(extractApiErrorMessage(error))
    }
  }

  return (
    <div className="rounded-lg border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Immobilisation</TableHead>
            <TableHead>Type</TableHead>
            <TableHead>Description</TableHead>
            <TableHead>Signalée par</TableHead>
            <TableHead>Statut</TableHead>
            <TableHead className="w-64">Action</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                Chargement...
              </TableCell>
            </TableRow>
          )}
          {!isLoading && anomalies?.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="text-center text-muted-foreground">
                Aucune anomalie pour cette session.
              </TableCell>
            </TableRow>
          )}
          {anomalies?.map((anomaly) => (
            <TableRow key={anomaly.id}>
              <TableCell className="font-mono text-xs">{anomaly.assetCode ?? '— (non référencée)'}</TableCell>
              <TableCell>{ANOMALY_TYPE_LABEL[anomaly.anomalyType]}</TableCell>
              <TableCell className="max-w-xs truncate text-sm text-muted-foreground">{anomaly.description ?? '—'}</TableCell>
              <TableCell className="text-sm text-muted-foreground">{anomaly.reportedByName ?? '—'}</TableCell>
              <TableCell>
                <Badge variant={ANOMALY_STATUS_BADGE_VARIANT[anomaly.status]}>{ANOMALY_STATUS_LABEL[anomaly.status]}</Badge>
              </TableCell>
              <TableCell>
                <div className="flex flex-wrap gap-1">
                  {canProposeMovement && anomaly.anomalyType === 'MAUVAISE_LOCALISATION' && anomaly.assetId && (
                    <Button size="sm" variant="outline" onClick={() => proposeRelocation(anomaly)}>
                      Proposer un changement de localisation
                    </Button>
                  )}
                  {canValidate && anomaly.status !== 'RESOLUE' && anomaly.status !== 'REJETEE' && (
                    <>
                      <Button size="sm" variant="outline" onClick={() => statusMutation.mutate({ id: anomaly.id, status: 'RESOLUE' })}>
                        Résoudre
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => statusMutation.mutate({ id: anomaly.id, status: 'REJETEE' })}>
                        Rejeter
                      </Button>
                    </>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function ScanHistoryPanel({ sessionId }: { sessionId: string }) {
  const { data: scans, isLoading } = useQuery({
    queryKey: ['location-inventory-sessions', sessionId, 'scans'],
    queryFn: () => locationInventorySessionsApi.scans(sessionId),
  })

  return (
    <div className="rounded-lg border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Immobilisation</TableHead>
            <TableHead>Scanné par</TableHead>
            <TableHead>Date</TableHead>
            <TableHead>Résultat</TableHead>
            <TableHead>Commentaire</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-muted-foreground">
                Chargement...
              </TableCell>
            </TableRow>
          )}
          {!isLoading && scans?.length === 0 && (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-muted-foreground">
                Aucun scan enregistré pour l'instant.
              </TableCell>
            </TableRow>
          )}
          {scans?.map((scan) => (
            <TableRow key={scan.id}>
              <TableCell className="font-mono text-xs">{scan.assetCode ?? '—'}</TableCell>
              <TableCell className="text-sm text-muted-foreground">{scan.scannedByName ?? '—'}</TableCell>
              <TableCell>{formatDateTime(scan.scannedAt)}</TableCell>
              <TableCell>
                <Badge variant={scan.result === 'PRESENT' ? 'success' : 'destructive'}>
                  {scan.result === 'PRESENT' ? 'Présente' : 'Anomalie'}
                </Badge>
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">{scan.comment ?? '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function PendingAssetsPanel({ sessionId }: { sessionId: string }) {
  const { data: pending, isLoading } = useQuery({
    queryKey: ['location-inventory-sessions', sessionId, 'pending-assets'],
    queryFn: () => locationInventorySessionsApi.pendingAssets(sessionId),
  })

  return (
    <div className="rounded-lg border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Code</TableHead>
            <TableHead>Désignation</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow>
              <TableCell colSpan={2} className="text-center text-muted-foreground">
                Chargement...
              </TableCell>
            </TableRow>
          )}
          {!isLoading && pending?.length === 0 && (
            <TableRow>
              <TableCell colSpan={2} className="text-center text-muted-foreground">
                Toutes les immobilisations attendues dans ce local ont été scannées.
              </TableCell>
            </TableRow>
          )}
          {pending?.map((asset) => (
            <TableRow key={asset.id}>
              <TableCell className="font-mono text-xs">{asset.assetCode}</TableCell>
              <TableCell className="font-medium">{asset.designation}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
