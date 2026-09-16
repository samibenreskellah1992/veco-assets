import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { toast } from 'sonner'

import { useAuth } from '@/hooks/use-auth'
import { reportsApi } from '@/services/report-service'
import { sitesApi, assetCategoriesApi } from '@/services/referentiel-service'
import { extractApiErrorMessage, extractBlobApiErrorMessage } from '@/lib/api-error'
import { ASSET_CONDITION_LABEL, ASSET_STATUS_LABEL, type AssetCondition, type AssetStatus } from '@/types/asset'
import { ANOMALY_STATUS_LABEL, type AnomalyStatus } from '@/types/inventory'
import { MOVEMENT_TYPE_LABEL, type MovementType } from '@/types/movement'
import { REPORT_TYPE_LABEL, REPORT_TYPE_RELEVANT_FILTERS, type ExportFormat, type ReportFilterParams, type ReportType } from '@/types/report'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const ALL = '__all__'
const REPORT_TYPES: ReportType[] = [
  'PAR_SITE',
  'PAR_CATEGORIE',
  'PAR_SERVICE',
  'PAR_UTILISATEUR',
  'PAR_ETAT',
  'NON_ETIQUETEES',
  'NON_INVENTORIEES',
  'ANOMALIES',
  'MOUVEMENTS',
  'TRANSFERTS',
  'REFORMES',
]

/**
 * Module /rapports (prompt maitre Phase 9). Le tableau affiché à l'écran et
 * les trois exports (CSV/Excel/PDF) viennent du MÊME appel API
 * (reportsApi.get / reportsApi.export appellent tous les deux
 * ReportService.generate côté backend) - ce qui est vu à l'écran est
 * garanti identique à ce qui est téléchargé.
 */
export function ReportsPage() {
  const { hasPermission } = useAuth()

  if (!hasPermission('REPORT_VIEW')) {
    return (
      <Card>
        <CardContent className="pt-6 text-sm text-muted-foreground">
          La consultation des rapports est réservée aux comptes disposant de la permission REPORT_VIEW.
        </CardContent>
      </Card>
    )
  }

  return <ReportsContent />
}

