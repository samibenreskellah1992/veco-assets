import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { assetLabelFormatsApi } from '@/services/label-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import type { AssetLabelFormatDto } from '@/types/label'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { ConfirmDialog } from '@/components/ConfirmDialog'

const formatSchema = z.object({
  code: z.string().min(1, 'Code requis').max(50),
  name: z.string().min(1, 'Nom requis').max(150),
  widthMm: z.coerce.number().positive("La largeur doit être supérieure à 0"),
  heightMm: z.coerce.number().positive('La hauteur doit être supérieure à 0'),
  showLogo: z.boolean(),
  showShortDesignation: z.boolean(),
  showQrCode: z.boolean(),
  showBarcode: z.boolean(),
})
type FormatFormValues = z.infer<typeof formatSchema>

const DEFAULT_VALUES: FormatFormValues = {
  code: '',
  name: '',
  widthMm: 50,
  heightMm: 30,
  showLogo: true,
  showShortDesignation: true,
  showQrCode: true,
  showBarcode: false,
}

/**
 * Formats d'étiquette (Phase 6, prompt maître section 13) : dimensions et
 * contenu affiché sont administrables ici plutôt que codés en dur dans le
 * module /etiquetage — celui-ci se contente de peupler son sélecteur de
 * format depuis GET /api/asset-label-formats.
 */
export function LabelFormatsPanel() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('ETIQUETTE_MANAGE')
  const queryClient = useQueryClient()
  const { data: formats, isLoading } = useQuery({ queryKey: ['asset-label-formats'], queryFn: assetLabelFormatsApi.list })

  const [editing, setEditing] = useState<AssetLabelFormatDto | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleting, setDeleting] = useState<AssetLabelFormatDto | null>(null)

  const form = useForm<FormatFormValues>({
    resolver: zodResolver(formatSchema),
    defaultValues: DEFAULT_VALUES,
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['asset-label-formats'] })
  }

  function openCreate() {
    setEditing(null)
    form.reset(DEFAULT_VALUES)
    setDialogOpen(true)
  }

  function openEdit(format: AssetLabelFormatDto) {
    setEditing(format)
    form.reset({
      code: format.code,
      name: format.name,
      widthMm: format.widthMm,
      heightMm: format.heightMm,
      showLogo: format.showLogo,
      showShortDesignation: format.showShortDesignation,
      showQrCode: format.showQrCode,
      showBarcode: format.showBarcode,
    })
    setDialogOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: (values: FormatFormValues) =>
      editing ? assetLabelFormatsApi.update(editing.id, values) : assetLabelFormatsApi.create(values),
    onSuccess: () => {
      invalidate()
      toast.success(editing ? 'Format modifié' : 'Format créé')
      setDialogOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const toggleActiveMutation = useMutation({
    mutationFn: (format: AssetLabelFormatDto) =>
      format.active ? assetLabelFormatsApi.deactivate(format.id) : assetLabelFormatsApi.activate(format.id),
    onSuccess: invalidate,
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => assetLabelFormatsApi.remove(id),
    onSuccess: () => {
      invalidate()
      toast.success('Format supprimé')
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
          Dimensions réelles et contenu affiché (logo, désignation, QR code, code-barres) pour le module Étiquetage.
        </p>
        {canManage && (
          <Button size="sm" onClick={openCreate}>
            <Plus /> Nouveau format
          </Button>
        )}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Nom</TableHead>
              <TableHead>Dimensions</TableHead>
              <TableHead>Contenu</TableHead>
              <TableHead>Statut</TableHead>
              {canManage && <TableHead className="text-right">Actions</TableHead>}
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
            {!isLoading && formats?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Aucun format d'étiquette.
                </TableCell>
              </TableRow>
            )}
            {formats?.map((format) => (
              <TableRow key={format.id}>
                <TableCell className="font-medium">{format.code}</TableCell>
                <TableCell>{format.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {format.widthMm} × {format.heightMm} mm
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {format.showLogo && <Badge variant="outline">Logo</Badge>}
                    {format.showShortDesignation && <Badge variant="outline">Désignation</Badge>}
                    {format.showQrCode && <Badge variant="outline">QR code</Badge>}
                    {format.showBarcode && <Badge variant="outline">Code-barres</Badge>}
                  </div>
                </TableCell>
                <TableCell>
                  {canManage ? (
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={format.active}
                        disabled={toggleActiveMutation.isPending}
                        onCheckedChange={() => toggleActiveMutation.mutate(format)}
                      />
                      <span className="text-xs text-muted-foreground">{format.active ? 'Actif' : 'Inactif'}</span>
                    </div>
                  ) : (
                    <Badge variant={format.active ? 'success' : 'secondary'}>{format.active ? 'Actif' : 'Inactif'}</Badge>
                  )}
                </TableCell>
                {canManage && (
                  <TableCell className="text-right">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(format)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => setDeleting(format)}>
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
            <DialogTitle>{editing ? 'Modifier le format' : 'Nouveau format'}</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
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
              </div>
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="widthMm"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Largeur (mm)</FormLabel>
                      <FormControl>
                        <Input type="number" min="1" step="0.1" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="heightMm"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Hauteur (mm)</FormLabel>
                      <FormControl>
                        <Input type="number" min="1" step="0.1" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
              <div className="grid grid-cols-2 gap-3 rounded-md border border-input p-3">
                <FormField
                  control={form.control}
                  name="showLogo"
                  render={({ field }) => (
                    <label className="flex items-center gap-2 text-sm">
                      <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                      Logo
                    </label>
                  )}
                />
                <FormField
                  control={form.control}
                  name="showShortDesignation"
                  render={({ field }) => (
                    <label className="flex items-center gap-2 text-sm">
                      <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                      Désignation courte
                    </label>
                  )}
                />
                <FormField
                  control={form.control}
                  name="showQrCode"
                  render={({ field }) => (
                    <label className="flex items-center gap-2 text-sm">
                      <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                      QR code
                    </label>
                  )}
                />
                <FormField
                  control={form.control}
                  name="showBarcode"
                  render={({ field }) => (
                    <label className="flex items-center gap-2 text-sm">
                      <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                      Code-barres
                    </label>
                  )}
                />
              </div>
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
        title="Supprimer ce format ?"
        description={`"${deleting?.name}" sera définitivement supprimé si aucune étiquette n'a déjà été générée avec. Cette action est irréversible.`}
        destructive
        isConfirming={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  )
}
