import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { assetsApi } from '@/services/asset-service'
import { usersApi } from '@/services/user-service'
import { sitesApi, buildingsApi, floorsApi, zonesApi, locationsApi, assetCategoriesApi } from '@/services/referentiel-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import { ASSET_CONDITION_LABEL, ASSET_STATUS_LABEL, type AssetCondition, type AssetStatus } from '@/types/asset'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const NONE = '__none__'

const assetSchema = z.object({
  designation: z.string().min(1, 'Désignation requise').max(255),
  categoryId: z.string().min(1, 'Catégorie requise'),
  brand: z.string().max(100).optional().or(z.literal('')),
  model: z.string().max(100).optional().or(z.literal('')),
  serialNumber: z.string().max(150).optional().or(z.literal('')),
  siteId: z.string().min(1, 'Site requis'),
  buildingId: z.string(),
  floorId: z.string(),
  zoneId: z.string(),
  locationId: z.string(),
  direction: z.string().max(150).optional().or(z.literal('')),
  department: z.string().max(150).optional().or(z.literal('')),
  service: z.string().max(150).optional().or(z.literal('')),
  currentUserId: z.string(),
  responsibleUserId: z.string(),
  acquisitionDate: z.string().optional().or(z.literal('')),
  supplier: z.string().max(150).optional().or(z.literal('')),
  invoiceNumber: z.string().max(100).optional().or(z.literal('')),
  acquisitionValue: z.string().optional().or(z.literal('')),
  commissioningDate: z.string().optional().or(z.literal('')),
  warrantyUntil: z.string().optional().or(z.literal('')),
  condition: z.string(),
  status: z.string(),
  comment: z.string().optional().or(z.literal('')),
  changeComment: z.string().optional().or(z.literal('')),
})
type AssetFormValues = z.infer<typeof assetSchema>

const EMPTY_VALUES: AssetFormValues = {
  designation: '',
  categoryId: '',
  brand: '',
  model: '',
  serialNumber: '',
  siteId: '',
  buildingId: NONE,
  floorId: NONE,
  zoneId: NONE,
  locationId: NONE,
  direction: '',
  department: '',
  service: '',
  currentUserId: NONE,
  responsibleUserId: NONE,
  acquisitionDate: '',
  supplier: '',
  invoiceNumber: '',
  acquisitionValue: '',
  commissioningDate: '',
  warrantyUntil: '',
  condition: 'NEUF',
  status: 'EN_STOCK',
  comment: '',
  changeComment: '',
}

function undef(value: string) {
  return value === '' || value === NONE ? undefined : value
}

