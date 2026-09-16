import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Plus, Search } from 'lucide-react'

import { useAuth } from '@/hooks/use-auth'
import { assetsApi } from '@/services/asset-service'
import { sitesApi, assetCategoriesApi } from '@/services/referentiel-service'
import {
  ASSET_CONDITION_LABEL,
  ASSET_STATUS_LABEL,
  type AssetCondition,
  type AssetStatus,
} from '@/types/asset'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'

const ALL = '__all__'
const PAGE_SIZE = 20

function conditionBadgeVariant(condition: AssetCondition) {
  if (condition === 'NEUF' || condition === 'BON') return 'success' as const
  if (condition === 'MOYEN') return 'secondary' as const
  return 'destructive' as const
}

function statusBadgeVariant(status: AssetStatus) {
  if (status === 'EN_SERVICE') return 'success' as const
  if (status === 'REFORME' || status === 'SORTI') return 'destructive' as const
  return 'secondary' as const
}

export function AssetsPage() {
  const { hasPermission } = useAuth()
  const navigate = useNavigate()

  const [search, setSearch] = useState('')
  const [siteId, setSiteId] = useState(ALL)
  const [categoryId, setCategoryId] = useState(ALL)
  const [condition, setCondition] = useState(ALL)
  const [status, setStatus] = useState(ALL)
  const [includeDeleted, setIncludeDeleted] = useState(false)
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState('designation,asc')

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: categories } = useQuery({ queryKey: ['asset-categories'], queryFn: assetCategoriesApi.list })

  const params = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      sort,
      search: search || undefined,
      siteId: siteId === ALL ? undefined : siteId,
      categoryId: categoryId === ALL ? undefined : categoryId,
      condition: condition === ALL ? undefined : (condition as AssetCondition),
      status: status === ALL ? undefined : (status as AssetStatus),
      includeDeleted,
    }),
    [page, sort, search, siteId, categoryId, condition, status, includeDeleted],
  )

  const { data, isLoading } = useQuery({
    queryKey: ['assets', params],
    queryFn: () => assetsApi.list(params),
  })

  function toggleSort(field: string) {
    setPage(0)
    setSort((current) => {
      const [currentField, currentDir] = current.split(',')
      if (currentField === field) {
        return `${field},${currentDir === 'asc' ? 'desc' : 'asc'}`
      }
      return `${field},asc`
    })
  }

  function resetPageAnd<T>(setter: (value: T) => void) {
    return (value: T) => {
      setPage(0)
      setter(value)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Immobilisations</h1>
          <p className="text-sm text-muted-foreground">
            Registre des immobilisations VECOPHARM — recherche, filtres combinables et export à venir.
          </p>
        </div>
        {hasPermission('IMMOBILISATION_CREATE') && (
          <Button size="sm" onClick={() => navigate('/immobilisations/nouveau')}>
            <Plus /> Nouvelle immobilisation
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
                placeholder="Code, désignation, numéro de série..."
                value={search}
                onChange={(event) => {
                  setPage(0)
                  setSearch(event.target.value)
                }}
              />
            </div>
          </div>

          <div className="w-48">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Site</label>
            <Select value={siteId} onValueChange={resetPageAnd(setSiteId)}>
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
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Catégorie</label>
            <Select value={categoryId} onValueChange={resetPageAnd(setCategoryId)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Toutes les catégories</SelectItem>
                {categories?.map((category) => (
                  <SelectItem key={category.id} value={category.id}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="w-40">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">État</label>
            <Select value={condition} onValueChange={resetPageAnd(setCondition)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous</SelectItem>
                {(Object.keys(ASSET_CONDITION_LABEL) as AssetCondition[]).map((value) => (
                  <SelectItem key={value} value={value}>
                    {ASSET_CONDITION_LABEL[value]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="w-40">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Statut</label>
            <Select value={status} onValueChange={resetPageAnd(setStatus)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>Tous</SelectItem>
                {(Object.keys(ASSET_STATUS_LABEL) as AssetStatus[]).map((value) => (
                  <SelectItem key={value} value={value}>
                    {ASSET_STATUS_LABEL[value]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <label className="flex items-center gap-2 pb-2 text-sm">
            <Checkbox
              checked={includeDeleted}
              onCheckedChange={(checked) => {
                setPage(0)
                setIncludeDeleted(checked === true)
              }}
            />
            Inclure les archivées
          </label>
        </CardContent>
      </Card>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="cursor-pointer select-none" onClick={() => toggleSort('assetCode')}>
                Code
              </TableHead>
              <TableHead className="cursor-pointer select-none" onClick={() => toggleSort('designation')}>
                Désignation
              </TableHead>
              <TableHead>Catégorie</TableHead>
              <TableHead>Localisation</TableHead>
              <TableHead>Affecté à</TableHead>
              <TableHead>État</TableHead>
              <TableHead>Statut</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Chargement...
                </TableCell>
              </TableRow>
            )}
            {!isLoading && data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Aucune immobilisation ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {data?.content.map((asset) => (
              <TableRow key={asset.id} className={asset.deleted ? 'opacity-60' : undefined}>
                <TableCell className="font-mono text-xs">
                  <Link to={`/immobilisations/${asset.id}`} className="text-primary hover:underline">
                    {asset.assetCode}
                  </Link>
                </TableCell>
                <TableCell className="font-medium">
                  {asset.designation}
                  {asset.deleted && (
                    <Badge variant="destructive" className="ml-2">
                      Archivée
                    </Badge>
                  )}
                </TableCell>
                <TableCell>{asset.categoryName ?? '—'}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {[asset.siteName, asset.buildingName, asset.locationName].filter(Boolean).join(' / ') || '—'}
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{asset.currentUserName ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={conditionBadgeVariant(asset.condition)}>{ASSET_CONDITION_LABEL[asset.condition]}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={statusBadgeVariant(asset.status)}>{ASSET_STATUS_LABEL[asset.status]}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            {data.page * data.size + 1}–{Math.min((data.page + 1) * data.size, data.totalElements)} sur {data.totalElements}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="icon" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span>
              Page {data.page + 1} / {Math.max(data.totalPages, 1)}
            </span>
            <Button
              variant="outline"
              size="icon"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
