import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Archive, Pencil } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { assetsApi } from '@/services/asset-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import { ASSET_CONDITION_LABEL, ASSET_STATUS_LABEL, type AssetCondition, type AssetStatus } from '@/types/asset'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ConfirmDialog } from '@/components/ConfirmDialog'

function conditionBadgeVariant(condition: AssetCondition) {
  if (condition === 'NEUF' || condition === 'BON') return 'success' as const
  if (condition === 'MOYEN') return 'secondary' as const
  return 'destructive' as const
}

function statusBadgeVariant(status: AssetStatus) {
  if (status === 'EN_SERVICE') return 'success' as const
  if (status === 'REFORME' || status === 'SORTI') return 'destructive' as const
  return 'secondary' as const
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('fr-FR')
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

function PlaceholderTab({ phase }: { phase: string }) {
  return (
    <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
      Module prévu en <span className="font-medium text-foreground">{phase}</span> (voir docs/ROADMAP.md).
      <br />
      Pas encore implémenté — cet onglet n'appelle aucune API tant que le module n'est pas construit.
    </div>
  )
}

export function AssetDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { hasPermission } = useAuth()
  const [archiveOpen, setArchiveOpen] = useState(false)

  const { data: asset, isLoading } = useQuery({ queryKey: ['assets', id], queryFn: () => assetsApi.get(id!) })
  const { data: assignments } = useQuery({ queryKey: ['assets', id, 'assignments'], queryFn: () => assetsApi.assignments(id!) })
  const { data: statusHistory } = useQuery({ queryKey: ['assets', id, 'status-history'], queryFn: () => assetsApi.statusHistory(id!) })

  const archiveMutation = useMutation({
    mutationFn: () => assetsApi.archive(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      toast.success('Immobilisation archivée')
      setArchiveOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  if (isLoading || !asset) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  const location = [asset.siteName, asset.buildingName, asset.floorName, asset.zoneName, asset.locationName]
    .filter(Boolean)
    .join(' / ')

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/immobilisations" className="mb-1 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-3.5 w-3.5" /> Retour à la liste
          </Link>
          <h1 className="text-2xl font-semibold tracking-tight">
            {asset.designation}
            {asset.deleted && (
              <Badge variant="destructive" className="ml-2 align-middle">
                Archivée
              </Badge>
            )}
          </h1>
          <p className="font-mono text-sm text-muted-foreground">{asset.assetCode}</p>
        </div>
        <div className="flex gap-2">
          {!asset.deleted && hasPermission('IMMOBILISATION_EDIT') && (
            <Button variant="outline" onClick={() => navigate(`/immobilisations/${asset.id}/modifier`)}>
              <Pencil className="h-4 w-4" /> Modifier
            </Button>
          )}
          {!asset.deleted && hasPermission('IMMOBILISATION_ARCHIVE') && (
            <Button variant="outline" onClick={() => setArchiveOpen(true)}>
              <Archive className="h-4 w-4" /> Archiver
            </Button>
          )}
        </div>
      </div>

      <Tabs defaultValue="resume">
        <TabsList>
          <TabsTrigger value="resume">Résumé</TabsTrigger>
          <TabsTrigger value="affectation">Affectation</TabsTrigger>
          <TabsTrigger value="historique">Historique</TabsTrigger>
          <TabsTrigger value="inventaire">Inventaire</TabsTrigger>
          <TabsTrigger value="mouvements">Mouvements</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="photos">Photos</TabsTrigger>
        </TabsList>

        <TabsContent value="resume" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Identification</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <Field label="Catégorie" value={asset.categoryName} />
              <Field label="Marque" value={asset.brand} />
              <Field label="Modèle" value={asset.model} />
              <Field label="Numéro de série" value={asset.serialNumber} />
              <Field
                label="État"
                value={<Badge variant={conditionBadgeVariant(asset.condition)}>{ASSET_CONDITION_LABEL[asset.condition]}</Badge>}
              />
              <Field
                label="Statut"
                value={<Badge variant={statusBadgeVariant(asset.status)}>{ASSET_STATUS_LABEL[asset.status]}</Badge>}
              />
              <Field label="Étiqueté" value={asset.labeled ? 'Oui' : 'Non'} />
              <Field label="Dernier inventaire" value={formatDateTime(asset.lastInventoryAt)} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Localisation & affectation</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <Field label="Localisation" value={location || '—'} />
              <Field label="Direction" value={asset.direction} />
              <Field label="Département" value={asset.department} />
              <Field label="Service" value={asset.service} />
              <Field label="Utilisateur actuel" value={asset.currentUserName} />
              <Field label="Responsable" value={asset.responsibleUserName} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Acquisition</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <Field label="Date d'acquisition" value={formatDate(asset.acquisitionDate)} />
              <Field label="Mise en service" value={formatDate(asset.commissioningDate)} />
              <Field label="Garantie jusqu'au" value={formatDate(asset.warrantyUntil)} />
              <Field label="Fournisseur" value={asset.supplier} />
              <Field label="N° de facture" value={asset.invoiceNumber} />
              <Field label="Valeur d'acquisition" value={asset.acquisitionValue != null ? `${asset.acquisitionValue} DZD` : null} />
            </CardContent>
          </Card>

          {asset.comment && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Commentaire</CardTitle>
              </CardHeader>
              <CardContent className="text-sm whitespace-pre-wrap">{asset.comment}</CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="affectation">
          <div className="rounded-lg border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Utilisateur</TableHead>
                  <TableHead>Direction / Département / Service</TableHead>
                  <TableHead>Depuis</TableHead>
                  <TableHead>Jusqu'à</TableHead>
                  <TableHead>Affecté par</TableHead>
                  <TableHead>Commentaire</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(!assignments || assignments.length === 0) && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground">
                      Aucune affectation enregistrée.
                    </TableCell>
                  </TableRow>
                )}
                {assignments?.map((assignment) => (
                  <TableRow key={assignment.id}>
                    <TableCell className="font-medium">{assignment.userName ?? '—'}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {[assignment.direction, assignment.department, assignment.service].filter(Boolean).join(' / ') || '—'}
                    </TableCell>
                    <TableCell>{formatDateTime(assignment.assignedFrom)}</TableCell>
                    <TableCell>
                      {assignment.assignedUntil ? formatDateTime(assignment.assignedUntil) : <Badge variant="success">En cours</Badge>}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">{assignment.assignedByName ?? '—'}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{assignment.comment ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </TabsContent>

        <TabsContent value="historique">
          <div className="rounded-lg border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Champ</TableHead>
                  <TableHead>Ancienne valeur</TableHead>
                  <TableHead>Nouvelle valeur</TableHead>
                  <TableHead>Modifié par</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Commentaire</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(!statusHistory || statusHistory.length === 0) && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground">
                      Aucun changement d'état ou de statut enregistré.
                    </TableCell>
                  </TableRow>
                )}
                {statusHistory?.map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell>
                      <Badge variant="outline">{entry.fieldName === 'CONDITION' ? 'État' : 'Statut'}</Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">{entry.oldValue ?? '—'}</TableCell>
                    <TableCell className="text-sm font-medium">{entry.newValue}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{entry.changedByName ?? '—'}</TableCell>
                    <TableCell>{formatDateTime(entry.changedAt)}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{entry.comment ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </TabsContent>

        <TabsContent value="inventaire">
          <PlaceholderTab phase="Phase 7 — Inventaire" />
        </TabsContent>
        <TabsContent value="mouvements">
          <PlaceholderTab phase="Phase 8 — Mouvements" />
        </TabsContent>
        <TabsContent value="documents">
          <PlaceholderTab phase="Phase 6 — Étiquetage / pièces jointes" />
        </TabsContent>
        <TabsContent value="photos">
          <PlaceholderTab phase="Phase 6 — Étiquetage / pièces jointes" />
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title="Archiver cette immobilisation ?"
        description="L'immobilisation sera marquée comme archivée (suppression logique) — elle reste consultable dans son historique mais disparaît des opérations courantes. Cette action ne supprime jamais physiquement la donnée."
        confirmLabel="Archiver"
        destructive
        isConfirming={archiveMutation.isPending}
        onConfirm={() => archiveMutation.mutate()}
      />
    </div>
  )
}