export function AssetFormPage() {
  const { id } = useParams<{ id: string }>()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: asset, isLoading: loadingAsset } = useQuery({
    queryKey: ['assets', id],
    queryFn: () => assetsApi.get(id!),
    enabled: editing,
  })

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: categories } = useQuery({ queryKey: ['asset-categories'], queryFn: assetCategoriesApi.list })
  const { data: users } = useQuery({ queryKey: ['users'], queryFn: usersApi.list })

  const form = useForm<AssetFormValues>({
    resolver: zodResolver(assetSchema),
    defaultValues: EMPTY_VALUES,
  })

  const siteId = form.watch('siteId')
  const buildingId = form.watch('buildingId')
  const floorId = form.watch('floorId')
  const zoneId = form.watch('zoneId')

  const { data: buildings } = useQuery({
    queryKey: ['buildings', siteId],
    queryFn: () => buildingsApi.list(siteId),
    enabled: Boolean(siteId),
  })
  const { data: floors } = useQuery({
    queryKey: ['floors', buildingId],
    queryFn: () => floorsApi.list(buildingId),
    enabled: buildingId !== NONE && Boolean(buildingId),
  })
  const { data: zones } = useQuery({
    queryKey: ['zones', floorId],
    queryFn: () => zonesApi.list(floorId),
    enabled: floorId !== NONE && Boolean(floorId),
  })
  const { data: locations } = useQuery({
    queryKey: ['locations', zoneId],
    queryFn: () => locationsApi.list(zoneId),
    enabled: zoneId !== NONE && Boolean(zoneId),
  })

  useEffect(() => {
    if (asset) {
      form.reset({
        designation: asset.designation,
        categoryId: asset.categoryId,
        brand: asset.brand ?? '',
        model: asset.model ?? '',
        serialNumber: asset.serialNumber ?? '',
        siteId: asset.siteId,
        buildingId: asset.buildingId ?? NONE,
        floorId: asset.floorId ?? NONE,
        zoneId: asset.zoneId ?? NONE,
        locationId: asset.locationId ?? NONE,
        direction: asset.direction ?? '',
        department: asset.department ?? '',
        service: asset.service ?? '',
        currentUserId: asset.currentUserId ?? NONE,
        responsibleUserId: asset.responsibleUserId ?? NONE,
        acquisitionDate: asset.acquisitionDate ?? '',
        supplier: asset.supplier ?? '',
        invoiceNumber: asset.invoiceNumber ?? '',
        acquisitionValue: asset.acquisitionValue != null ? String(asset.acquisitionValue) : '',
        commissioningDate: asset.commissioningDate ?? '',
        warrantyUntil: asset.warrantyUntil ?? '',
        condition: asset.condition,
        status: asset.status,
        comment: asset.comment ?? '',
        changeComment: '',
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asset])

  const saveMutation = useMutation({
    mutationFn: (values: AssetFormValues) => {
      const shared = {
        designation: values.designation.trim(),
        categoryId: values.categoryId,
        brand: undef(values.brand ?? ''),
        model: undef(values.model ?? ''),
        serialNumber: undef(values.serialNumber ?? ''),
        siteId: values.siteId,
        buildingId: undef(values.buildingId),
        floorId: undef(values.floorId),
        zoneId: undef(values.zoneId),
        locationId: undef(values.locationId),
        direction: undef(values.direction ?? ''),
        department: undef(values.department ?? ''),
        service: undef(values.service ?? ''),
        currentUserId: undef(values.currentUserId),
        responsibleUserId: undef(values.responsibleUserId),
        acquisitionDate: undef(values.acquisitionDate ?? ''),
        supplier: undef(values.supplier ?? ''),
        invoiceNumber: undef(values.invoiceNumber ?? ''),
        acquisitionValue: values.acquisitionValue ? Number(values.acquisitionValue) : undefined,
        commissioningDate: undef(values.commissioningDate ?? ''),
        warrantyUntil: undef(values.warrantyUntil ?? ''),
        comment: undef(values.comment ?? ''),
      }
      if (editing) {
        return assetsApi.update(id!, {
          ...shared,
          condition: values.condition as AssetCondition,
          status: values.status as AssetStatus,
          changeComment: undef(values.changeComment ?? ''),
        })
      }
      return assetsApi.create({ ...shared, condition: values.condition as AssetCondition })
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      toast.success(editing ? 'Immobilisation modifiée' : 'Immobilisation créée')
      navigate(`/immobilisations/${saved.id}`)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  if (editing && loadingAsset) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">
        {editing ? `Modifier — ${asset?.assetCode ?? ''}` : 'Nouvelle immobilisation'}
      </h1>

      <Form {...form}>
        <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Identification</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="designation"
                render={({ field }) => (
                  <FormItem className="col-span-2">
                    <FormLabel>Désignation</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="categoryId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Catégorie</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {categories?.map((category) => (
                          <SelectItem key={category.id} value={category.id}>
                            {category.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="serialNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Numéro de série</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="brand"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Marque</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="model"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Modèle</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Localisation</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <FormField
                control={form.control}
                name="siteId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Site</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(value) => {
                        field.onChange(value)
                        form.setValue('buildingId', NONE)
                        form.setValue('floorId', NONE)
                        form.setValue('zoneId', NONE)
                        form.setValue('locationId', NONE)
                      }}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {sites?.map((site) => (
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
              <FormField
                control={form.control}
                name="buildingId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Bâtiment</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(value) => {
                        field.onChange(value)
                        form.setValue('floorId', NONE)
                        form.setValue('zoneId', NONE)
                        form.setValue('locationId', NONE)
                      }}
                      disabled={!siteId}
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
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="floorId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Étage</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(value) => {
                        field.onChange(value)
                        form.setValue('zoneId', NONE)
                        form.setValue('locationId', NONE)
                      }}
                      disabled={buildingId === NONE || !buildingId}
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
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="zoneId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Zone</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(value) => {
                        field.onChange(value)
                        form.setValue('locationId', NONE)
                      }}
                      disabled={floorId === NONE || !floorId}
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
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="locationId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Local</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange} disabled={zoneId === NONE || !zoneId}>
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
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Affectation</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <FormField
                control={form.control}
                name="direction"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Direction</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="department"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Département</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="service"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Service</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="currentUserId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Utilisateur actuel</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value={NONE}>— Aucun —</SelectItem>
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
              <FormField
                control={form.control}
                name="responsibleUserId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Responsable</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value={NONE}>— Aucun —</SelectItem>
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
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Acquisition</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <FormField
                control={form.control}
                name="acquisitionDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Date d'acquisition</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="commissioningDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Date de mise en service</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="warrantyUntil"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Garantie jusqu'au</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="supplier"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Fournisseur</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="invoiceNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>N° de facture</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="acquisitionValue"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Valeur d'acquisition (DZD)</FormLabel>
                    <FormControl>
                      <Input type="number" min="0" step="0.01" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">État {editing && '/ Statut'}</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <FormField
                control={form.control}
                name="condition"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>État physique</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(Object.keys(ASSET_CONDITION_LABEL) as AssetCondition[]).map((value) => (
                          <SelectItem key={value} value={value}>
                            {ASSET_CONDITION_LABEL[value]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              {editing && (
                <FormField
                  control={form.control}
                  name="status"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Statut opérationnel</FormLabel>
                      <Select value={field.value} onValueChange={field.onChange}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {(Object.keys(ASSET_STATUS_LABEL) as AssetStatus[]).map((value) => (
                            <SelectItem key={value} value={value}>
                              {ASSET_STATUS_LABEL[value]}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
              {editing && (
                <FormField
                  control={form.control}
                  name="changeComment"
                  render={({ field }) => (
                    <FormItem className="md:col-span-1">
                      <FormLabel>Motif du changement (optionnel)</FormLabel>
                      <FormControl>
                        <Input placeholder="Consigné dans l'historique" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
              <FormField
                control={form.control}
                name="comment"
                render={({ field }) => (
                  <FormItem className="col-span-2 md:col-span-3">
                    <FormLabel>Commentaire</FormLabel>
                    <FormControl>
                      <Textarea rows={3} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Annuler
            </Button>
            <Button type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Enregistrement...' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  )
}
