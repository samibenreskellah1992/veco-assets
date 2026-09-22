import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { movementsApi } from '@/services/movement-service'
import { assetsApi } from '@/services/asset-service'
import { sitesApi, buildingsApi, floorsApi, zonesApi, locationsApi } from '@/services/referentiel-service'
import { usersApi } from '@/services/user-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import {
  MOVEMENT_STATUS_LABEL,
  MOVEMENT_TYPE_LABEL,
  MOVEMENT_TYPES_REQUIRING_LOCATION,
  MOVEMENT_TYPES_REQUIRING_SERVICE,
  MOVEMENT_TYPES_REQUIRING_SITE,
  MOVEMENT_TYPES_REQUIRING_USER,
  type MovementPrefill,
  type MovementStatus,
  type MovementType,
} from '@/types/movement'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const ALL = '__all__'
const NONE = '__none__'

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('fr-FR')
}

const STATUS_BADGE_VARIANT: Record<MovementStatus, 'default' | 'secondary' | 'success' | 'destructive'> = {
  DEMANDE: 'secondary',
  VALIDE: 'default',
  EXECUTE: 'success',
  REJETE: 'destructive',
}

const movementSchema = z
  .object({
    assetId: z.string().min(1, 'Immobilisation requise'),
    movementType: z.string().min(1, 'Type de mouvement requis'),
    toUserId: z.string(),
    toSiteId: z.string(),
    toBuildingId: z.string(),
    toFloorId: z.string(),
    toZoneId: z.string(),
    toLocationId: z.string(),
    toDirection: z.string().optional().or(z.literal('')),
    toDepartment: z.string().optional().or(z.literal('')),
    toService: z.string().optional().or(z.literal('')),
    reason: z.string().optional().or(z.literal('')),
    comment: z.string().optional().or(z.literal('')),
  })
  .superRefine((values, ctx) => {
    const type = values.movementType as MovementType
    if (MOVEMENT_TYPES_REQUIRING_USER.includes(type) && (!values.toUserId || values.toUserId === NONE)) {
      ctx.addIssue({ code: 'custom', path: ['toUserId'], message: 'Utilisateur destinataire requis pour ce type de mouvement' })
    }
    if (MOVEMENT_TYPES_REQUIRING_SERVICE.includes(type)) {
      const hasAny = [values.toDirection, values.toDepartment, values.toService].some((v) => v && v.trim() !== '')
      if (!hasAny) {
        ctx.addIssue({ code: 'custom', path: ['toService'], message: 'Au moins une nouvelle direction, département ou service requis' })
      }
    }
    if (MOVEMENT_TYPES_REQUIRING_LOCATION.includes(type) && (!values.toLocationId || values.toLocationId === NONE)) {
      ctx.addIssue({ code: 'custom', path: ['toLocationId'], message: 'Local de destination requis pour ce type de mouvement' })
    }
    if (MOVEMENT_TYPES_REQUIRING_SITE.includes(type) && (!values.toSiteId || values.toSiteId === NONE)) {
      ctx.addIssue({ code: 'custom', path: ['toSiteId'], message: 'Site de destination requis pour un transfert inter-site' })
    }
  })
type MovementFormValues = z.infer<typeof movementSchema>

const EMPTY_VALUES: MovementFormValues = {
  assetId: '',
  movementType: '',
  toUserId: NONE,
  toSiteId: NONE,
  toBuildingId: NONE,
  toFloorId: NONE,
  toZoneId: NONE,
  toLocationId: NONE,
  toDirection: '',
  toDepartment: '',
  toService: '',
  reason: '',
  comment: '',
}

export function MouvementsPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('IMMOBILISATION_VIEW')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La consultation des mouvements est réservée aux comptes disposant de la permission IMMOBILISATION_VIEW.
        </CardContent>
      </Card>
    )
  }

  return <MouvementsContent />
}

