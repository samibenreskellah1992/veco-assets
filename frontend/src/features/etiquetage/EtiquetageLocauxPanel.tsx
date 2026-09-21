import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Download, ExternalLink, Search, Tags } from 'lucide-react'
import { toast } from 'sonner'

import { locationsApi, sitesApi } from '@/services/referentiel-service'
import { labelsApi, assetLabelFormatsApi } from '@/services/label-service'
import { extractBlobApiErrorMessage } from '@/lib/api-error'
import type { LocationDto } from '@/types/referentiel'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'

const ALL = '__all__'

/**
 * Contenu de l'onglet "Locaux" de la page Étiquetage (Checkpoint 2 de
 * l'evolution "locaux scannables", 2026-09) - mirroir de
 * `EtiquetageAssetsPanel` (Phase 6) pour les locaux : meme principe
 * (sélection + format -> PDF réel), mais liste non paginée côté serveur
 * (comme `LocationsPage`, volume modeste) plutôt que paginée comme les
 * immobilisations.
 */
export function EtiquetageLocauxPanel() {
  const [search, setSearch] = useState('')
  const [siteId, setSiteId] = useState(ALL)
  const [onlyUnlabeled, setOnlyUnlabeled] = useState(false)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [formatId, setFormatId] = useState<string>('')

  const [generatedPdf, setGeneratedPdf] = useState<{ blob: Blob; url: string } | null>(null)

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: formats } = useQuery({ queryKey: ['asset-label-formats'], queryFn: assetLabelFormatsApi.list })
  const activeFormats = formats?.filter((format) => format.active) ?? []

  const { data: locations, isLoading } = useQuery({ queryKey: ['locations'], queryFn: () => locationsApi.list() })

  const filtered = useMemo(() => {
    if (!locations) return []
    const term = search.trim().toLowerCase()
    return locations.filter((location: LocationDto) => {
      if (siteId !== ALL && location.siteId !== siteId) return false
      if (onlyUnlabeled && location.labeled) return false
      if (term) {
        const haystack = [location.qrCode, location.name, location.code, location.zoneName]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        if (!haystack.includes(term)) return false
      }
      return true
    })
  }, [locations, search, siteId, onlyUnlabeled])

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

  function toggleSelectAllFiltered() {
    setGeneratedPdf(null)
    const ids = filtered.map((location) => location.id)
    const allSelected = ids.length > 0 && ids.every((id) => selected.has(id))
    setSelected((current) => {
      const next = new Set(current)
      ids.forEach((id) => (allSelected ? next.delete(id) : next.add(id)))
      return next
    })
  }

  const generateMutation = useMutation({
    mutationFn: () => labelsApi.generateForLocations({ locationIds: Array.from(selected), formatId }),
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
    link.download = `etiquettes-locaux-${new Date().toISOString().slice(0, 10)}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Sélectionnez des locaux et un format pour générer un PDF réel (QR code identifiant chaque local par son code
        uniquement — le même généré à sa création, voir Locaux). Formats administrables dans Référentiel → Formats
        d'étiquette (partagés avec l'étiquetage des immobilisations).
      </p>

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
          <label className="flex items-center gap-2 pb-2 text-sm">
            <Checkbox
              checked={onlyUnlabeled}
              onCheckedChange={(checked) => setOnlyUnlabeled(checked === true)}
            />
            Non étiquetés uniquement
          </label>
        </CardContent>
      </Card>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <Checkbox
                  checked={Boolean(filtered.length > 0 && filtered.every((location) => selected.has(location.id)))}
                  onCheckedChange={toggleSelectAllFiltered}
                />
              </TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Désignation</TableHead>
              <TableHead>Site</TableHead>
              <TableHead>Zone</TableHead>
              <TableHead>Étiqueté</TableHead>
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
            {!isLoading && filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Aucun local ne correspond à ces critères.
                </TableCell>
              </TableRow>
            )}
            {filtered.map((location) => (
              <TableRow key={location.id} className="cursor-pointer" onClick={() => toggleSelected(location.id)}>
                <TableCell onClick={(event) => event.stopPropagation()}>
                  <Checkbox checked={selected.has(location.id)} onCheckedChange={() => toggleSelected(location.id)} />
                </TableCell>
                <TableCell className="font-mono text-xs">{location.qrCode}</TableCell>
                <TableCell className="font-medium">{location.name}</TableCell>
                <TableCell>{location.siteName ?? '—'}</TableCell>
                <TableCell>{location.zoneName}</TableCell>
                <TableCell>
                  <Badge variant={location.labeled ? 'success' : 'secondary'}>{location.labeled ? 'Oui' : 'Non'}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="text-sm">
            <span className="font-medium">{selected.size}</span> local/locaux sélectionné(s)
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
          PDF généré pour {selected.size} local/locaux — chaque génération (aperçu ou téléchargement) est tracée dans
          l'historique du local et le journal d'audit.
        </p>
      )}
    </div>
  )
}
