import { describe, expect, it } from 'vitest'
import { REPORT_TYPE_LABEL, REPORT_TYPE_RELEVANT_FILTERS, type ReportType } from './report'

/**
 * Tests unitaires (Phase 10 - tache #88) de REPORT_TYPE_RELEVANT_FILTERS,
 * la logique de filtrage critique citee par le prompt maitre pour cette
 * tache : c'est cette table qui decide quels filtres de la barre commune
 * ReportsPage envoie reellement au backend pour chaque type de rapport
 * (voir ReportsPage.tsx - `filters` n'inclut un champ que si `relevant`
 * le contient). Une regression ici change silencieusement le contenu d'un
 * rapport sans qu'aucune erreur ne soit levee.
 */

const ALL_REPORT_TYPES: ReportType[] = [
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

describe('REPORT_TYPE_RELEVANT_FILTERS', () => {
  it('has an entry for every ReportType, with no typo/omission', () => {
    expect(Object.keys(REPORT_TYPE_RELEVANT_FILTERS).sort()).toEqual([...ALL_REPORT_TYPES].sort())
  })

  it('never lists a filter more than once for the same report type', () => {
    for (const type of ALL_REPORT_TYPES) {
      const filters = REPORT_TYPE_RELEVANT_FILTERS[type]
      expect(new Set(filters).size).toBe(filters.length)
    }
  })

  it('scopes ANOMALIES to anomalyStatus/date range and never to a movement-only or asset-condition filter', () => {
    expect(REPORT_TYPE_RELEVANT_FILTERS.ANOMALIES).toEqual(['siteId', 'anomalyStatus', 'dateFrom', 'dateTo'])
  })

  it('scopes MOUVEMENTS and TRANSFERTS to movement-relevant filters, never condition/status (asset fields)', () => {
    expect(REPORT_TYPE_RELEVANT_FILTERS.MOUVEMENTS).toEqual(['siteId', 'movementType', 'dateFrom', 'dateTo'])
    expect(REPORT_TYPE_RELEVANT_FILTERS.TRANSFERTS).toEqual(['siteId', 'dateFrom', 'dateTo'])
    for (const type of ['MOUVEMENTS', 'TRANSFERTS'] as const) {
      expect(REPORT_TYPE_RELEVANT_FILTERS[type]).not.toContain('condition')
      expect(REPORT_TYPE_RELEVANT_FILTERS[type]).not.toContain('status')
    }
  })

  it('never scopes an asset-breakdown report (PAR_*) to anomalyStatus or movementType', () => {
    const assetBreakdownTypes: ReportType[] = ['PAR_SITE', 'PAR_CATEGORIE', 'PAR_SERVICE', 'PAR_UTILISATEUR', 'PAR_ETAT']
    for (const type of assetBreakdownTypes) {
      expect(REPORT_TYPE_RELEVANT_FILTERS[type]).not.toContain('anomalyStatus')
      expect(REPORT_TYPE_RELEVANT_FILTERS[type]).not.toContain('movementType')
    }
  })

  it('has a non-empty French label for every report type (used as the Select option text)', () => {
    for (const type of ALL_REPORT_TYPES) {
      expect(REPORT_TYPE_LABEL[type]).toBeTruthy()
      expect(typeof REPORT_TYPE_LABEL[type]).toBe('string')
    }
  })
})