function MouvementsContent() {
  const { hasPermission } = useAuth()
  const canCreate = hasPermission('MOUVEMENT_CREATE')
  const canValidate = hasPermission('MOUVEMENT_VALIDATE')
  const queryClient = useQueryClient()
  const routerLocation = useLocation()
  const navigate = useNavigate()
  const prefillHandled = useRef(false)

  const [statusFilter, setStatusFilter] = useState(ALL)
  const [typeFilter, setTypeFilter] = useState(ALL)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [assetSearch, setAssetSearch] = useState('')
  const [rejectTarget, setRejectTarget] = useState<string | null>(null)
  const [rejectComment, setRejectComment] = useState('')
  const [confirmAction, setConfirmAction] = useState<{ id: string; kind: 'validate' | 'execute' } | null>(null)

  const { data: movements, isLoading } = useQuery({
    queryKey: ['movements', statusFilter, typeFilter],
    queryFn: () =>
      movementsApi.list({
        status: statusFilter === ALL ? undefined : (statusFilter as MovementStatus),
        type: typeFilter === ALL ? undefined : (typeFilter as MovementType),
      }),
  })

  const { data: assetOptions } = useQuery({
    queryKey: ['assets', 'movement-picker', assetSearch],
    queryFn: () => assetsApi.list({ page: 0, size: 20, sort: 'designation,asc', search: assetSearch || undefined }),
    enabled: dialogOpen,
  })
  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list, enabled: dialogOpen })
  const { data: users } = useQuery({ queryKey: ['users'], queryFn: usersApi.list, enabled: dialogOpen })

  const form = useForm<MovementFormValues>({ resolver: zodResolver(movementSchema), defaultValues: EMPTY_VALUES })
  const assetId = form.watch('assetId')
  const movementType = form.watch('movementType') as MovementType | ''
  const toSiteId = form.watch('toSiteId')
  const toBuildingId = form.watch('toBuildingId')
  const toFloorId = form.watch('toFloorId')
  const toZoneId = form.watch('toZoneId')

  const needsUser = movementType && MOVEMENT_TYPES_REQUIRING_USER.includes(movementType)
  const needsService = movementType && MOVEMENT_TYPES_REQUIRING_SERVICE.includes(movementType)
  const needsLocation = movementType && MOVEMENT_TYPES_REQUIRING_LOCATION.includes(movementType)
  const needsSite = movementType && MOVEMENT_TYPES_REQUIRING_SITE.includes(movementType)

  const { data: selectedAsset } = useQuery({
    queryKey: ['assets', assetId],
    queryFn: () => assetsApi.get(assetId),
    enabled: Boolean(assetId),
  })

  // Le site de reference de la cascade batiment/etage/zone/local : le site
  // actuel de l'immobilisation pour un changement de localisation (fige,
  // jamais editable - un changement de site passe par un transfert
  // inter-site), le site choisi par l'utilisateur pour un transfert.
  const cascadeSiteId = needsLocation ? selectedAsset?.siteId : needsSite ? (toSiteId === NONE ? undefined : toSiteId) : undefined

  const { data: buildings } = useQuery({
    queryKey: ['buildings', cascadeSiteId],
    queryFn: () => buildingsApi.list(cascadeSiteId),
    enabled: Boolean(cascadeSiteId),
  })
  const { data: floors } = useQuery({
    queryKey: ['floors', toBuildingId],
    queryFn: () => floorsApi.list(toBuildingId),
    enabled: toBuildingId !== NONE && Boolean(toBuildingId),
  })
  const { data: zones } = useQuery({
    queryKey: ['zones', toFloorId],
    queryFn: () => zonesApi.list(toFloorId),
    enabled: toFloorId !== NONE && Boolean(toFloorId),
  })
  const { data: locations } = useQuery({
    queryKey: ['locations', toZoneId],
    queryFn: () => locationsApi.list(toZoneId),
    enabled: toZoneId !== NONE && Boolean(toZoneId),
  })

  // Reinitialise les champs cible quand le type de mouvement change, pour ne
  // jamais envoyer au backend un champ cible qui appartenait a un type
  // precedemment selectionne dans le meme formulaire. Desactive une fois
  // (suppressToFieldsReset) juste apres un pre-remplissage (Checkpoint 3
  // "locaux scannables", voir plus bas) : ce changement de movementType-la
  // arrive AVEC des champs cible deja corrects, qu'il ne faut pas effacer.
  const suppressToFieldsReset = useRef(false)
  useEffect(() => {
    if (suppressToFieldsReset.current) {
      suppressToFieldsReset.current = false
      return
    }
    form.setValue('toUserId', NONE)
    form.setValue('toSiteId', NONE)
    form.setValue('toBuildingId', NONE)
    form.setValue('toFloorId', NONE)
    form.setValue('toZoneId', NONE)
    form.setValue('toLocationId', NONE)
    form.setValue('toDirection', '')
    form.setValue('toDepartment', '')
    form.setValue('toService', '')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [movementType])

  const closeDialog = () => {
    setDialogOpen(false)
    setAssetSearch('')
    form.reset(EMPTY_VALUES)
  }

  // Checkpoint 3 "locaux scannables" (2026-09) : ouverture du dialogue
  // pre-rempli quand on arrive depuis la page d'une session de scan de
  // local (bouton "Proposer un changement de localisation" sur une
  // anomalie MAUVAISE_LOCALISATION) - jamais de creation automatique du
  // mouvement, seulement le formulaire pre-rempli, l'utilisateur garde la
  // main pour verifier et confirmer (BR-LOC-006).
  useEffect(() => {
    if (prefillHandled.current) return
    const state = routerLocation.state as { prefill?: MovementPrefill } | null
    if (state?.prefill) {
      prefillHandled.current = true
      suppressToFieldsReset.current = true
      const prefill = state.prefill
      form.reset({
        ...EMPTY_VALUES,
        assetId: prefill.assetId,
        movementType: prefill.movementType,
        toSiteId: prefill.toSiteId ?? NONE,
        toBuildingId: prefill.toBuildingId ?? NONE,
        toFloorId: prefill.toFloorId ?? NONE,
        toZoneId: prefill.toZoneId ?? NONE,
        toLocationId: prefill.toLocationId ?? NONE,
        reason: prefill.reason ?? '',
      })
      setDialogOpen(true)
      navigate(routerLocation.pathname, { replace: true, state: null })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const requestMutation = useMutation({
    mutationFn: (values: MovementFormValues) =>
      movementsApi.request({
        assetId: values.assetId,
        movementType: values.movementType as MovementType,
        toUserId: needsUser && values.toUserId !== NONE ? values.toUserId : undefined,
        toSiteId: needsSite && values.toSiteId !== NONE ? values.toSiteId : undefined,
        toLocationId: (needsLocation || needsSite) && values.toLocationId !== NONE ? values.toLocationId : undefined,
        toDirection: values.toDirection || undefined,
        toDepartment: values.toDepartment || undefined,
        toService: values.toService || undefined,
        reason: values.reason || undefined,
        comment: values.comment || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['movements'] })
      toast.success('Mouvement demandé')
      closeDialog()
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const validateMutation = useMutation({
    mutationFn: (id: string) => movementsApi.validate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['movements'] })
      toast.success('Mouvement validé')
      setConfirmAction(null)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const executeMutation = useMutation({
    mutationFn: (id: string) => movementsApi.execute(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['movements'] })
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      toast.success('Mouvement exécuté — fiche immobilisation mise à jour')
      setConfirmAction(null)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, comment }: { id: string; comment: string }) => movementsApi.reject(id, { comment: comment || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['movements'] })
      toast.success('Mouvement rejeté')
      setRejectTarget(null)
      setRejectComment('')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Mouvements</h1>
          <p className="text-sm text-muted-foreground">
            Affectations, transferts, changements de localisation/service/utilisateur, maintenance, sorties et réformes
            (prompt maître Phase 8) : Demande → Validation → Exécution → Historisation.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" /> Nouveau mouvement
          </Button>
        )}
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
                {Object.entries(MOVEMENT_STATUS_LABEL).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="w-64">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Type</label>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous les types</SelectItem>
                {Object.entries(MOVEMENT_TYPE_LABEL).map(([value, label]) => (
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
              <TableHead>Immobilisation</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Demandé par</TableHead>
              <TableHead>Date de demande</TableHead>
              <TableHead>Statut</TableHead>
              <TableHead className="w-64">Actions</TableHead>
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
            {!isLoading && movements?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Aucun mouvement ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {movements?.map((movement) => (
              <TableRow key={movement.id}>
                <TableCell>
                  <div className="font-mono text-xs">{movement.assetCode}</div>
                  <div className="text-sm text-muted-foreground">{movement.assetDesignation}</div>
                </TableCell>
                <TableCell>{MOVEMENT_TYPE_LABEL[movement.movementType]}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{movement.requestedByName ?? '—'}</TableCell>
                <TableCell>{formatDateTime(movement.requestedAt)}</TableCell>
                <TableCell>
                  <Badge variant={STATUS_BADGE_VARIANT[movement.status]}>{MOVEMENT_STATUS_LABEL[movement.status]}</Badge>
                </TableCell>
                <TableCell>
                  {canValidate && movement.status === 'DEMANDE' && (
                    <div className="flex flex-wrap gap-1">
                      <Button size="sm" variant="outline" onClick={() => setConfirmAction({ id: movement.id, kind: 'validate' })}>
                        Valider
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => setRejectTarget(movement.id)}>
                        Rejeter
                      </Button>
                    </div>
                  )}
                  {canValidate && movement.status === 'VALIDE' && (
                    <Button size="sm" onClick={() => setConfirmAction({ id: movement.id, kind: 'execute' })}>
                      Exécuter
                    </Button>
                  )}
                  {(movement.status === 'EXECUTE' || movement.status === 'REJETE') && (
                    <span className="text-xs text-muted-foreground">
                      {movement.status === 'EXECUTE' ? `Exécuté le ${formatDateTime(movement.executedAt)}` : (movement.comment ?? '—')}
                    </span>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={(open) => (open ? setDialogOpen(true) : closeDialog())}>
        <DialogContent className="max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Nouveau mouvement</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit((values) => requestMutation.mutate(values))} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Immobilisation</label>
                <Input placeholder="Rechercher par code ou désignation..." value={assetSearch} onChange={(e) => setAssetSearch(e.target.value)} />
                <FormField
                  control={form.control}
                  name="assetId"
                  render={({ field }) => (
                    <FormItem>
                      <Select value={field.value} onValueChange={field.onChange}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Sélectionner une immobilisation..." />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {assetOptions?.content.map((asset) => (
                            <SelectItem key={asset.id} value={asset.id}>
                              {asset.assetCode} — {asset.designation}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                {selectedAsset && (
                  <p className="text-xs text-muted-foreground">
                    Actuellement : {selectedAsset.siteName} — {selectedAsset.locationName ?? 'sans local précis'}
                    {selectedAsset.currentUserName ? ` — affectée à ${selectedAsset.currentUserName}` : ' — non affectée'}
                  </p>
                )}
              </div>

              <FormField
                control={form.control}
                name="movementType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Type de mouvement</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {Object.entries(MOVEMENT_TYPE_LABEL).map(([value, label]) => (
                          <SelectItem key={value} value={value}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {needsUser && (
                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="toUserId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Utilisateur destinataire</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value={NONE}>— Sélectionner —</SelectItem>
                            {users?.map((user) => (
                              <SelectItem key={user.id} value={user.id}>
                                {user.firstName} {user.lastName}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              )}

              {(needsUser || needsService) && (
                <div className="grid grid-cols-3 gap-4">
                  <FormField
                    control={form.control}
                    name="toDirection"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Nouvelle direction</FormLabel>
                        <FormControl>
                          <Input {...field} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="toDepartment"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Nouveau département</FormLabel>
                        <FormControl>
                          <Input {...field} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="toService"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Nouveau service</FormLabel>
                        <FormControl>
                          <Input {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              )}

              {needsSite && (
                <FormField
                  control={form.control}
                  name="toSiteId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Site de destination</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={(value) => {
                          field.onChange(value)
                          form.setValue('toBuildingId', NONE)
                          form.setValue('toFloorId', NONE)
                          form.setValue('toZoneId', NONE)
                          form.setValue('toLocationId', NONE)
                        }}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Sélectionner..." />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {sites?.filter((site) => site.id !== selectedAsset?.siteId).map((site) => (
                            <SelectItem key={site.id} value={site.id}>
                              {site.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              {(needsLocation || (needsSite && toSiteId !== NONE)) && (
                <div className="grid grid-cols-3 gap-4">
                  <FormField
                    control={form.control}
                    name="toBuildingId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Bâtiment</FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={(value) => {
                            field.onChange(value)
                            form.setValue('toFloorId', NONE)
                            form.setValue('toZoneId', NONE)
                            form.setValue('toLocationId', NONE)
                          }}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value={NONE}>— Aucun —</SelectItem>
                            {buildings?.map((building) => (
                              <SelectItem key={building.id} value={building.id}>
                                {building.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="toFloorId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Étage</FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={(value) => {
                            field.onChange(value)
                            form.setValue('toZoneId', NONE)
                            form.setValue('toLocationId', NONE)
                          }}
                          disabled={toBuildingId === NONE || !toBuildingId}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value={NONE}>— Aucun —</SelectItem>
                            {floors?.map((floor) => (
                              <SelectItem key={floor.id} value={floor.id}>
                                {floor.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="toZoneId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Zone</FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={(value) => {
                            field.onChange(value)
                            form.setValue('toLocationId', NONE)
                          }}
                          disabled={toFloorId === NONE || !toFloorId}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value={NONE}>— Aucune —</SelectItem>
                            {zones?.map((zone) => (
                              <SelectItem key={zone.id} value={zone.id}>
                                {zone.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="toLocationId"
                    render={({ field }) => (
                      <FormItem className="col-span-3">
                        <FormLabel>Local {needsLocation ? 'de destination' : '(optionnel)'}</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange} disabled={toZoneId === NONE || !toZoneId}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value={NONE}>— Aucun —</SelectItem>
                            {locations?.map((location) => (
                              <SelectItem key={location.id} value={location.id}>
                                {location.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              )}

              <FormField
                control={form.control}
                name="reason"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Motif</FormLabel>
                    <FormControl>
                      <Textarea rows={2} {...field} />
                    </FormControl>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="comment"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Commentaire</FormLabel>
                    <FormControl>
                      <Textarea rows={2} {...field} />
                    </FormControl>
                  </FormItem>
                )}
              />

              <DialogFooter>
                <Button type="button" variant="outline" onClick={closeDialog}>
                  Annuler
                </Button>
                <Button type="submit" disabled={requestMutation.isPending}>
                  {requestMutation.isPending ? 'Envoi...' : 'Demander le mouvement'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(rejectTarget)} onOpenChange={(open) => !open && setRejectTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Rejeter ce mouvement ?</DialogTitle>
          </DialogHeader>
          <Textarea placeholder="Motif du rejet (optionnel)" value={rejectComment} onChange={(e) => setRejectComment(e.target.value)} rows={3} />
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectTarget(null)} disabled={rejectMutation.isPending}>
              Annuler
            </Button>
            <Button
              variant="destructive"
              disabled={rejectMutation.isPending}
              onClick={() => rejectTarget && rejectMutation.mutate({ id: rejectTarget, comment: rejectComment })}
            >
              {rejectMutation.isPending ? 'En cours...' : 'Rejeter'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={confirmAction?.kind === 'validate'}
        onOpenChange={(open) => !open && setConfirmAction(null)}
        title="Valider ce mouvement ?"
        description="Le mouvement passera au statut Validé. Il devra ensuite être exécuté pour que la fiche immobilisation soit réellement mise à jour."
        confirmLabel="Valider"
        isConfirming={validateMutation.isPending}
        onConfirm={() => confirmAction && validateMutation.mutate(confirmAction.id)}
      />
      <ConfirmDialog
        open={confirmAction?.kind === 'execute'}
        onOpenChange={(open) => !open && setConfirmAction(null)}
        title="Exécuter ce mouvement ?"
        description="L'immobilisation sera mise à jour immédiatement (localisation, affectation ou statut selon le type de mouvement) et l'historique sera conservé."
        confirmLabel="Exécuter"
        isConfirming={executeMutation.isPending}
        onConfirm={() => confirmAction && executeMutation.mutate(confirmAction.id)}
      />
    </div>
  )
}
