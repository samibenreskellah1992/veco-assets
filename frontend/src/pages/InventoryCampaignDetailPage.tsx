import { useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ScanLine } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { inventoryAnomaliesApi, inventoryCampaignsApi } from '@/services/inventory-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import {
  ANOMALY_STATUS_LABEL,
  ANOMALY_TYPE_LABEL,
  CAMPAIGN_STATUS_LABEL,
  CAMPAIGN_STATUS_ORDER,
  type AnomalyType,
  type CampaignStatus,
  type ScanResult,
} from '@/types/inventory'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const STATUS_BADGE_VARIANT: Record<CampaignStatus, 'default' | 'secondary' | 'success' | 'destructive'> = {
  BROUILLON: 'secondary',
  EN_PREPARATION: 'secondary',
  EN_COURS: 'default',
  TERMINE: 'default',
  VALIDE: 'success',
  CLOTURE: 'secondary',
}

const ANOMALY_STATUS_BADGE_VARIANT: Record<string, 'default' | 'secondary' | 'success' | 'destructive'> = {
  NOUVELLE: 'destructive',
  EN_COURS: 'default',
  RESOLUE: 'success',
  REJETEE: 'secondary',
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('fr-FR')
}

export function InventoryCampaignDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()

  const [validateOpen, setValidateOpen] = useState(false)
  const [closeOpen, setCloseOpen] = useState(false)

  const { data: campaign, isLoading } = useQuery({
    queryKey: ['inventory-campaigns', id],
    queryFn: () => inventoryCampaignsApi.get(id!),
  })
  const { data: progress } = useQuery({
    queryKey: ['inventory-campaigns', id, 'progress'],
    queryFn: () => inventoryCampaignsApi.progress(id!),
    enabled: Boolean(campaign),
  })

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['inventory-campaigns', id] })
    queryClient.invalidateQueries({ queryKey: ['inventory-campaigns'] })
  }

  const advanceMutation = useMutation({
    mutationFn: (status: CampaignStatus) => inventoryCampaignsApi.advance(id!, status),
    onSuccess: () => {
      invalidateAll()
      toast.success('Campagne mise à jour')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })
  const validateMutation = useMutation({
    mutationFn: () => inventoryCampaignsApi.validate(id!),
    onSuccess: () => {
      invalidateAll()
      setValidateOpen(false)
      toast.success('Campagne validée')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })
  const closeMutation = useMutation({
    mutationFn: () => inventoryCampaignsApi.close(id!),
    onSuccess: () => {
      invalidateAll()
      setCloseOpen(false)
      toast.success('Campagne clôturée')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  if (isLoading || !campaign) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  const nextIndex = CAMPAIGN_STATUS_ORDER.indexOf(campaign.status) + 1
  const nextAdvanceTarget = nextIndex < 4 ? CAMPAIGN_STATUS_ORDER[nextIndex] : null // stops at TERMINE (index 3)

  return (
    <div className="space-y-4">
      <div>
        <Link to="/inventaires" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" /> Retour aux inventaires
        </Link>
      </div>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{campaign.name}</h1>
            <Badge variant={STATUS_BADGE_VARIANT[campaign.status]}>{CAMPAIGN_STATUS_LABEL[campaign.status]}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {campaign.siteName} — {campaign.zoneName ?? 'Tout le site'} · {campaign.startDate} → {campaign.endDate}
            {campaign.responsibleUserName && <> · Responsable : {campaign.responsibleUserName}</>}
          </p>
        </div>
        <div className="flex gap-2">
          {nextAdvanceTarget && hasPermission('INVENTAIRE_CREATE') && (
            <Button onClick={() => advanceMutation.mutate(nextAdvanceTarget)} disabled={advanceMutation.isPending}>
              {advanceMutation.isPending ? 'Mise à jour...' : `Passer à « ${CAMPAIGN_STATUS_LABEL[nextAdvanceTarget]} »`}
            </Button>
          )}
          {campaign.status === 'TERMINE' && hasPermission('INVENTAIRE_VALIDATE') && (
            <Button onClick={() => setValidateOpen(true)}>Valider la campagne</Button>
          )}
          {campaign.status === 'VALIDE' && hasPermission('INVENTAIRE_VALIDATE') && (
            <Button variant="destructive" onClick={() => setCloseOpen(true)}>
              Clôturer la campagne
            </Button>
          )}
        </div>
      </div>

      {progress && (
        <Card>
          <CardContent className="grid grid-cols-2 gap-4 pt-6 sm:grid-cols-5">
            <Stat label="Périmètre attendu" value={progress.totalAssetsInScope} />
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
              <p className="mt-1 text-xs text-muted-foreground">{progress.progressPercent}% du périmètre scanné</p>
            </div>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue={campaign.status === 'EN_COURS' ? 'scanner' : 'anomalies'}>
        <TabsList>
          <TabsTrigger value="scanner">Scanner</TabsTrigger>
          <TabsTrigger value="anomalies">Anomalies</TabsTrigger>
          <TabsTrigger value="historique">Historique des scans</TabsTrigger>
          <TabsTrigger value="restantes">Restantes</TabsTrigger>
        </TabsList>

        <TabsContent value="scanner">
          <ScanPanel campaignId={campaign.id} campaignStatus={campaign.status} />
        </TabsContent>

        <TabsContent value="anomalies">
          <AnomaliesPanel campaignId={campaign.id} />
        </TabsContent>

        <TabsContent value="historique">
          <ScanHistoryPanel campaignId={campaign.id} />
        </TabsContent>

        <TabsContent value="restantes">
          <PendingAssetsPanel campaignId={campaign.id} />
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={validateOpen}
        onOpenChange={setValidateOpen}
        title="Valider la campagne"
        description="Une fois validée, la campagne n'accepte plus de scans. Confirmer la validation ?"
        confirmLabel="Valider"
        isConfirming={validateMutation.isPending}
        onConfirm={() => validateMutation.mutate()}
      />
      <ConfirmDialog
        open={closeOpen}
        onOpenChange={setCloseOpen}
        title="Clôturer la campagne"
        description="Une campagne clôturée est définitivement figée. Confirmer la clôture ?"
        confirmLabel="Clôturer"
        destructive
        isConfirming={closeMutation.isPending}
        onConfirm={() => closeMutation.mutate()}
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

function ScanPanel({ campaignId, campaignStatus }: { campaignId: string; campaignStatus: CampaignStatus }) {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)

  const [assetCode, setAssetCode] = useState('')
  const [result, setResult] = useState<ScanResult>('PRESENT')
  const [anomalyType, setAnomalyType] = useState<AnomalyType>('AUTRE')
  const [comment, setComment] = useState('')

  const scanMutation = useMutation({
    mutationFn: () =>
      inventoryCampaignsApi.scan(campaignId, {
        assetCode: assetCode.trim(),
        result,
        anomalyType: result === 'ANOMALIE' ? anomalyType : undefined,
        comment: comment.trim() || undefined,
      }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['inventory-campaigns', campaignId] })
      if (!response.assetRecognized) {
        toast.error(`Code non reconnu : ${assetCode.trim()} — anomalie enregistrée`)
      } else if (response.anomaly) {
        toast.warning(`${response.scan?.assetCode} — anomalie enregistrée (${ANOMALY_TYPE_LABEL[response.anomaly.anomalyType]})`)
      } else {
        toast.success(`${response.scan?.assetCode} — présent`)
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
  if (campaignStatus !== 'EN_COURS') {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          Cette campagne n'accepte pas de scan dans son statut actuel ({CAMPAIGN_STATUS_LABEL[campaignStatus]}) — seule une
          campagne « En cours » peut être scannée.
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
          Un code qui ne correspond à aucune immobilisation connue, ou à une immobilisation hors du périmètre de cette
          campagne (site/zone), est automatiquement enregistré comme anomalie — vérification d'appartenance à la
          campagne, prompt maître Phase 7.
        </p>
      </CardContent>
    </Card>
  )
}

function AnomaliesPanel({ campaignId }: { campaignId: string }) {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const canValidate = hasPermission('INVENTAIRE_VALIDATE')

  const { data: anomalies, isLoading } = useQuery({
    queryKey: ['inventory-campaigns', campaignId, 'anomalies'],
    queryFn: () => inventoryCampaignsApi.anomalies(campaignId),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: 'EN_COURS' | 'RESOLUE' | 'REJETEE' }) =>
      inventoryAnomaliesApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-campaigns', campaignId, 'anomalies'] })
      toast.success('Statut mis à jour')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

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
            {canValidate && <TableHead className="w-40">Action</TableHead>}
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
                Aucune anomalie pour cette campagne.
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
              {canValidate && (
                <TableCell>
                  {anomaly.status !== 'RESOLUE' && anomaly.status !== 'REJETEE' && (
                    <div className="flex gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => statusMutation.mutate({ id: anomaly.id, status: 'RESOLUE' })}
                      >
                        Résoudre
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => statusMutation.mutate({ id: anomaly.id, status: 'REJETEE' })}
                      >
                        Rejeter
                      </Button>
                    </div>
                  )}
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function ScanHistoryPanel({ campaignId }: { campaignId: string }) {
  const { data: scans, isLoading } = useQuery({
    queryKey: ['inventory-campaigns', campaignId, 'scans'],
    queryFn: () => inventoryCampaignsApi.scans(campaignId),
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

function PendingAssetsPanel({ campaignId }: { campaignId: string }) {
  const { data: pending, isLoading } = useQuery({
    queryKey: ['inventory-campaigns', campaignId, 'pending-assets'],
    queryFn: () => inventoryCampaignsApi.pendingAssets(campaignId),
  })

  return (
    <div className="rounded-lg border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Code</TableHead>
            <TableHead>Désignation</TableHead>
            <TableHead>Localisation</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading && (
            <TableRow>
              <TableCell colSpan={3} className="text-center text-muted-foreground">
                Chargement...
              </TableCell>
            </TableRow>
          )}
          {!isLoading && pending?.length === 0 && (
            <TableRow>
              <TableCell colSpan={3} className="text-center text-muted-foreground">
                Toutes les immobilisations du périmètre ont été scannées.
              </TableCell>
            </TableRow>
          )}
          {pending?.map((asset) => (
            <TableRow key={asset.id}>
              <TableCell className="font-mono text-xs">{asset.assetCode}</TableCell>
              <TableCell className="font-medium">{asset.designation}</TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {[asset.buildingName, asset.floorName, asset.zoneName, asset.locationName].filter(Boolean).join(' / ') || '—'}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
