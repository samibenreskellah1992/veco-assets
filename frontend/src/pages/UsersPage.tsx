import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Pencil, Plus } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { usersApi, rolesApi } from '@/services/user-service'
import { sitesApi } from '@/services/referentiel-service'
import { extractApiErrorMessage } from '@/lib/api-error'
import type { UserDto, UserStatus } from '@/types/user'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const NO_SITE = '__none__'

const userSchema = z.object({
  matricule: z.string().max(30).optional().or(z.literal('')),
  firstName: z.string().min(1, 'Prénom requis').max(100),
  lastName: z.string().min(1, 'Nom requis').max(100),
  email: z.string().min(1, 'Email requis').email('Email invalide'),
  phone: z.string().max(30).optional().or(z.literal('')),
  siteId: z.string(),
  department: z.string().max(150).optional().or(z.literal('')),
  service: z.string().max(150).optional().or(z.literal('')),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']),
  password: z.string().max(100).optional().or(z.literal('')),
  roleCodes: z.array(z.string()).min(1, 'Au moins un rôle est requis'),
})
type UserFormValues = z.infer<typeof userSchema>

const STATUS_LABEL: Record<UserStatus, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  SUSPENDED: 'Suspendu',
}

function statusBadgeVariant(status: UserStatus) {
  if (status === 'ACTIVE') return 'success' as const
  if (status === 'SUSPENDED') return 'destructive' as const
  return 'secondary' as const
}

export function UsersPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('USER_MANAGE')) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Accès refusé</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          La gestion des utilisateurs est réservée aux comptes disposant de la permission USER_MANAGE.
        </CardContent>
      </Card>
    )
  }

  return <UsersPageContent />
}

