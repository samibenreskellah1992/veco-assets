import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { inventoryCampaignsApi } from '@/services/inventory-service'
import { sitesApi, buildingsApi, floorsApi, zonesApi } from '@/services/referentiel-service'
import { usersApi } from '@/services/user-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import { CAMPAIGN_STATUS_LABEL, type CampaignStatus } from '@/types/inventory'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'

const ALL = '__all__'
const NONE = '__none__'

const campaignSchema = z.object({
  name: z.string().min(1, 'Nom requis').max(150),
  siteId: z.string().min(1, 'Site requis'),
  buildingId: z.string(),
  floorId: z.string(),
  zoneId: z.string(),
  responsibleUserId: z.string(),
  startDate: z.string().min(1, 'Date de début requise'),
  endDate: z.string().min(1, 'Date de fin requise'),
})
type CampaignFormValues = z.infer<typeof campaignSchema>

const EMPTY_VALUES: CampaignFormValues = {
  name: '',
  siteId: '',
  buildingId: NONE,
  floorId: NONE,
  zoneId: NONE,
  responsibleUserId: NONE,
  startDate: '',
  endDate: '',
}

const STATUS_BADGE_VARIANT: Record<CampaignStatus, 'default' | 'secondary' | 'success' | 'destructive'> = {
  BROUILLON: 'secondary',
  EN_PREPARATION: 'secondary',
  EN_COURS: 'default',
  TERMINE: 'default',
  VALIDE: 'success',
  CLOTURE: 'secondary',
}

export function InventoryCampaignsPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('INVENTAIRE_VIEW')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La consultation des campagnes d'inventaire est réservée aux comptes disposant de la permission INVENTAIRE_VIEW.
        </CardContent>
      </Card>
    )
  }

  return <InventoryCampaignsContent />
}

function InventoryCampaignsContent() {
  const { hasPermission } = useAuth()
  const canCreate = hasPermission('INVENTAIRE_CREATE')
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [siteFilter, setSiteFilter] = useState(ALL)
  const [statusFilter, setStatusFilter] = useState(ALL)
  const [dialogOpen, setDialogOpen] = useState(false)

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: users } = useQuery({ queryKey: ['users'], queryFn: usersApi.list })

  const { data: campaigns, isLoading } = useQuery({
    queryKey: ['inventory-campaigns', siteFilter, statusFilter],
    queryFn: () =>
      inventoryCampaignsApi.list({
        siteId: siteFilter === ALL ? undefined : siteFilter,
        status: statusFilter === ALL ? undefined : (statusFilter as CampaignStatus),
      }),
  })

  const form = useForm<CampaignFormValues>({ resolver: zodResolver(campaignSchema), defaultValues: EMPTY_VALUES })
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
    enabled: buildingId !== NONE && Boolean(buildingId),
  })
  const { data: zones } = useQuery({
    queryKey: ['zones', floorId],
    queryFn: () => zonesApi.list(floorId),
    enabled: floorId !== NONE && Boolean(floorId),
  })

  const createMutation = useMutation({
    mutationFn: (values: CampaignFormValues) =>
      inventoryCampaignsApi.create({
        name: values.name.trim(),
        siteId: values.siteId,
        zoneId: values.zoneId === NONE ? undefined : values.zoneId,
        responsibleUserId: values.responsibleUserId === NONE ? undefined : values.responsibleUserId,
        startDate: values.startDate,
        endDate: values.endDate,
      }),
    onSuccess: (campaign) => {
      queryClient.invalidateQueries({ queryKey: ['inventory-campaigns'] })
      toast.success('Campagne créée')
      setDialogOpen(false)
      form.reset(EMPTY_VALUES)
      navigate(`/inventaires/${campaign.id}`)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Inventaires</h1>
          <p className="text-sm text-muted-foreground">
            Campagnes d'inventaire par scan (prompt maître Phase 7) : Brouillon → En préparation → En cours → Terminé →
            Validé → Clôturé.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" /> Nouvelle campagne
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="w-48">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Site</label>
            <Select value={siteFilter} onValueChange={setSiteFilter}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous les sites</SelectItem>
                {sites?.map((site) => (
                  <SelectItem key={site.id} value={site.id}>
                    {site.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="w-48">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Statut</label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous les statuts</SelectItem>
                {Object.entries(CAMPAIGN_STATUS_LABEL).map(([value, label]) => (
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
              <TableHead>Nom</TableHead>
              <TableHead>Site</TableHead>
              <TableHead>Zone</TableHead>
              <TableHead>Responsable</TableHead>
              <TableHead>Période</TableHead>
              <TableHead>Statut</TableHead>
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
            {!isLoading && campaigns?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Aucune campagne ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {campaigns?.map((campaign) => (
              <TableRow
                key={campaign.id}
                className="cursor-pointer"
                onClick={() => navigate(`/inventaires/${campaign.id}`)}
              >
                <TableCell className="font-medium">{campaign.name}</TableCell>
                <TableCell>{campaign.siteName ?? '—'}</TableCell>
                <TableCell>{campaign.zoneName ?? 'Tout le site'}</TableCell>
                <TableCell>{campaign.responsibleUserName ?? '—'}</TableCell>
                <TableCell>
                  {campaign.startDate} → {campaign.endDate}
                </TableCell>
                <TableCell>
                  <Badge variant={STATUS_BADGE_VARIANT[campaign.status]}>{CAMPAIGN_STATUS_LABEL[campaign.status]}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nouvelle campagne d'inventaire</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
              className="space-y-4"
            >
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nom</FormLabel>
                    <FormControl>
                      <Input placeholder="Inventaire annuel VSA - Bureaux" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <div className="grid grid-cols-2 gap-4">
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
              </div>
              <div className="grid grid-cols-3 gap-4">
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
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="zoneId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Zone</FormLabel>
                      <Select value={field.value} onValueChange={field.onChange} disabled={floorId === NONE || !floorId}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={NONE}>Tout le site</SelectItem>
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
              </div>
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="startDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Date de début</FormLabel>
                      <FormControl>
                        <Input type="date" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="endDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Date de fin</FormLabel>
                      <FormControl>
                        <Input type="date" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
              <DialogFooter>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Création...' : 'Créer la campagne'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
