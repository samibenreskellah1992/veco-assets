import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { assetCategoriesApi } from '@/services/referentiel-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import type { AssetCategoryDto } from '@/types/referentiel'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const NO_PARENT = '__none__'

const categorySchema = z.object({
  code: z.string().min(1, 'Code requis').max(30),
  name: z.string().min(1, 'Nom requis').max(150),
  parentId: z.string(),
})
type CategoryFormValues = z.infer<typeof categorySchema>

export function CategoriesPanel() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENTIEL_MANAGE')
  const queryClient = useQueryClient()
  const { data: categories, isLoading } = useQuery({ queryKey: ['asset-categories'], queryFn: assetCategoriesApi.list })

  const [editing, setEditing] = useState<AssetCategoryDto | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleting, setDeleting] = useState<AssetCategoryDto | null>(null)

  const form = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { code: '', name: '', parentId: NO_PARENT },
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['asset-categories'] })
  }

  function openCreate() {
    setEditing(null)
    form.reset({ code: '', name: '', parentId: NO_PARENT })
    setDialogOpen(true)
  }

  function openEdit(category: AssetCategoryDto) {
    setEditing(category)
    form.reset({ code: category.code, name: category.name, parentId: category.parentId ?? NO_PARENT })
    setDialogOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: (values: CategoryFormValues) => {
      const request = {
        code: values.code,
        name: values.name,
        parentId: values.parentId === NO_PARENT ? null : values.parentId,
      }
      return editing ? assetCategoriesApi.update(editing.id, request) : assetCategoriesApi.create(request)
    },
    onSuccess: () => {
      invalidate()
      toast.success(editing ? 'Catégorie modifiée' : 'Catégorie créée')
      setDialogOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const toggleActiveMutation = useMutation({
    mutationFn: (category: AssetCategoryDto) =>
      category.active ? assetCategoriesApi.deactivate(category.id) : assetCategoriesApi.activate(category.id),
    onSuccess: invalidate,
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => assetCategoriesApi.remove(id),
    onSuccess: () => {
      invalidate()
      toast.success('Catégorie supprimée')
      setDeleting(null)
    },
    onError: (error) => {
      toast.error(extractApiErrorMessage(error))
      setDeleting(null)
    },
  })

  // Une categorie ne peut pas devenir son propre parent ni celui d'un de ses ancetres
  // (le backend le revalide de toute facon - voir AssetCategoryService.resolveParent) -
  // on l'exclut simplement elle-meme de la liste des parents proposes.
  const parentCandidates = categories?.filter((category) => category.id !== editing?.id) ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Catégories et sous-catégories d'immobilisation. Le code reste unique sur l'ensemble de l'arbre.
        </p>
        {canManage && (
          <Button size="sm" onClick={openCreate}>
            <Plus /> Nouvelle catégorie
          </Button>
        )}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Nom</TableHead>
              <TableHead>Catégorie parente</TableHead>
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
            {!isLoading && categories?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">
                  Aucune catégorie.
                </TableCell>
              </TableRow>
            )}
            {categories?.map((category) => (
              <TableRow key={category.id}>
                <TableCell className="font-medium">{category.code}</TableCell>
                <TableCell>{category.name}</TableCell>
                <TableCell>{category.parentName ?? <span className="text-muted-foreground">— racine —</span>}</TableCell>
                <TableCell>
                  {canManage ? (
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={category.active}
                        disabled={toggleActiveMutation.isPending}
                        onCheckedChange={() => toggleActiveMutation.mutate(category)}
                      />
                      <span className="text-xs text-muted-foreground">{category.active ? 'Actif' : 'Inactif'}</span>
                    </div>
                  ) : (
                    <Badge variant={category.active ? 'success' : 'secondary'}>
                      {category.active ? 'Actif' : 'Inactif'}
                    </Badge>
                  )}
                </TableCell>
                {canManage && (
                  <TableCell className="text-right">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(category)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => setDeleting(category)}>
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
            <DialogTitle>{editing ? 'Modifier la catégorie' : 'Nouvelle catégorie'}</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
              <FormField
                control={form.control}
                name="parentId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Catégorie parente</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value={NO_PARENT}>— Aucune (catégorie racine) —</SelectItem>
                        {parentCandidates.map((option) => (
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
        title="Supprimer cette catégorie ?"
        description={`"${deleting?.name}" sera définitivement supprimée si aucune donnée n'y est rattachée. Cette action est irréversible.`}
        destructive
        isConfirming={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  )
}
