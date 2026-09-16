import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Camera, Image as ImageIcon } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { attachmentsApi, inventoryAnomaliesApi } from '@/services/inventory-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import { ANOMALY_STATUS_LABEL, ANOMALY_TYPE_LABEL, type AnomalyStatus, type InventoryAnomalyDto } from '@/types/inventory'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const ALL = '__all__'

const STATUS_BADGE_VARIANT: Record<AnomalyStatus, 'default' | 'secondary' | 'success' | 'destructive'> = {
  NOUVELLE: 'destructive',
  EN_COURS: 'default',
  RESOLUE: 'success',
  REJETEE: 'secondary',
}

export function AnomaliesPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('INVENTAIRE_VIEW')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La consultation des anomalies est réservée aux comptes disposant de la permission INVENTAIRE_VIEW.
        </CardContent>
      </Card>
    )
  }

  return <AnomaliesContent />
}

function AnomaliesContent() {
  const { hasPermission } = useAuth()
  const canValidate = hasPermission('INVENTAIRE_VALIDATE')
  const queryClient = useQueryClient()

  const [statusFilter, setStatusFilter] = useState(ALL)
  const [photosAnomaly, setPhotosAnomaly] = useState<InventoryAnomalyDto | null>(null)

  const { data: anomalies, isLoading } = useQuery({
    queryKey: ['inventory-anomalies'],
    queryFn: inventoryAnomaliesApi.listAll,
  })

  const filtered = anomalies?.filter((anomaly) => statusFilter === ALL || anomaly.status === statusFilter)

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: AnomalyStatus }) => inventoryAnomaliesApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-anomalies'] })
      toast.success('Statut mis à jour')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Anomalies</h1>
        <p className="text-sm text-muted-foreground">
          Anomalies détectées pendant les campagnes d'inventaire (prompt maître Phase 7), toutes campagnes confondues.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="w-56">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Statut</label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous les statuts</SelectItem>
                {Object.entries(ANOMALY_STATUS_LABEL).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Campagne</TableHead>
              <TableHead>Immobilisation</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Signalée par</TableHead>
              <TableHead>Statut</TableHead>
              <TableHead className="w-56">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Chargement...
                </TableCell>
              </TableRow>
            )}
            {!isLoading && filtered?.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Aucune anomalie ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {filtered?.map((anomaly) => (
              <TableRow key={anomaly.id}>
                <TableCell className="text-sm">{anomaly.campaignName ?? '—'}</TableCell>
                <TableCell className="font-mono text-xs">{anomaly.assetCode ?? '— (non référencée)'}</TableCell>
                <TableCell>{ANOMALY_TYPE_LABEL[anomaly.anomalyType]}</TableCell>
                <TableCell className="max-w-xs truncate text-sm text-muted-foreground">{anomaly.description ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{anomaly.reportedByName ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_BADGE_VARIANT[anomaly.status]}>{ANOMALY_STATUS_LABEL[anomaly.status]}</Badge>
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    <Button size="sm" variant="outline" onClick={() => setPhotosAnomaly(anomaly)}>
                      <ImageIcon className="h-3.5 w-3.5" /> Photos
                    </Button>
                    {canValidate && anomaly.status !== 'RESOLUE' && anomaly.status !== 'REJETEE' && (
                      <>
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
                      </>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <PhotosDialog anomaly={photosAnomaly} onOpenChange={(open) => !open && setPhotosAnomaly(null)} />
    </div>
  )
}

function PhotosDialog({ anomaly, onOpenChange }: { anomaly: InventoryAnomalyDto | null; onOpenChange: (open: boolean) => void }) {
  const { hasPermission } = useAuth()
  const canUpload = hasPermission('INVENTAIRE_EXECUTE')
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [previews, setPreviews] = useState<Record<string, string>>({})
  const requestedRef = useRef<Set<string>>(new Set())

  const { data: photos } = useQuery({
    queryKey: ['inventory-anomalies', anomaly?.id, 'photos'],
    queryFn: () => inventoryAnomaliesApi.photos(anomaly!.id),
    enabled: Boolean(anomaly),
  })

  useEffect(() => {
    if (!photos) return
    let cancelled = false
    for (const photo of photos) {
      if (requestedRef.current.has(photo.id)) continue
      requestedRef.current.add(photo.id)
      attachmentsApi.fetchBlob(photo.id).then((blob) => {
        if (cancelled) return
        setPreviews((current) => ({ ...current, [photo.id]: URL.createObjectURL(blob) }))
      })
    }
    return () => {
      cancelled = true
    }
  }, [photos])

  const uploadMutation = useMutation({
    mutationFn: (file: File) => inventoryAnomaliesApi.attachPhoto(anomaly!.id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-anomalies', anomaly?.id, 'photos'] })
      toast.success('Photo ajoutée')
      if (fileInputRef.current) fileInputRef.current.value = ''
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  return (
    <Dialog open={Boolean(anomaly)} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Photos — {anomaly?.assetCode ?? 'anomalie non référencée'}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          {photos?.length === 0 && <p className="text-sm text-muted-foreground">Aucune photo pour cette anomalie.</p>}
          <div className="grid grid-cols-3 gap-2">
            {photos?.map((photo) => (
              <a
                key={photo.id}
                href={previews[photo.id]}
                target="_blank"
                rel="noreferrer"
                className="flex aspect-square items-center justify-center overflow-hidden rounded-md border border-border bg-muted"
              >
                {previews[photo.id] ? (
                  <img src={previews[photo.id]} alt={photo.fileName} className="h-full w-full object-cover" />
                ) : (
                  <ImageIcon className="h-6 w-6 text-muted-foreground" />
                )}
              </a>
            ))}
          </div>
          {canUpload && (
            <div>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  if (file) uploadMutation.mutate(file)
                }}
              />
              <Button
                type="button"
                variant="outline"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadMutation.isPending}
              >
                <Camera className="h-4 w-4" /> {uploadMutation.isPending ? 'Envoi...' : 'Ajouter une photo'}
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
