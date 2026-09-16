import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Download, ExternalLink, Search, Tags } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { assetsApi } from '@/services/asset-service'
import { labelsApi, assetLabelFormatsApi } from '@/services/label-service'
import { sitesApi, assetCategoriesApi } from '@/services/referentiel-service'
import { extractBlobApiErrorMessage } from '@/lib/api-error'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'

const ALL = '__all__'
const PAGE_SIZE = 10

export function EtiquetagePage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('ETIQUETTE_GENERATE')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La génération d'étiquettes est réservée aux comptes disposant de la permission ETIQUETTE_GENERATE.
        </CardContent>
      </Card>
    )
  }

  return <EtiquetageContent />
}

function EtiquetageContent() {
  const [search, setSearch] = useState('')
  const [siteId, setSiteId] = useState(ALL)
  const [categoryId, setCategoryId] = useState(ALL)
  const [onlyUnlabeled, setOnlyUnlabeled] = useState(false)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [formatId, setFormatId] = useState<string>('')

  const [generatedPdf, setGeneratedPdf] = useState<{ blob: Blob; url: string } | null>(null)

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: categories } = useQuery({ queryKey: ['asset-categories'], queryFn: assetCategoriesApi.list })
  const { data: formats } = useQuery({ queryKey: ['asset-label-formats'], queryFn: assetLabelFormatsApi.list })
  const activeFormats = formats?.filter((format) => format.active) ?? []

  const params = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      sort: 'designation,asc',
      search: search || undefined,
      siteId: siteId === ALL ? undefined : siteId,
      categoryId: categoryId === ALL ? undefined : categoryId,
      labeled: onlyUnlabeled ? false : undefined,
    }),
    [page, search, siteId, categoryId, onlyUnlabeled],
  )

  const { data, isLoading } = useQuery({
    queryKey: ['assets', 'etiquetage', params],
    queryFn: () => assetsApi.list(params),
  })

  function resetPageAnd<T>(setter: (value: T) => void) {
    return (value: T) => {
      setPage(0)
      setter(value)
    }
  }

  function toggleSelected(id: string) {
    setGeneratedPdf(null)
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  function toggleSelectAllOnPage() {
    if (!data) return
    setGeneratedPdf(null)
    const idsOnPage = data.content.map((asset) => asset.id)
    const allSelected = idsOnPage.every((id) => selected.has(id))
    setSelected((current) => {
      const next = new Set(current)
      idsOnPage.forEach((id) => (allSelected ? next.delete(id) : next.add(id)))
      return next
    })
  }

  const generateMutation = useMutation({
    mutationFn: () => labelsApi.generate({ assetIds: Array.from(selected), formatId }),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      setGeneratedPdf((previous) => {
        if (previous) URL.revokeObjectURL(previous.url)
        return { blob, url }
      })
      toast.success(`${selected.size} étiquette(s) générée(s)`)
    },
    onError: async (error) => toast.error(await extractBlobApiErrorMessage(error)),
  })

  function downloadGenerated() {
    if (!generatedPdf) return
    const link = document.createElement('a')
    link.href = generatedPdf.url
    link.download = `etiquettes-${new Date().toISOString().slice(0, 10)}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Étiquetage</h1>
        <p className="text-sm text-muted-foreground">
          Sélectionnez des immobilisations et un format pour générer un PDF réel (QR code — et code-barres selon le
          format — identifiant chaque immobilisation par son code uniquement). Formats administrables dans
          Référentiel → Formats d'étiquette.
        </p>
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
          <label className="flex items-center gap-2 pb-2 text-sm">
            <Checkbox
              checked={onlyUnlabeled}
              onCheckedChange={(checked) => {
                setPage(0)
                setOnlyUnlabeled(checked === true)
              }}
            />
            Non étiquetées uniquement
          </label>
        </CardContent>
      </Card>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <Checkbox
                  checked={Boolean(data && data.content.length > 0 && data.content.every((asset) => selected.has(asset.id)))}
                  onCheckedChange={toggleSelectAllOnPage}
                />
              </TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Désignation</TableHead>
              <TableHead>Catégorie</TableHead>
              <TableHead>Site</TableHead>
              <TableHead>Étiquetée</TableHead>
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
            {!isLoading && data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Aucune immobilisation ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {data?.content.map((asset) => (
              <TableRow key={asset.id} className="cursor-pointer" onClick={() => toggleSelected(asset.id)}>
                <TableCell onClick={(event) => event.stopPropagation()}>
                  <Checkbox checked={selected.has(asset.id)} onCheckedChange={() => toggleSelected(asset.id)} />
                </TableCell>
                <TableCell className="font-mono text-xs">{asset.assetCode}</TableCell>
                <TableCell className="font-medium">{asset.designation}</TableCell>
                <TableCell>{asset.categoryName ?? '—'}</TableCell>
                <TableCell>{asset.siteName ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={asset.labeled ? 'success' : 'secondary'}>{asset.labeled ? 'Oui' : 'Non'}</Badge>
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

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="text-sm">
            <span className="font-medium">{selected.size}</span> immobilisation(s) sélectionnée(s)
            {selected.size > 0 && (
              <Button variant="link" className="h-auto p-0 pl-2 text-xs" onClick={() => setSelected(new Set())}>
                vider la sélection
              </Button>
            )}
          </div>
          <div className="w-64">
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Format d'étiquette</label>
            <Select value={formatId} onValueChange={setFormatId}>
              <SelectTrigger>
                <SelectValue placeholder="Sélectionner un format..." />
              </SelectTrigger>
              <SelectContent>
                {activeFormats.map((format) => (
                  <SelectItem key={format.id} value={format.id}>
                    {format.name} ({format.widthMm}×{format.heightMm} mm)
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button
            disabled={selected.size === 0 || !formatId || generateMutation.isPending}
            onClick={() => generateMutation.mutate()}
          >
            <Tags className="h-4 w-4" />
            {generateMutation.isPending ? 'Génération...' : 'Générer le PDF'}
          </Button>

          {generatedPdf && (
            <>
              <Button variant="outline" onClick={() => window.open(generatedPdf.url, '_blank')}>
                <ExternalLink className="h-4 w-4" /> Ouvrir l'aperçu
              </Button>
              <Button variant="outline" onClick={downloadGenerated}>
                <Download className="h-4 w-4" /> Télécharger
              </Button>
            </>
          )}
        </CardContent>
      </Card>
      {generatedPdf && (
        <p className="text-xs text-muted-foreground">
          PDF généré pour {selected.size} immobilisation(s) — chaque génération (aperçu ou téléchargement) est
          tracée dans l'historique de l'immobilisation et le journal d'audit.
        </p>
      )}
    </div>
  )
}
