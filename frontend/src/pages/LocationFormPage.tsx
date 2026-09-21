import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'

import { usersApi } from '@/services/user-service'
import { sitesApi, buildingsApi, floorsApi, zonesApi, locationsApi } from '@/services/referentiel-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import { LOCATION_STATUS_LABEL, type LocationDto, type LocationStatus } from '@/types/referentiel'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const NONE = '__none__'

const locationSchema = z.object({
  siteId: z.string().min(1, 'Site requis'),
  buildingId: z.string().min(1, 'Bâtiment requis'),
  floorId: z.string().min(1, 'Étage requis'),
  zoneId: z.string().min(1, 'Zone requise'),
  code: z.string().min(1, 'Code requis').max(30),
  name: z.string().min(1, 'Désignation requise').max(150),
  description: z.string().optional().or(z.literal('')),
  responsibleUserId: z.string(),
  status: z.string(),
})
type LocationFormValues = z.infer<typeof locationSchema>

const EMPTY_VALUES: LocationFormValues = {
  siteId: '',
  buildingId: '',
  floorId: '',
  zoneId: '',
  code: '',
  name: '',
  description: '',
  responsibleUserId: NONE,
  status: 'ACTIF',
}

function locationToFormValues(location: LocationDto): LocationFormValues {
  return {
    siteId: location.siteId ?? '',
    buildingId: location.buildingId ?? '',
    floorId: location.floorId ?? '',
    zoneId: location.zoneId,
    code: location.code,
    name: location.name,
    description: location.description ?? '',
    responsibleUserId: location.responsibleUserId ?? NONE,
    status: location.status,
  }
}

function undef(value: string) {
  return value === '' || value === NONE ? undefined : value
}

export function LocationFormPage() {
  const { id } = useParams<{ id: string }>()
  const editing = Boolean(id)

  const { data: location, isLoading: loadingLocation } = useQuery({
    queryKey: ['locations', id],
    queryFn: () => locationsApi.get(id!),
    enabled: editing,
  })

  // Même course évitée que sur AssetFormPage (voir son commentaire détaillé) :
  // useForm calcule ses defaultValues une seule fois, au tout premier rendu.
  // On ne monte donc le formulaire (LocationForm) qu'une fois le local à
  // modifier réellement chargé, plutôt que de le monter vide puis le
  // "rattraper" via reset()/l'option `values` — les deux cassent la
  // synchronisation des Select pilotés par Controller (Site/Bâtiment/Étage/
  // Zone/Responsable/Statut).
  if (editing && loadingLocation) {
    return <p className="text-sm text-muted-foreground">Chargement...</p>
  }

  return <LocationForm location={editing ? (location ?? null) : null} editing={editing} id={id} />
}

function LocationForm({ location, editing, id }: { location: LocationDto | null; editing: boolean; id?: string }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: users } = useQuery({ queryKey: ['users'], queryFn: usersApi.list })

  const form = useForm<LocationFormValues>({
    resolver: zodResolver(locationSchema),
    defaultValues: location ? locationToFormValues(location) : EMPTY_VALUES,
  })

  const siteId = form.watch('siteId')
  const buildingId = form.watch('buildingId')
  const floorId = form.watch('floorId')

  const { data: buildings } = useQuery({
    queryKey: ['buildings', siteId],
    queryFn: () => buildingsApi.list(siteId),
    enabled: Boolean(siteId),
  })
  const { data: floors } = useQuery({
    queryKey: ['floors', buildingId],
    queryFn: () => floorsApi.list(buildingId),
    enabled: Boolean(buildingId),
  })
  const { data: zones } = useQuery({
    queryKey: ['zones', floorId],
    queryFn: () => zonesApi.list(floorId),
    enabled: Boolean(floorId),
  })

  const saveMutation = useMutation({
    mutationFn: (values: LocationFormValues) => {
      const payload = {
        zoneId: values.zoneId,
        code: values.code.trim(),
        name: values.name.trim(),
        status: values.status as LocationStatus,
        description: undef(values.description ?? ''),
        responsibleUserId: undef(values.responsibleUserId),
      }
      return editing ? locationsApi.update(id!, payload) : locationsApi.create(payload)
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['locations'] })
      toast.success(editing ? 'Local modifié' : 'Local créé')
      navigate(`/locaux/${saved.id}`)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          {editing ? `Modifier — ${location?.name ?? ''}` : 'Nouveau local'}
        </h1>
        {editing && location && (
          <p className="mt-1 font-mono text-sm text-muted-foreground">Code QR : {location.qrCode} (non modifiable)</p>
        )}
        {!editing && (
          <p className="mt-1 text-sm text-muted-foreground">
            Le code QR du local sera généré automatiquement à l'enregistrement.
          </p>
        )}
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
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
                        form.setValue('buildingId', '')
                        form.setValue('floorId', '')
                        form.setValue('zoneId', '')
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
                        form.setValue('floorId', '')
                        form.setValue('zoneId', '')
                      }}
                      disabled={!siteId}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
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
                        form.setValue('zoneId', '')
                      }}
                      disabled={!buildingId}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
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
                    <Select value={field.value} onValueChange={field.onChange} disabled={!floorId}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sélectionner..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
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
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Code</FormLabel>
                    <FormControl>
                      <Input placeholder="ex. 204" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Désignation</FormLabel>
                    <FormControl>
                      <Input placeholder="ex. Bureau 204" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Détails</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Statut</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(Object.keys(LOCATION_STATUS_LABEL) as LocationStatus[]).map((value) => (
                          <SelectItem key={value} value={value}>
                            {LOCATION_STATUS_LABEL[value]}
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
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem className="col-span-2 md:col-span-3">
                    <FormLabel>Description</FormLabel>
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
