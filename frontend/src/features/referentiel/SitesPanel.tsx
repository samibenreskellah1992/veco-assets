import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { sitesApi } from '@/services/referentiel-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import type { SiteDto } from '@/types/referentiel'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const siteSchema = z.object({
  code: z.string().min(1, 'Code requis').max(30),
  name: z.string().min(1, 'Nom requis').max(150),
  city: z.string().max(100).optional().or(z.literal('')),
  address: z.string().max(2000).optional().or(z.literal('')),
})
type SiteFormValues = z.infer<typeof siteSchema>

export function SitesPanel() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENTIEL_MANAGE')
  const queryClient = useQueryClient()
  const { data: sites, isLoading } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })

  const [editing, setEditing] = useState<SiteDto | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleting, setDeleting] = useState<SiteDto | null>(null)

  const form = useForm<SiteFormValues>({
    resolver: zodResolver(siteSchema),
    defaultValues: { code: '', name: '', city: '', address: '' },
  })

  function openCreate() {
    setEditing(null)
    form.reset({ code: '', name: '', city: '', address: '' })
    setDialogOpen(true)
  }

  function openEdit(site: SiteDto) {
    setEditing(site)
    form.reset({ code: site.code, name: site.name, city: site.city ?? '', address: site.address ?? '' })
    setDialogOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: (values: SiteFormValues) =>
      editing ? sitesApi.update(editing.id, values) : sitesApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sites'] })
      toast.success(editing ? 'Site modifié' : 'Site créé')
      setDialogOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const toggleActiveMutation = useMutation({
    mutationFn: (site: SiteDto) => (site.active ? sitesApi.deactivate(site.id) : sitesApi.activate(site.id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sites'] }),
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => sitesApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sites'] })
      toast.success('Site supprimé')
      setDeleting(null)
    },
    onError: (error) => {
      toast.error(extractApiErrorMessage(error))
      setDeleting(null)
    },
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Racine de la hiérarchie de localisation. Un site utilisé (bâtiments, utilisateurs, immobilisations) ne peut
          pas être supprimé — désactivez-le plutôt.
        </p>
        {canManage && (
          <Button size="sm" onClick={openCreate}>
            <Plus /> Nouveau site
          </Button>
        )}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Nom</TableHead>
              <TableHead>Ville</TableHead>
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
            {!isLoading && sites?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">
                  Aucun site.
                </TableCell>
              </TableRow>
            )}
            {sites?.map((site) => (
              <TableRow key={site.id}>
                <TableCell className="font-medium">{site.code}</TableCell>
                <TableCell>{site.name}</TableCell>
                <TableCell>{site.city || '—'}</TableCell>
                <TableCell>
                  {canManage ? (
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={site.active}
                        disabled={toggleActiveMutation.isPending}
                        onCheckedChange={() => toggleActiveMutation.mutate(site)}
                      />
                      <span className="text-xs text-muted-foreground">{site.active ? 'Actif' : 'Inactif'}</span>
                    </div>
                  ) : (
                    <Badge variant={site.active ? 'success' : 'secondary'}>{site.active ? 'Actif' : 'Inactif'}</Badge>
                  )}
                </TableCell>
                {canManage && (
                  <TableCell className="text-right">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(site)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => setDeleting(site)}>
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
            <DialogTitle>{editing ? 'Modifier le site' : 'Nouveau site'}</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Code</FormLabel>
                    <FormControl>
                      <Input placeholder="ALG" {...field} />
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
                      <Input placeholder="Alger" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Ville</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="address"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Adresse</FormLabel>
                    <FormControl>
                      <Textarea rows={2} {...field} />
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
        title="Supprimer ce site ?"
        description={`"${deleting?.name}" sera définitivement supprimé si aucune donnée n'y est rattachée. Cette action est irréversible.`}
        destructive
        isConfirming={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  )
}