function ReportsContent() {
  const { hasPermission } = useAuth()
  const canExport = hasPermission('REPORT_EXPORT')

  const [type, setType] = useState<ReportType>('PAR_SITE')
  const [siteId, setSiteId] = useState<string>(ALL)
  const [categoryId, setCategoryId] = useState<string>(ALL)
  const [condition, setCondition] = useState<AssetCondition | typeof ALL>(ALL)
  const [status, setStatus] = useState<AssetStatus | typeof ALL>(ALL)
  const [movementType, setMovementType] = useState<MovementType | typeof ALL>(ALL)
  const [anomalyStatus, setAnomalyStatus] = useState<AnomalyStatus | typeof ALL>(ALL)
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  const relevant = REPORT_TYPE_RELEVANT_FILTERS[type]

  const { data: sites } = useQuery({ queryKey: ['sites'], queryFn: sitesApi.list })
  const { data: categories } = useQuery({ queryKey: ['asset-categories'], queryFn: assetCategoriesApi.list })

  const filters: ReportFilterParams = useMemo(
    () => ({
      siteId: relevant.includes('siteId') && siteId !== ALL ? siteId : undefined,
      categoryId: relevant.includes('categoryId') && categoryId !== ALL ? categoryId : undefined,
      condition: relevant.includes('condition') && condition !== ALL ? condition : undefined,
      status: relevant.includes('status') && status !== ALL ? status : undefined,
      movementType: relevant.includes('movementType') && movementType !== ALL ? movementType : undefined,
      anomalyStatus: relevant.includes('anomalyStatus') && anomalyStatus !== ALL ? anomalyStatus : undefined,
      dateFrom: relevant.includes('dateFrom') && dateFrom ? dateFrom : undefined,
      dateTo: relevant.includes('dateTo') && dateTo ? dateTo : undefined,
    }),
    [relevant, siteId, categoryId, condition, status, movementType, anomalyStatus, dateFrom, dateTo],
  )

  const { data: report, isLoading, isError, error } = useQuery({
    queryKey: ['reports', type, filters],
    queryFn: () => reportsApi.get(type, filters),
  })

  const exportMutation = useMutation({
    mutationFn: (format: ExportFormat) => reportsApi.export(type, format, filters),
    onSuccess: (blob, format) => {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `rapport-${type.toLowerCase()}-${new Date().toISOString().slice(0, 10)}.${format.toLowerCase()}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      toast.success('Export généré')
    },
    onError: async (err) => toast.error(await extractBlobApiErrorMessage(err)),
  })

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Rapports</h1>
        <p className="text-sm text-muted-foreground">Répartitions, listes de contrôle et exports du patrimoine</p>
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="min-w-[220px]">
            <label className="mb-1 block text-xs text-muted-foreground">Type de rapport</label>
            <Select value={type} onValueChange={(v) => setType(v as ReportType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {REPORT_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {REPORT_TYPE_LABEL[t]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {relevant.includes('siteId') && (
            <FilterSelect label="Site" value={siteId} onChange={setSiteId}>
              {sites?.map((s) => (
                <SelectItem key={s.id} value={s.id}>
                  {s.name}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('categoryId') && (
            <FilterSelect label="Catégorie" value={categoryId} onChange={setCategoryId}>
              {categories?.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('condition') && (
            <FilterSelect label="État physique" value={condition} onChange={(v) => setCondition(v as AssetCondition | typeof ALL)}>
              {Object.entries(ASSET_CONDITION_LABEL).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('status') && (
            <FilterSelect label="Statut" value={status} onChange={(v) => setStatus(v as AssetStatus | typeof ALL)}>
              {Object.entries(ASSET_STATUS_LABEL).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('movementType') && (
            <FilterSelect label="Type de mouvement" value={movementType} onChange={(v) => setMovementType(v as MovementType | typeof ALL)}>
              {Object.entries(MOVEMENT_TYPE_LABEL).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('anomalyStatus') && (
            <FilterSelect label="Statut anomalie" value={anomalyStatus} onChange={(v) => setAnomalyStatus(v as AnomalyStatus | typeof ALL)}>
              {Object.entries(ANOMALY_STATUS_LABEL).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </FilterSelect>
          )}

          {relevant.includes('dateFrom') && (
            <div className="w-[160px]">
              <label className="mb-1 block text-xs text-muted-foreground">Du</label>
              <Input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
            </div>
          )}
          {relevant.includes('dateTo') && (
            <div className="w-[160px]">
              <label className="mb-1 block text-xs text-muted-foreground">Au</label>
              <Input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
            </div>
          )}

          {canExport && (
            <div className="ml-auto flex gap-2">
              <Button variant="outline" size="sm" disabled={exportMutation.isPending} onClick={() => exportMutation.mutate('CSV')}>
                <Download className="mr-1 h-4 w-4" /> CSV
              </Button>
              <Button variant="outline" size="sm" disabled={exportMutation.isPending} onClick={() => exportMutation.mutate('XLSX')}>
                <Download className="mr-1 h-4 w-4" /> Excel
              </Button>
              <Button variant="outline" size="sm" disabled={exportMutation.isPending} onClick={() => exportMutation.mutate('PDF')}>
                <Download className="mr-1 h-4 w-4" /> PDF
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="pt-6">
          {isLoading && <p className="text-sm text-muted-foreground">Chargement...</p>}
          {isError && <p className="text-sm text-destructive">{extractApiErrorMessage(error, 'Impossible de charger ce rapport.')}</p>}
          {report && (
            <>
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-sm font-medium">{report.title}</h2>
                <p className="text-xs text-muted-foreground">
                  {report.rows.length} ligne(s) — généré le {new Date(report.generatedAt).toLocaleString('fr-FR')}
                </p>
              </div>
              {report.rows.length === 0 ? (
                <p className="text-sm text-muted-foreground">Aucune donnée pour les filtres sélectionnés.</p>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        {report.columns.map((col) => (
                          <TableHead key={col}>{col}</TableHead>
                        ))}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {report.rows.map((row, i) => (
                        <TableRow key={i}>
                          {row.map((cell, j) => (
                            <TableCell key={j}>{cell}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function FilterSelect({
  label,
  value,
  onChange,
  children,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  children: React.ReactNode
}) {
  return (
    <div className="w-[180px]">
      <label className="mb-1 block text-xs text-muted-foreground">{label}</label>
      <Select value={value} onValueChange={onChange}>
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL}>Tous</SelectItem>
          {children}
        </SelectContent>
      </Select>
    </div>
  )
}
