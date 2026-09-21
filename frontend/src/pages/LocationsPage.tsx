import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'

import { useAuth } from '@/hooks/use-auth'
import { locationsApi, sitesApi } from '@/services/referentiel-service'
import { LOCATION_STATUS_LABEL, type LocationDto, type LocationStatus } from '@/types/referentiel'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const ALL = '__all__'

function statusBadgeVariant(status: LocationStatus) {
  if (status === 'ACTIF') return 'success' as const
  if (status === 'FERME' || status === 'INACTIF') return 'destructive' as const
  return 'secondary' as const
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('fr-FR')
}

/**
 * Checkpoint 1 de l'evolution "locaux scannables" (2026-09) : module CRUD
 * dedie aux locaux, distinct de l'onglet "Localisations" du référentiel
 * (qui reste la creation/edition rapide en tableau imbriqué) - cette liste
 * met en avant les nouveaux champs (code QR, statut, responsable, nombre
 * d'immobilisations) attendus par les futurs ecrans de scan (phases
 * suivantes, hors perimetre ici).
 *
 * `GET /api/locations` n'est pas paginé côté backend (liste référentiel,
 * volume modeste) : on charge tout et on filtre côté client, comme
 * `ReferentielPage`/`LevelPanel` le font déjà pour les autres niveaux de la
 * hiérarchie.
 */
export function LocationsPage() {
  const { hasPermission } = useAuth()
  const navigate = useNavigate()

  const [search, setSearch] = useState('')
  const [siteId, setSiteId] = useState(ALL)
  const [status, setStatus] = useState(ALL)

  const { data: locations, isLoading } = useQuery({ queryKey: ['locations'], queryFn: () => locationsApi.list() })
  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })

  const filtered = useMemo(() => {
    if (!locations) return []
    const term = search.trim().toLowerCase()
    return locations.filter((location: LocationDto) => {
      if (siteId !== ALL && location.siteId !== siteId) return false
      if (status !== ALL && location.status !== status) return false
      if (term) {
        const haystack = [location.qrCode, location.name, location.code, location.zoneName]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        if (!haystack.includes(term)) return false
      }
      return true
    })
  }, [locations, search, siteId, status])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Locaux</h1>
          <p className="text-sm text-muted-foreground">
            Locaux scannables (Site / Bâtiment / Étage / Zone / Local) — point d'entrée du futur inventaire par local.
          </p>
        </div>
        {hasPermission('REFERENTIEL_MANAGE') && (
          <Button size="sm" onClick={() => navigate('/locaux/nouveau')}>
            <Plus /> Nouveau local
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="min-w-[220px] flex-1">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Recherche</label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                className="pl-8"
                placeholder="Code QR, désignation, zone..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
          </div>

          <div className="w-48">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Site</label>
            <Select value={siteId} onValueChange={setSiteId}>
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

          <div className="w-44">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Statut</label>
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous</SelectItem>
                {(Object.keys(LOCATION_STATUS_LABEL) as LocationStatus[]).map((value) => (
                  <SelectItem key={value} value={value}>
                    {LOCATION_STATUS_LABEL[value]}
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
              <TableHead>Code</TableHead>
              <TableHead>Désignation</TableHead>
              <TableHead>Site</TableHead>
              <TableHead>Bâtiment</TableHead>
              <TableHead>Étage</TableHead>
              <TableHead>Zone</TableHead>
              <TableHead>Nb immobilisations</TableHead>
              <TableHead>Dernier inventaire</TableHead>
              <TableHead>Statut</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={9} className="text-center text-muted-foreground">
                  Chargement...
                </TableCell>
              </TableRow>
            )}
            {!isLoading && filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={9} className="text-center text-muted-foreground">
                  Aucun local ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {filtered.map((location) => (
              <TableRow key={location.id}>
                <TableCell className="font-mono text-xs">
                  <Link to={`/locaux/${location.id}`} className="text-primary hover:underline">
                    {location.qrCode}
                  </Link>
                </TableCell>
                <TableCell className="font-medium">{location.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{location.siteName ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{location.buildingName ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{location.floorName ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{location.zoneName}</TableCell>
                <TableCell>{location.assetCount}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{formatDate(location.lastInventoryAt)}</TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(location.status)}>{LOCATION_STATUS_LABEL[location.status]}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
