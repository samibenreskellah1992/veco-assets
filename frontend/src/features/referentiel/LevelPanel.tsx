import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { extractApiErrorMessage } from '@/lib/api-error'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ConfirmDialog } from '@/components/ConfirmDialog'

export interface LevelItem {
  id: string
  code: string
  name: string
  active: boolean
}

export interface ParentOption {
  id: string
  name: string
  active: boolean
}

export interface LevelConfig<TDto extends LevelItem, TRequest extends { code: string; name: string }> {
  itemLabel: string
  itemLabelPlural: string
  parentLabel: string
  queryKey: string[]
  parentOptionsQueryKey: string[]
  fetchParentOptions: () => Promise<ParentOption[]>
  getParentId: (item: TDto) => string
  getParentName: (item: TDto) => string
  buildRequest: (values: { code: string; name: string; parentId: string }) => TRequest
  api: {
    list: (parentId?: string) => Promise<TDto[]>
    create: (request: TRequest) => Promise<TDto>
    update: (id: string, request: TRequest) => Promise<TDto>
    activate: (id: string) => Promise<TDto>
    deactivate: (id: string) => Promise<TDto>
    remove: (id: string) => Promise<unknown>
  }
}

const levelSchema = z.object({
  code: z.string().min(1, 'Code requis').max(30),
  name: z.string().min(1, 'Nom requis').max(150),
  parentId: z.string().min(1, 'Parent requis'),
})
type LevelFormValues = z.infer<typeof levelSchema>

/** Generic CRUD panel shared by Buildings/Floors/Zones/Locations - see docs/ARCHITECTURE.md Phase 4 (hierarchie de localisation, meme forme structurelle a chaque niveau). */
export function LevelPanel<TDto extends LevelItem, TRequest extends { code: string; name: string }>({
  config,
}: {
  config: LevelConfig<TDto, TRequest>
}) {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENTIEL_MANAGE')
  const queryClient = useQueryClient()

  const [parentFilter, setParentFilter] = useState<string>('all')
  const { data: parentOptions } = useQuery({
    queryKey: config.parentOptionsQueryKey,
    queryFn: config.fetchParentOptions,
  })
  const { data: items, isLoading } = useQuery({
    queryKey: [...config.queryKey, parentFilter],
    queryFn: () => config.api.list(parentFilter === 'all' ? undefined : parentFilter),
  })

  const [editing, setEditing] = useState<TDto | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleting, setDeleting] = useState<TDto | null>(null)

  const form = useForm<LevelFormValues>({
    resolver: zodResolver(levelSchema),
    defaultValues: { code: '', name: '', parentId: '' },
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: config.queryKey })
  }

  function openCreate() {
    setEditing(null)
    form.reset({ code: '', name: '', parentId: parentFilter !== 'all' ? parentFilter : '' })
    setDialogOpen(true)
  }

  function openEdit(item: TDto) {
    setEditing(item)
    form.reset({ code: item.code, name: item.name, parentId: config.getParentId(item) })
    setDialogOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: (values: LevelFormValues) => {
      const request = config.buildRequest(values)
      return editing ? config.api.update(editing.id, request) : config.api.create(request)
    },
    onSuccess: () => {
      invalidate()
      toast.success(editing ? `${config.itemLabel} modifié` : `${config.itemLabel} créé`)
      setDialogOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const toggleActiveMutation = useMutation({
    mutationFn: (item: TDto) => (item.active ? config.api.deactivate(item.id) : config.api.activate(item.id)),
    onSuccess: invalidate,
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => config.api.remove(id),
    onSuccess: () => {
      invalidate()
      toast.success(`${config.itemLabel} supprimé`)
      setDeleting(null)
    },
    onError: (error) => {
      toast.error(extractApiErrorMessage(error))
      setDeleting(null)
    },
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Filtrer par {config.parentLabel.toLowerCase()} :</span>
          <Select value={parentFilter} onValueChange={setParentFilter}>
            <SelectTrigger className="w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tous</SelectItem>
              {parentOptions?.map((option) => (
                <SelectItem key={option.id} value={option.id}>
                  {option.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        {canManage && (
          <Button size="sm" onClick={openCreate} disabled={!parentOptions?.length}>
            <Plus /> Nouveau {config.itemLabel}
          </Button>
        )}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Nom</TableHead>
              <TableHead>{config.parentLabel}</TableHead>
              <TableHead>Statut</TableHead>
              {canManage && <TableHead className="text-right">Actions</TableHead>}
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
            {!isLoading && items?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">
                  Aucun {config.itemLabel}.
                </TableCell>
              </TableRow>
            )}
            {items?.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-medium">{item.code}</TableCell>
                <TableCell>{item.name}</TableCell>
                <TableCell>{config.getParentName(item)}</TableCell>
                <TableCell>
                  {canManage ? (
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={item.active}
                        disabled={toggleActiveMutation.isPending}
                        onCheckedChange={() => toggleActiveMutation.mutate(item)}
                      />
                      <span className="text-xs text-muted-foreground">{item.active ? 'Actif' : 'Inactif'}</span>
                    </div>
                  ) : (
                    <Badge variant={item.active ? 'success' : 'secondary'}>{item.active ? 'Actif' : 'Inactif'}</Badge>
                  )}
                </TableCell>
                {canManage && (
                  <TableCell className="text-right">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(item)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => setDeleting(item)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editing ? `Modifier le ${config.itemLabel}` : `Nouveau ${config.itemLabel}`}
            </DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
              <FormField
                control={form.control}
                name="parentId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{config.parentLabel}</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder={`Choisir un(e) ${config.parentLabel.toLowerCase()}`} />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {parentOptions?.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.name}
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
                      <Input {...field} />
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
                    <FormLabel>Nom</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Annuler
                </Button>
                <Button type="submit" disabled={saveMutation.isPending}>
                  {saveMutation.isPending ? 'Enregistrement...' : 'Enregistrer'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={`Supprimer ce ${config.itemLabel} ?`}
        description={`"${deleting?.name}" sera définitivement supprimé si aucune donnée n'y est rattachée. Cette action est irréversible.`}
        destructive
        isConfirming={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  )
}