function UsersPageContent() {
  const queryClient = useQueryClient()
  const { data: users, isLoading } = useQuery({ queryKey: ['users'], queryFn: usersApi.list })
  const { data: roles } = useQuery({ queryKey: ['roles'], queryFn: rolesApi.list })
  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })

  const [editing, setEditing] = useState<UserDto | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [resetTarget, setResetTarget] = useState<UserDto | null>(null)
  const [newPassword, setNewPassword] = useState('')

  const form = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      matricule: '',
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      siteId: NO_SITE,
      department: '',
      service: '',
      status: 'ACTIVE',
      password: '',
      roleCodes: [],
    },
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['users'] })
  }

  function openCreate() {
    setEditing(null)
    form.reset({
      matricule: '',
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      siteId: NO_SITE,
      department: '',
      service: '',
      status: 'ACTIVE',
      password: '',
      roleCodes: [],
    })
    setDialogOpen(true)
  }

  function openEdit(user: UserDto) {
    setEditing(user)
    form.reset({
      matricule: user.matricule ?? '',
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone ?? '',
      siteId: user.siteId ?? NO_SITE,
      department: user.department ?? '',
      service: user.service ?? '',
      status: user.status,
      password: '',
      roleCodes: user.roleCodes,
    })
    setDialogOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: (values: UserFormValues) => {
      const shared = {
        matricule: values.matricule || undefined,
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        phone: values.phone || undefined,
        siteId: values.siteId === NO_SITE ? undefined : values.siteId,
        department: values.department || undefined,
        service: values.service || undefined,
        roleCodes: values.roleCodes,
      }
      if (editing) {
        return usersApi.update(editing.id, { ...shared, status: values.status })
      }
      return usersApi.create({ ...shared, password: values.password ?? '' })
    },
    onSuccess: () => {
      invalidate()
      toast.success(editing ? 'Utilisateur modifié' : 'Utilisateur créé')
      setDialogOpen(false)
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const toggleStatusMutation = useMutation({
    mutationFn: (user: UserDto) => (user.status === 'ACTIVE' ? usersApi.deactivate(user.id) : usersApi.activate(user.id)),
    onSuccess: invalidate,
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  const resetPasswordMutation = useMutation({
    mutationFn: () => usersApi.resetPassword(resetTarget!.id, newPassword),
    onSuccess: () => {
      toast.success('Mot de passe réinitialisé')
      setResetTarget(null)
      setNewPassword('')
    },
    onError: (error) => toast.error(extractApiErrorMessage(error)),
  })

  function onSubmit(values: UserFormValues) {
    if (!editing && (!values.password || values.password.length < 8)) {
      form.setError('password', { message: 'Mot de passe requis (8 caractères minimum)' })
      return
    }
    saveMutation.mutate(values)
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Utilisateurs</h1>
          <p className="text-sm text-muted-foreground">
            Comptes, rattachement site/service et rôles RBAC. Aucun compte n'est jamais supprimé — désactivez-le
            plutôt (l'audit trail et l'historique des immobilisations en dépendent).
          </p>
        </div>
        <Button size="sm" onClick={openCreate}>
          <Plus /> Nouvel utilisateur
        </Button>
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nom</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Site</TableHead>
              <TableHead>Rôles</TableHead>
              <TableHead>Statut</TableHead>
              <TableHead className="text-right">Actions</TableHead>
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
            {users?.map((user) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">
                  {user.firstName} {user.lastName}
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.siteName ?? '—'}</TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {user.roleCodes.map((code) => (
                      <Badge key={code} variant="secondary">
                        {code}
                      </Badge>
                    ))}
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(user.status)}>{STATUS_LABEL[user.status]}</Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="icon" title="Modifier" onClick={() => openEdit(user)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" title="Réinitialiser le mot de passe" onClick={() => setResetTarget(user)}>
                    <KeyRound className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={toggleStatusMutation.isPending}
                    onClick={() => toggleStatusMutation.mutate(user)}
                  >
                    {user.status === 'ACTIVE' ? 'Désactiver' : 'Activer'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-xl">
          <DialogHeader>
            <DialogTitle>{editing ? "Modifier l'utilisateur" : 'Nouvel utilisateur'}</DialogTitle>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Prénom</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="lastName"
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
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="matricule"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Matricule</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="phone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Téléphone</FormLabel>
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
                  name="department"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Direction / Département</FormLabel>
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
              </div>
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="siteId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Site</FormLabel>
                      <Select value={field.value} onValueChange={field.onChange}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={NO_SITE}>— Aucun —</SelectItem>
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
                {editing && (
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
                            {(Object.keys(STATUS_LABEL) as UserStatus[]).map((status) => (
                              <SelectItem key={status} value={status}>
                                {STATUS_LABEL[status]}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                )}
              </div>
              {!editing && (
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mot de passe initial</FormLabel>
                      <FormControl>
                        <Input type="password" autoComplete="new-password" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
              <FormField
                control={form.control}
                name="roleCodes"
                render={() => (
                  <FormItem>
                    <FormLabel>Rôles</FormLabel>
                    <div className="grid grid-cols-2 gap-2 rounded-md border border-input p-3">
                      {roles?.map((role) => (
                        <label key={role.code} className="flex items-center gap-2 text-sm">
                          <Checkbox
                            checked={form.watch('roleCodes').includes(role.code)}
                            onCheckedChange={(checked) => {
                              const current = form.getValues('roleCodes')
                              form.setValue(
                                'roleCodes',
                                checked ? [...current, role.code] : current.filter((code) => code !== role.code),
                                { shouldValidate: true },
                              )
                            }}
                          />
                          {role.label}
                        </label>
                      ))}
                    </div>
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

      <Dialog open={resetTarget !== null} onOpenChange={(open) => !open && setResetTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Réinitialiser le mot de passe</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Nouveau mot de passe pour {resetTarget?.firstName} {resetTarget?.lastName} — à communiquer à
            l'utilisateur hors application (aucune notification email en V1).
          </p>
          <Input
            type="password"
            placeholder="Nouveau mot de passe (8 caractères minimum)"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
          />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setResetTarget(null)}>
              Annuler
            </Button>
            <Button
              disabled={newPassword.length < 8 || resetPasswordMutation.isPending}
              onClick={() => resetPasswordMutation.mutate()}
            >
              {resetPasswordMutation.isPending ? 'Enregistrement...' : 'Réinitialiser'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
